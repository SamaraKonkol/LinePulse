import { FormEvent, useState } from 'react';
import './incident-modal.css';
import type { IncidentCategory, IncidentPriority, Machine } from './types/api';

type IncidentDraft = {
  machineId: string;
  title: string;
  description: string;
  category: IncidentCategory;
  priority: IncidentPriority;
};

type Props = {
  machines: Machine[];
  loading: boolean;
  onClose: () => void;
  onSubmit: (draft: IncidentDraft) => void;
};

function IncidentModal({ machines, loading, onClose, onSubmit }: Props) {
  const [machineId, setMachineId] = useState(machines[0]?.id ?? '');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState<IncidentCategory>('MECHANICAL');
  const [priority, setPriority] = useState<IncidentPriority>('MEDIUM');

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onSubmit({ machineId, title, description, category, priority });
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="incident-title" onMouseDown={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <div><span className="eyebrow">Ocorrência</span><h2 id="incident-title">Registrar nova ocorrência</h2></div>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Fechar">×</button>
        </div>
        <form className="incident-form" onSubmit={handleSubmit}>
          <label>Máquina<select value={machineId} onChange={(event) => setMachineId(event.target.value)} required>{machines.map((machine) => <option key={machine.id} value={machine.id}>{machine.assetCode} · {machine.name}</option>)}</select></label>
          <label>Título<input value={title} onChange={(event) => setTitle(event.target.value)} required minLength={3} maxLength={160} placeholder="Ex.: Vibração acima do normal" /></label>
          <label>Descrição<textarea value={description} onChange={(event) => setDescription(event.target.value)} required minLength={5} rows={5} placeholder="Descreva o que foi observado na operação." /></label>
          <label>Categoria<select value={category} onChange={(event) => setCategory(event.target.value as IncidentCategory)}><option value="MECHANICAL">Mecânica</option><option value="ELECTRICAL">Elétrica</option><option value="HYDRAULIC">Hidráulica</option><option value="PNEUMATIC">Pneumática</option><option value="SAFETY">Segurança</option><option value="PROCESS">Processo</option><option value="OTHER">Outro</option></select></label>
          <label>Prioridade<select value={priority} onChange={(event) => setPriority(event.target.value as IncidentPriority)}><option value="LOW">Baixa</option><option value="MEDIUM">Média</option><option value="HIGH">Alta</option><option value="CRITICAL">Crítica</option></select></label>
          <div className="modal-actions"><button type="button" className="secondary-button" onClick={onClose}>Cancelar</button><button className="primary-button" disabled={loading || !machineId}>{loading ? 'Registrando...' : 'Registrar ocorrência'}</button></div>
        </form>
      </section>
    </div>
  );
}

export default IncidentModal;
