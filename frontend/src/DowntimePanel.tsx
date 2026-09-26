import type { Downtime } from './types/api';
import './downtime.css';

type Props = {
  downtimes: Downtime[];
  canManage: boolean;
  closing: boolean;
  onNew: () => void;
  onCloseDowntime: (id: string) => void;
};

function DowntimePanel({ downtimes, canManage, closing, onNew, onCloseDowntime }: Props) {
  const openDowntimes = downtimes.filter((downtime) => downtime.endedAt === null);

  return (
    <section className="panel downtime-panel" id="downtime">
      <div className="panel-heading">
        <div>
          <span className="eyebrow">Paradas</span>
          <h2>Downtime em aberto</h2>
        </div>
        {canManage && <button className="secondary-button" onClick={onNew}>Registrar parada</button>}
      </div>

      {openDowntimes.length === 0 && <div className="empty-state">Nenhuma parada aberta.</div>}

      <div className="downtime-list">
        {openDowntimes.map((downtime) => (
          <article className="downtime-row" key={downtime.id}>
            <div className="downtime-indicator" />
            <div className="downtime-copy">
              <strong>{downtime.assetCode} · {downtime.machineName}</strong>
              <span>{downtime.reason}</span>
              <small>Desde {new Date(downtime.startedAt).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })}</small>
            </div>
            {canManage && <button className="order-action complete" disabled={closing} onClick={() => onCloseDowntime(downtime.id)}>Encerrar parada</button>}
          </article>
        ))}
      </div>
    </section>
  );
}

export default DowntimePanel;
