import { Activity, AlertTriangle, Clock3, Wrench, X } from 'lucide-react';
import type { Downtime, Incident, Machine, WorkOrder } from './types/api';
import './machine-detail.css';

type Props = {
  machine: Machine;
  incidents: Incident[];
  orders: WorkOrder[];
  downtimes: Downtime[];
  onClose: () => void;
};

const statusLabel = { RUNNING: 'Operando', STOPPED: 'Parada', MAINTENANCE: 'Manutenção', INACTIVE: 'Inativa' };

function MachineDetailPanel({ machine, incidents, orders, downtimes, onClose }: Props) {
  const relatedIncidents = incidents.filter((incident) => incident.machineId === machine.id);
  const relatedOrders = orders.filter((order) => order.machineId === machine.id);
  const relatedDowntimes = downtimes.filter((downtime) => downtime.machineId === machine.id);
  const now = Date.now();
  const dayStart = now - 24 * 60 * 60 * 1000;
  const downtimeMs = relatedDowntimes.reduce((total, downtime) => {
    const start = Math.max(new Date(downtime.startedAt).getTime(), dayStart);
    const end = Math.min(downtime.endedAt ? new Date(downtime.endedAt).getTime() : now, now);
    return total + Math.max(0, end - start);
  }, 0);
  const availability = machine.status === 'INACTIVE' ? null : Math.max(0, 100 * (1 - downtimeMs / (24 * 60 * 60 * 1000)));
  const completedDurations = relatedOrders
    .filter((order) => order.startedAt && order.completedAt)
    .map((order) => new Date(order.completedAt!).getTime() - new Date(order.startedAt!).getTime())
    .filter((duration) => duration >= 0);
  const mttr = completedDurations.length ? completedDurations.reduce((sum, duration) => sum + duration, 0) / completedDurations.length / 60_000 : null;
  const openIncidents = relatedIncidents.filter((incident) => incident.status === 'OPEN' || incident.status === 'IN_PROGRESS').length;
  const activeOrders = relatedOrders.filter((order) => order.status === 'OPEN' || order.status === 'IN_PROGRESS').length;

  const timeline = [
    ...relatedIncidents.map((incident) => ({ id: `i-${incident.id}`, at: incident.occurredAt, title: `Ocorrência · ${incident.title}`, detail: incident.status })),
    ...relatedOrders.map((order) => ({ id: `o-${order.id}`, at: order.createdAt, title: `Ordem · ${order.title}`, detail: order.status })),
    ...relatedDowntimes.map((downtime) => ({ id: `d-${downtime.id}`, at: downtime.startedAt, title: `Parada · ${downtime.reason}`, detail: downtime.endedAt ? 'ENCERRADA' : 'EM ABERTO' })),
  ].sort((a, b) => new Date(b.at).getTime() - new Date(a.at).getTime()).slice(0, 12);

  return (
    <div className="machine-detail-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="machine-detail" role="dialog" aria-modal="true" aria-labelledby="machine-detail-title" onMouseDown={(event) => event.stopPropagation()}>
        <header className="machine-detail-header">
          <div>
            <span className="eyebrow">Ficha do ativo</span>
            <h2 id="machine-detail-title">{machine.assetCode} · {machine.name}</h2>
            <p>{machine.plant} / {machine.sector} / {machine.productionLine}</p>
          </div>
          <button type="button" onClick={onClose} aria-label="Fechar"><X size={20} /></button>
        </header>

        <div className="machine-detail-status-row">
          <span className={`machine-status-label status-${machine.status.toLowerCase()}`}>{statusLabel[machine.status]}</span>
          <span>{machine.manufacturer ?? 'Fabricante não informado'}{machine.model ? ` · ${machine.model}` : ''}</span>
          <span>Série: {machine.serialNumber ?? '—'}</span>
          <span>Instalação: {machine.installedAt ? new Date(`${machine.installedAt}T12:00:00`).toLocaleDateString('pt-BR') : '—'}</span>
        </div>

        <div className="machine-detail-metrics">
          <article><Activity size={18} /><span>Disponibilidade 24h</span><strong>{availability === null ? 'Fora de operação' : `${availability.toFixed(1)}%`}</strong></article>
          <article><AlertTriangle size={18} /><span>Ocorrências abertas</span><strong>{openIncidents}</strong></article>
          <article><Wrench size={18} /><span>Ordens ativas</span><strong>{activeOrders}</strong></article>
          <article><Clock3 size={18} /><span>MTTR do ativo</span><strong>{mttr === null ? 'Sem dados' : `${mttr.toFixed(1)} min`}</strong></article>
        </div>

        <div className="machine-detail-grid">
          <article className="machine-detail-card">
            <div className="panel-heading"><div><span className="eyebrow">Confiabilidade</span><h3>Resumo operacional</h3></div></div>
            <dl className="machine-facts">
              <div><dt>Ocorrências totais</dt><dd>{relatedIncidents.length}</dd></div>
              <div><dt>Ordens totais</dt><dd>{relatedOrders.length}</dd></div>
              <div><dt>Downtimes registrados</dt><dd>{relatedDowntimes.length}</dd></div>
              <div><dt>Downtime nas últimas 24h</dt><dd>{(downtimeMs / 60_000).toFixed(0)} min</dd></div>
            </dl>
          </article>

          <article className="machine-detail-card">
            <div className="panel-heading"><div><span className="eyebrow">Histórico</span><h3>Eventos recentes</h3></div></div>
            {timeline.length === 0 && <div className="empty-state">Ainda não há histórico para este ativo.</div>}
            <div className="machine-timeline">{timeline.map((event) => <div key={event.id}><span /><div><strong>{event.title}</strong><small>{event.detail} · {new Date(event.at).toLocaleString('pt-BR')}</small></div></div>)}</div>
          </article>
        </div>
      </section>
    </div>
  );
}

export default MachineDetailPanel;
