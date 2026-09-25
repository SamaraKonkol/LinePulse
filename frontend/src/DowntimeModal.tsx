import { FormEvent, useState } from 'react';
import './incident-modal.css';
import type { Machine } from './types/api';

type Props = {
  machines: Machine[];
  loading: boolean;
  onClose: () => void;
  onSubmit: (draft: { machineId: string; reason: string }) => void;
};

function DowntimeModal({ machines, loading, onClose, onSubmit }: Props) {
  const [machineId, setMachineId] = useState(machines[0]?.id ?? '');
  const [reason, setReason] = useState('');

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onSubmit({ machineId, reason });
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="downtime-title" onMouseDown={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <div>
            <span className="eyebrow">Disponibilidade</span>
            <h2 id="downtime-title">Registrar parada de máquina</h2>
          </div>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Fechar">×</button>
        </div>

        <form className="incident-form" onSubmit={handleSubmit}>
          <label>
            Máquina
            <select value={machineId} onChange={(event) => setMachineId(event.target.value)} required>
              {machines.map((machine) => (
                <option key={machine.id} value={machine.id}>{machine.assetCode} · {machine.name}</option>
              ))}
            </select>
          </label>

          <label>
            Motivo da parada
            <textarea value={reason} onChange={(event) => setReason(event.target.value)} required minLength={5} rows={5} placeholder="Ex.: Falha no acionamento principal" />
          </label>

          <div className="modal-actions">
            <button type="button" className="secondary-button" onClick={onClose}>Cancelar</button>
            <button className="primary-button" disabled={loading || !machineId}>{loading ? 'Registrando...' : 'Registrar parada'}</button>
          </div>
        </form>
      </section>
    </div>
  );
}

export default DowntimeModal;
