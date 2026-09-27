import { FormEvent, useState } from 'react';
import type { Machine, MaintenancePlan, MaintenancePlanInput, WorkOrderPriority } from './types/api';

function MaintenancePlanModal({ machines, plan, loading, onClose, onSubmit }: { machines: Machine[]; plan?: MaintenancePlan | null; loading: boolean; onClose: () => void; onSubmit: (input: MaintenancePlanInput) => void }) {
  const [machineId, setMachineId] = useState(plan?.machineId ?? machines.find((machine) => machine.status !== 'INACTIVE')?.id ?? '');
  const [title, setTitle] = useState(plan?.title ?? '');
  const [description, setDescription] = useState(plan?.description ?? '');
  const [intervalDays, setIntervalDays] = useState(plan?.intervalDays ?? 90);
  const [nextDueDate, setNextDueDate] = useState(plan?.nextDueDate ?? new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10));
  const [priority, setPriority] = useState<WorkOrderPriority>(plan?.priority ?? 'MEDIUM');

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onSubmit({ machineId, title, description, intervalDays, nextDueDate, priority });
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="maintenance-plan-title" onMouseDown={(event) => event.stopPropagation()}>
        <div className="modal-header"><div><span className="eyebrow">Planejamento preventivo</span><h2 id="maintenance-plan-title">{plan ? 'Editar plano' : 'Nova preventiva'}</h2></div><button type="button" className="modal-close" onClick={onClose} aria-label="Fechar">×</button></div>
        <form className="incident-form" onSubmit={handleSubmit}>
          <label>Máquina<select value={machineId} onChange={(event) => setMachineId(event.target.value)} required>{machines.filter((machine) => machine.status !== 'INACTIVE' || machine.id === plan?.machineId).map((machine) => <option key={machine.id} value={machine.id}>{machine.assetCode} · {machine.name}</option>)}</select></label>
          <label>Título<input value={title} onChange={(event) => setTitle(event.target.value)} required maxLength={160} placeholder="Ex.: Troca de óleo do redutor" /></label>
          <label>Procedimento<textarea value={description} onChange={(event) => setDescription(event.target.value)} required rows={4} placeholder="Descreva o serviço preventivo previsto." /></label>
          <label>Recorrência (dias)<input type="number" min={1} max={3650} value={intervalDays} onChange={(event) => setIntervalDays(Number(event.target.value))} required /></label>
          <label>Próxima execução<input type="date" value={nextDueDate} onChange={(event) => setNextDueDate(event.target.value)} required /></label>
          <label>Prioridade<select value={priority} onChange={(event) => setPriority(event.target.value as WorkOrderPriority)}><option value="LOW">Baixa</option><option value="MEDIUM">Média</option><option value="HIGH">Alta</option><option value="CRITICAL">Crítica</option></select></label>
          <div className="modal-actions"><button type="button" className="secondary-button" onClick={onClose}>Cancelar</button><button className="primary-button" disabled={loading || !machineId}>{loading ? 'Salvando...' : plan ? 'Salvar plano' : 'Criar preventiva'}</button></div>
        </form>
      </section>
    </div>
  );
}

export default MaintenancePlanModal;
