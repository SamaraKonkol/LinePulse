import { useMemo, useState } from 'react';
import type { Incident, IncidentPriority, IncidentStatus } from './types/api';

type IncidentAction = 'start' | 'resolve' | 'cancel';

type Props = {
  incidents: Incident[];
  canManage: boolean;
  busy: boolean;
  onClose: () => void;
  onTransition: (id: string, action: IncidentAction) => void;
};

const statusLabel: Record<IncidentStatus, string> = {
  OPEN: 'Aberta',
  IN_PROGRESS: 'Em andamento',
  RESOLVED: 'Resolvida',
  CANCELLED: 'Cancelada',
};

const priorityLabel: Record<IncidentPriority, string> = {
  LOW: 'Baixa',
  MEDIUM: 'Média',
  HIGH: 'Alta',
  CRITICAL: 'Crítica',
};

function IncidentHistoryPanel({ incidents, canManage, busy, onClose, onTransition }: Props) {
  const [status, setStatus] = useState<'ALL' | IncidentStatus>('ALL');
  const [priority, setPriority] = useState<'ALL' | IncidentPriority>('ALL');
  const [search, setSearch] = useState('');

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    return incidents.filter((incident) => {
      const matchesStatus = status === 'ALL' || incident.status === status;
      const matchesPriority = priority === 'ALL' || incident.priority === priority;
      const matchesSearch = !term || [incident.assetCode, incident.machineName, incident.title, incident.description]
        .some((value) => value.toLowerCase().includes(term));
      return matchesStatus && matchesPriority && matchesSearch;
    });
  }, [incidents, priority, search, status]);

  return (
    <section className="panel incident-history-panel">
      <div className="panel-heading incident-history-heading">
        <div>
          <span className="eyebrow">Histórico</span>
          <h2>Todas as ocorrências</h2>
        </div>
        <button className="text-button" type="button" onClick={onClose}>Fechar</button>
      </div>

      <div className="incident-filters">
        <input
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Buscar máquina, ativo ou ocorrência"
        />
        <select value={status} onChange={(event) => setStatus(event.target.value as 'ALL' | IncidentStatus)}>
          <option value="ALL">Todos os status</option>
          <option value="OPEN">Aberta</option>
          <option value="IN_PROGRESS">Em andamento</option>
          <option value="RESOLVED">Resolvida</option>
          <option value="CANCELLED">Cancelada</option>
        </select>
        <select value={priority} onChange={(event) => setPriority(event.target.value as 'ALL' | IncidentPriority)}>
          <option value="ALL">Todas as prioridades</option>
          <option value="CRITICAL">Crítica</option>
          <option value="HIGH">Alta</option>
          <option value="MEDIUM">Média</option>
          <option value="LOW">Baixa</option>
        </select>
      </div>

      {filtered.length === 0 && <div className="empty-state">Nenhuma ocorrência encontrada com esses filtros.</div>}

      <div className="incident-history-list">
        {filtered.map((incident) => (
          <article className="incident-history-row" key={incident.id}>
            <div className="incident-history-copy">
              <div className="incident-history-title">
                <strong>{incident.assetCode} · {incident.machineName}</strong>
                <span className={`incident-status ${incident.status.toLowerCase()}`}>{statusLabel[incident.status]}</span>
              </div>
              <span>{incident.title}</span>
              <small>{new Date(incident.occurredAt).toLocaleString('pt-BR')} · prioridade {priorityLabel[incident.priority].toLowerCase()}</small>
            </div>

            {canManage && (incident.status === 'OPEN' || incident.status === 'IN_PROGRESS') && (
              <div className="incident-history-actions">
                {incident.status === 'OPEN' && (
                  <button type="button" disabled={busy} onClick={() => onTransition(incident.id, 'start')}>Iniciar</button>
                )}
                {incident.status === 'IN_PROGRESS' && (
                  <button className="resolve" type="button" disabled={busy} onClick={() => onTransition(incident.id, 'resolve')}>Resolver</button>
                )}
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
