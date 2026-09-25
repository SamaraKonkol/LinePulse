import { BellRing } from 'lucide-react';
import type { OperationalAlert } from './types/api';

type Props = {
  alerts: OperationalAlert[];
  loading: boolean;
};

function AlertPanel({ alerts, loading }: Props) {
  if (loading) {
    return <section className="panel alert-panel"><div className="empty-state">Verificando alertas operacionais...</div></section>;
  }

  return (
    <section className={`panel alert-panel ${alerts.length === 0 ? 'clear' : ''}`}>
      <div className="panel-heading">
        <div>
          <span className="eyebrow">Monitoramento</span>
          <h2>Alertas operacionais</h2>
        </div>
        <div className="alert-count"><BellRing size={17} /> {alerts.length}</div>
      </div>

      {alerts.length === 0 && <div className="alert-clear-state">Nenhum alerta ativo pelas regras atuais.</div>}

      <div className="alert-list">
        {alerts.slice(0, 6).map((alert) => (
          <article className={`alert-row ${alert.severity.toLowerCase()}`} key={alert.key}>
            <span className="alert-indicator" />
            <div>
              <strong>{alert.title}</strong>
              <p>{alert.message}</p>
              <small>{new Date(alert.detectedAt).toLocaleString('pt-BR')}</small>
            </div>
            <span className="alert-severity">{alert.severity === 'CRITICAL' ? 'Crítico' : 'Atenção'}</span>
          </article>
        ))}
      </div>
    </section>
  );
}

export default AlertPanel;
