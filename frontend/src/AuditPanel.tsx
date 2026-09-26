import { History, Search } from 'lucide-react';
import { useMemo, useState } from 'react';
import type { AuditEvent } from './types/api';
import './audit.css';

type Props = {
  events: AuditEvent[];
  loading: boolean;
};

function AuditPanel({ events, loading }: Props) {
  const [search, setSearch] = useState('');
  const [actionFilter, setActionFilter] = useState('ALL');
  const [entityFilter, setEntityFilter] = useState('ALL');
  const [period, setPeriod] = useState<'ALL' | 'TODAY' | '7D'>('ALL');
  const actions = [...new Set(events.map((event) => event.action))].sort();
  const entities = [...new Set(events.map((event) => event.entityType))].sort();

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    const now = Date.now();
    const todayStart = new Date();
    todayStart.setHours(0, 0, 0, 0);
    return events.filter((event) => {
      const created = new Date(event.createdAt).getTime();
      const matchesSearch = !term || `${event.actorRegistration} ${event.description}`.toLowerCase().includes(term);
      const matchesAction = actionFilter === 'ALL' || event.action === actionFilter;
      const matchesEntity = entityFilter === 'ALL' || event.entityType === entityFilter;
      const matchesPeriod = period === 'ALL' || (period === 'TODAY' ? created >= todayStart.getTime() : created >= now - 7 * 24 * 60 * 60 * 1000);
      return matchesSearch && matchesAction && matchesEntity && matchesPeriod;
    });
  }, [events, search, actionFilter, entityFilter, period]);

  return (
    <section className="panel audit-panel" id="audit">
      <div className="panel-heading">
        <div><span className="eyebrow">Rastreabilidade</span><h2>Atividade recente</h2></div>
        <History size={19} />
      </div>

      <div className="audit-filters">
        <label><Search size={14} /><input type="search" placeholder="Cadastro ou descrição" value={search} onChange={(event) => setSearch(event.target.value)} /></label>
        <select value={actionFilter} onChange={(event) => setActionFilter(event.target.value)}><option value="ALL">Todas as ações</option>{actions.map((action) => <option key={action} value={action}>{action}</option>)}</select>
        <select value={entityFilter} onChange={(event) => setEntityFilter(event.target.value)}><option value="ALL">Todas as entidades</option>{entities.map((entity) => <option key={entity} value={entity}>{entity}</option>)}</select>
        <select value={period} onChange={(event) => setPeriod(event.target.value as 'ALL' | 'TODAY' | '7D')}><option value="ALL">Todo o período</option><option value="TODAY">Hoje</option><option value="7D">Últimos 7 dias</option></select>
      </div>

      {loading && <div className="empty-state">Carregando atividades...</div>}
      {!loading && events.length === 0 && <div className="empty-state">Nenhuma atividade registrada ainda.</div>}
      {!loading && events.length > 0 && filtered.length === 0 && <div className="empty-state">Nenhuma atividade encontrada com esses filtros.</div>}

      <div className="audit-list">
        {filtered.slice(0, 50).map((event) => (
          <article className="audit-row" key={event.id}>
            <span className="audit-dot" />
            <div><strong>{event.description}</strong><small>{event.actorRegistration} · {event.action} · {event.entityType} · {new Date(event.createdAt).toLocaleString('pt-BR')}</small></div>
          </article>
        ))}
      </div>
    </section>
  );
}

export default AuditPanel;
