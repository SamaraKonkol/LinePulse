import { BarChart3, Gauge, TriangleAlert } from 'lucide-react';
import type { Downtime, Incident, Machine, WorkOrder } from './types/api';
import './operational-insights.css';

type Props = {
  machines: Machine[];
  incidents: Incident[];
  orders: WorkOrder[];
  downtimes: Downtime[];
  onSelectMachine: (machine: Machine) => void;
};

type DowntimeAccumulator = { minutes: number; count: number };

function minutesInLast24Hours(entries: Downtime[]) {
  const now = Date.now();
  const startWindow = now - 24 * 60 * 60 * 1000;
  return entries.reduce((total, entry) => {
    const start = Math.max(new Date(entry.startedAt).getTime(), startWindow);
    const end = Math.min(entry.endedAt ? new Date(entry.endedAt).getTime() : now, now);
    return total + Math.max(0, end - start) / 60_000;
  }, 0);
}

function OperationalInsightsPanel({ machines, incidents, orders, downtimes, onSelectMachine }: Props) {
  const activeMachines = machines.filter((machine) => machine.status !== 'INACTIVE');
  const machineRisk = activeMachines.map((machine) => {
    const machineIncidents = incidents.filter((incident) => incident.machineId === machine.id);
    const open = machineIncidents.filter((incident) => incident.status === 'OPEN' || incident.status === 'IN_PROGRESS').length;
    const critical = machineIncidents.filter((incident) => incident.priority === 'CRITICAL' && incident.status !== 'RESOLVED' && incident.status !== 'CANCELLED').length;
    const downtimeMinutes = minutesInLast24Hours(downtimes.filter((downtime) => downtime.machineId === machine.id));
    const activeOrders = orders.filter((order) => order.machineId === machine.id && (order.status === 'OPEN' || order.status === 'IN_PROGRESS')).length;
    const score = critical * 8 + open * 3 + activeOrders * 2 + downtimeMinutes / 30;
    return { machine, open, critical, downtimeMinutes, activeOrders, score };
  }).sort((a, b) => b.score - a.score);

  const lines = Array.from(new Set(activeMachines.map((machine) => machine.productionLine))).map((lineName) => {
    const lineMachines = activeMachines.filter((machine) => machine.productionLine === lineName);
    const ids = new Set(lineMachines.map((machine) => machine.id));
    const lineDowntimes = downtimes.filter((downtime) => ids.has(downtime.machineId));
    const downtimeMinutes = minutesInLast24Hours(lineDowntimes);
    const capacityMinutes = lineMachines.length * 24 * 60;
    const availability = capacityMinutes ? Math.max(0, 100 * (1 - downtimeMinutes / capacityMinutes)) : 0;
    const openIncidents = incidents.filter((incident) => ids.has(incident.machineId) && (incident.status === 'OPEN' || incident.status === 'IN_PROGRESS')).length;
    return { lineName, plant: lineMachines[0]?.plant ?? '', sector: lineMachines[0]?.sector ?? '', machines: lineMachines.length, downtimeMinutes, availability, openIncidents };
  }).sort((a, b) => a.availability - b.availability);

  const downtimeSummary = downtimes.reduce<Record<string, DowntimeAccumulator>>((acc, downtime) => {
    const current = acc[downtime.machineId] ?? { minutes: 0, count: 0 };
    current.minutes += minutesInLast24Hours([downtime]);
    current.count += 1;
    acc[downtime.machineId] = current;
    return acc;
  }, {});
  const totalDowntime = Object.values(downtimeSummary).reduce((sum, item) => sum + item.minutes, 0);

  return (
    <section className="panel insights-panel" id="insights">
      <div className="panel-heading">
        <div><span className="eyebrow">Análise operacional</span><h2>Indicadores por linha e ativo</h2></div>
        <BarChart3 size={20} />
      </div>

      <div className="insight-summary">
        <article><Gauge size={18} /><span>Linhas monitoradas</span><strong>{lines.length}</strong></article>
        <article><TriangleAlert size={18} /><span>Ativos com ocorrência aberta</span><strong>{machineRisk.filter((item) => item.open > 0).length}</strong></article>
        <article><span>Downtime acumulado · 24h</span><strong>{totalDowntime.toFixed(0)} min</strong></article>
      </div>

      <div className="insights-grid">
        <div>
          <h3>Disponibilidade por linha</h3>
          {lines.length === 0 && <div className="empty-state">Sem linhas ativas para analisar.</div>}
          <div className="line-insights">{lines.map((line) => <article key={line.lineName}><div><strong>{line.lineName}</strong><small>{line.plant} · {line.sector} · {line.machines} ativos</small></div><div className="line-availability"><span>{line.availability.toFixed(1)}%</span><div><i style={{ width: `${Math.max(0, Math.min(100, line.availability))}%` }} /></div><small>{line.openIncidents} ocorrências abertas · {line.downtimeMinutes.toFixed(0)} min downtime</small></div></article>)}</div>
        </div>

        <div>
          <h3>Ativos que pedem atenção</h3>
          {machineRisk.length === 0 && <div className="empty-state">Sem ativos ativos para analisar.</div>}
          <div className="risk-list">{machineRisk.slice(0, 6).map((item) => <button type="button" key={item.machine.id} onClick={() => onSelectMachine(item.machine)}><span><strong>{item.machine.assetCode} · {item.machine.name}</strong><small>{item.machine.productionLine}</small></span><span className="risk-data"><b>{item.open} falhas</b><small>{item.downtimeMinutes.toFixed(0)} min downtime</small></span></button>)}</div>
        </div>
      </div>
    </section>
  );
}

export default OperationalInsightsPanel;
