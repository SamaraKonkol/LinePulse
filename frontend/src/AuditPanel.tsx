import { History } from 'lucide-react';
import type { AuditEvent } from './types/api';

type Props = {
  events: AuditEvent[];
  loading: boolean;
};

function AuditPanel({ events, loading }: Props) {
  return (
    <section className="panel audit-panel">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">Rastreabilidade</span>
          <h2>Atividade recente</h2>
        </div>
        <History size={19} />
      </div>

      {loading && <div className="empty-state">Carregando atividades...</div>}
      {!loading && events.length === 0 && <div className="empty-state">Nenhuma atividade registrada ainda.</div>}

      <div className="audit-list">
        {events.slice(0, 8).map((event) => (
          <article className="audit-row" key={event.id}>
            <span className="audit-dot" />
            <div>
              <strong>{event.description}</strong>
              <small>{event.actorEmail} · {new Date(event.createdAt).toLocaleString('pt-BR')}</small>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

export default AuditPanel;
