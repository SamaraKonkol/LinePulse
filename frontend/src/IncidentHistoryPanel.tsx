import { Download } from 'lucide-react';
import { useMemo, useState } from 'react';
import type { Incident, IncidentCategory, IncidentPriority, IncidentStatus } from './types/api';
import { exportCsv } from './utils/exportCsv';

type IncidentAction = 'start' | 'cancel';

type Props = {
  incidents: Incident[];
  canManage: boolean;
  busy: boolean;
  onClose: () => void;
  onTransition: (id: string, action: IncidentAction) => void;
  onResolve: (incident: Incident) => void;
};

const statusLabel: Record<IncidentStatus, string> = { OPEN: 'Aberta', IN_PROGRESS: 'Em andamento', RESOLVED: 'Resolvida', CANCELLED: 'Cancelada' };
const priorityLabel: Record<IncidentPriority, string> = { LOW: 'Baixa', MEDIUM: 'Média', HIGH: 'Alta', CRITICAL: 'Crítica' };
const categoryLabel: Record<IncidentCategory, string> = { MECHANICAL: 'Mecânica', ELECTRICAL: 'Elétrica', HYDRAULIC: 'Hidráulica', PNEUMATIC: 'Pneumática', SAFETY: 'Segurança', PROCESS: 'Processo', OTHER: 'Outro' };

function IncidentHistoryPanel({ incidents, canManage, busy, onClose, onTransition, onResolve }: Props) {
  const [status, setStatus] = useState<'ALL' | IncidentStatus>('ALL');
  const [priority, setPriority] = useState<'ALL' | IncidentPriority>('ALL');
  const [category, setCategory] = useState<'ALL' | IncidentCategory>('ALL');
  const [search, setSearch] = useState('');

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    return incidents.filter((incident) => {
      const matchesStatus = status === 'ALL' || incident.status === status;
      const matchesPriority = priority === 'ALL' || incident.priority === priority;
      const matchesCategory = category === 'ALL' || incident.category === category;
      const matchesSearch = !term || [incident.assetCode, incident.machineName, incident.title, incident.description, incident.rootCause ?? '', incident.solution ?? ''].some((value) => value.toLowerCase().includes(term));
      return matchesStatus && matchesPriority && matchesCategory && matchesSearch;
    });
  }, [category, incidents, priority, search, status]);

  function exportFiltered() {
    exportCsv('linepulse-ocorrencias.csv', filtered.map((incident) => ({
      ativo: incident.assetCode,
      maquina: incident.machineName,
      titulo: incident.title,
      categoria: categoryLabel[incident.category],
      prioridade: priorityLabel[incident.priority],
      status: statusLabel[incident.status],
      descricao: incident.description,
      causa_raiz: incident.rootCause,
      solucao: incident.solution,
      ocorrido_em: new Date(incident.occurredAt).toLocaleString('pt-BR'),
      resolvido_em: incident.resolvedAt ? new Date(incident.resolvedAt).toLocaleString('pt-BR') : '',
    })));
  }

  return (
    <section className="panel incident-history-panel">
      <div className="panel-heading incident-history-heading">
        <div><span className="eyebrow">Histórico</span><h2>Todas as ocorrências</h2></div>
        <div className="maintenance-heading-actions"><button className="text-button" type="button" onClick={exportFiltered} disabled={filtered.length === 0}><Download size={15} /> Exportar CSV</button><button className="text-button" type="button" onClick={onClose}>Fechar</button></div>
      </div>

      <div className="incident-filters">
        <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar máquina, causa ou solução" />
        <select value={status} onChange={(event) => setStatus(event.target.value as 'ALL' | IncidentStatus)}><option value="ALL">Todos os status</option><option value="OPEN">Aberta</option><option value="IN_PROGRESS">Em andamento</option><option value="RESOLVED">Resolvida</option><option value="CANCELLED">Cancelada</option></select>
        <select value={priority} onChange={(event) => setPriority(event.target.value as 'ALL' | IncidentPriority)}><option value="ALL">Todas as prioridades</option><option value="CRITICAL">Crítica</option><option value="HIGH">Alta</option><option value="MEDIUM">Média</option><option value="LOW">Baixa</option></select>
        <select value={category} onChange={(event) => setCategory(event.target.value as 'ALL' | IncidentCategory)}><option value="ALL">Todas as categorias</option>{Object.entries(categoryLabel).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
      </div>

      {filtered.length === 0 && <div className="empty-state">Nenhuma ocorrência encontrada com esses filtros.</div>}
      <div className="incident-history-list">
        {filtered.map((incident) => (
          <article className="incident-history-row" key={incident.id}>
            <div className="incident-history-copy">
              <div className="incident-history-title"><strong>{incident.assetCode} · {incident.machineName}</strong><span className={`incident-status ${incident.status.toLowerCase()}`}>{statusLabel[incident.status]}</span></div>
              <span>{incident.title}</span>
              <small>{categoryLabel[incident.category]} · {new Date(incident.occurredAt).toLocaleString('pt-BR')} · prioridade {priorityLabel[incident.priority].toLowerCase()}</small>
              {incident.status === 'RESOLVED' && <div className="incident-diagnosis"><strong>Causa raiz</strong><span>{incident.rootCause}</span><strong>Solução</strong><span>{incident.solution}</span></div>}
            </div>
            {canManage && (incident.status === 'OPEN' || incident.status === 'IN_PROGRESS') && (
              <div className="incident-history-actions">
                {incident.status === 'OPEN' && <button type="button" disabled={busy} onClick={() => onTransition(incident.id, 'start')}>Iniciar</button>}
                {incident.status === 'IN_PROGRESS' && <button className="resolve" type="button" disabled={busy} onClick={() => onResolve(incident)}>Resolver</button>}
                <button className="cancel" type="button" disabled={busy} onClick={() => onTransition(incident.id, 'cancel')}>Cancelar</button>
              </div>
            )}
          </article>
        ))}
      </div>
    </section>
  );
}

export default IncidentHistoryPanel;
