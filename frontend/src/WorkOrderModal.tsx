import { FormEvent, useState } from 'react';
import type { MaintenanceType, Machine, WorkOrderPriority } from './types/api';

type WorkOrderDraft = {
  machineId: string;
  title: string;
  description: string;
  type: MaintenanceType;
  priority: WorkOrderPriority;
};

type Props = {
  machines: Machine[];
  loading: boolean;
  onClose: () => void;
  onSubmit: (draft: WorkOrderDraft) => void;
};

function WorkOrderModal({ machines, loading, onClose, onSubmit }: Props) {
  const [machineId, setMachineId] = useState(machines[0]?.id ?? '');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [type, setType] = useState<MaintenanceType>('CORRECTIVE');
  const [priority, setPriority] = useState<WorkOrderPriority>('MEDIUM');

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onSubmit({ machineId, title, description, type, priority });
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="work-order-title" onMouseDown={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <div>
            <span className="eyebrow">Manutenção</span>
            <h2 id="work-order-title">Nova ordem de manutenção</h2>
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
            Título
            <input value={title} onChange={(event) => setTitle(event.target.value)} required minLength={3} placeholder="Ex.: Inspeção do conjunto de acionamento" />
          </label>

          <label>
            Descrição
            <textarea value={description} onChange={(event) => setDescription(event.target.value)} required minLength={5} rows={5} placeholder="Detalhe a intervenção necessária." />
          </label>

          <div className="form-grid-two">
            <label>
              Tipo
              <select value={type} onChange={(event) => setType(event.target.value as MaintenanceType)}>
                <option value="CORRECTIVE">Corretiva</option>
                <option value="PREVENTIVE">Preventiva</option>
                <option value="INSPECTION">Inspeção</option>
              </select>
            </label>

            <label>
              Prioridade
              <select value={priority} onChange={(event) => setPriority(event.target.value as WorkOrderPriority)}>
                <option value="LOW">Baixa</option>
                <option value="MEDIUM">Média</option>
                <option value="HIGH">Alta</option>
                <option value="CRITICAL">Crítica</option>
              </select>
            </label>
          </div>

          <div className="modal-actions">
            <button type="button" className="secondary-button" onClick={onClose}>Cancelar</button>
            <button className="primary-button" disabled={loading || !machineId}>{loading ? 'Criando...' : 'Criar ordem'}</button>
          </div>
        </form>
      </section>
    </div>
  );
}

export default WorkOrderModal;
