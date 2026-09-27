import { CalendarClock, Pencil, Play, Plus } from 'lucide-react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import MaintenancePlanModal from './MaintenancePlanModal';
import { createMaintenancePlan, generateMaintenancePlan, getMaintenancePlans, updateMaintenancePlan, updateMaintenancePlanStatus } from './services/api';
import type { Machine, MaintenancePlan, MaintenancePlanInput } from './types/api';
import { getApiErrorMessage } from './utils/apiError';

function daysUntil(date: string) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const due = new Date(`${date}T00:00:00`);
  return Math.round((due.getTime() - today.getTime()) / 86400000);
}

function PreventiveMaintenancePanel({ machines, isAdmin }: { machines: Machine[]; isAdmin: boolean }) {
  const queryClient = useQueryClient();
  const plansQuery = useQuery({ queryKey: ['maintenance-plans'], queryFn: getMaintenancePlans });
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<MaintenancePlan | null>(null);
  const [filter, setFilter] = useState<'ALL' | 'DUE' | 'ACTIVE' | 'INACTIVE'>('ALL');

  const refresh = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ['maintenance-plans'] }),
    queryClient.invalidateQueries({ queryKey: ['work-orders'] }),
    queryClient.invalidateQueries({ queryKey: ['alerts'] }),
    queryClient.invalidateQueries({ queryKey: ['audit-events'] }),
  ]);

  const createMutation = useMutation({ mutationFn: createMaintenancePlan, onSuccess: async () => { setModalOpen(false); await refresh(); } });
  const updateMutation = useMutation({ mutationFn: ({ id, input }: { id: string; input: MaintenancePlanInput }) => updateMaintenancePlan(id, input), onSuccess: async () => { setEditing(null); await refresh(); } });
  const statusMutation = useMutation({ mutationFn: ({ id, active }: { id: string; active: boolean }) => updateMaintenancePlanStatus(id, active), onSuccess: refresh });
  const generateMutation = useMutation({ mutationFn: generateMaintenancePlan, onSuccess: refresh });

  const plans = plansQuery.data ?? [];
  const filtered = useMemo(() => plans.filter((plan) => {
    if (filter === 'DUE') return plan.active && daysUntil(plan.nextDueDate) <= 7;
    if (filter === 'ACTIVE') return plan.active;
    if (filter === 'INACTIVE') return !plan.active;
    return true;
  }), [filter, plans]);
  const error = plansQuery.error || createMutation.error || updateMutation.error || statusMutation.error || generateMutation.error;

  function submit(input: MaintenancePlanInput) {
    if (editing) updateMutation.mutate({ id: editing.id, input });
    else createMutation.mutate(input);
  }

  return (
    <section className="panel preventive-panel" id="preventive-maintenance">
      <div className="panel-heading">
        <div><span className="eyebrow">Planejamento</span><h2>Manutenção preventiva</h2></div>
        {isAdmin && <button type="button" className="secondary-button" onClick={() => setModalOpen(true)} disabled={machines.length === 0}><Plus size={16} /> Nova preventiva</button>}
      </div>
      <div className="preventive-toolbar">
        <select value={filter} onChange={(event) => setFilter(event.target.value as typeof filter)}><option value="ALL">Todos os planos</option><option value="DUE">Próximos 7 dias / vencidos</option><option value="ACTIVE">Ativos</option><option value="INACTIVE">Inativos</option></select>
        <span>{plans.filter((plan) => plan.active && daysUntil(plan.nextDueDate) <= 7).length} exigem atenção</span>
      </div>
      {error && <div className="inline-error">{getApiErrorMessage(error, 'Não foi possível carregar o planejamento preventivo.')}</div>}
      {plansQuery.isLoading && <div className="empty-state">Carregando planos preventivos...</div>}
      {!plansQuery.isLoading && filtered.length === 0 && <div className="empty-state">Nenhum plano preventivo encontrado.</div>}
      <div className="preventive-list">
        {filtered.map((plan) => {
          const days = daysUntil(plan.nextDueDate);
          const dueLabel = days < 0 ? `${Math.abs(days)} dia(s) vencido` : days === 0 ? 'vence hoje' : `em ${days} dia(s)`;
          return (
            <article className={`preventive-row ${days <= 0 && plan.active ? 'overdue' : ''} ${!plan.active ? 'inactive' : ''}`} key={plan.id}>
              <div className="preventive-icon"><CalendarClock size={18} /></div>
              <div className="preventive-copy"><strong>{plan.assetCode} · {plan.title}</strong><span>{plan.machineName} · a cada {plan.intervalDays} dias</span><small>Próxima: {new Date(`${plan.nextDueDate}T00:00:00`).toLocaleDateString('pt-BR')} · {dueLabel}</small></div>
              <span className={`badge ${plan.priority === 'CRITICAL' || plan.priority === 'HIGH' ? 'critical' : plan.priority === 'MEDIUM' ? 'warning' : 'neutral'}`}>{plan.priority}</span>
              {isAdmin && <div className="preventive-actions"><button type="button" title="Gerar ordem agora" disabled={generateMutation.isPending || !plan.active} onClick={() => generateMutation.mutate(plan.id)}><Play size={14} /></button><button type="button" title="Editar plano" onClick={() => setEditing(plan)}><Pencil size={14} /></button><button type="button" onClick={() => statusMutation.mutate({ id: plan.id, active: !plan.active })}>{plan.active ? 'Pausar' : 'Ativar'}</button></div>}
            </article>
          );
        })}
      </div>
      {(modalOpen || editing) && <MaintenancePlanModal machines={machines} plan={editing} loading={createMutation.isPending || updateMutation.isPending} onClose={() => { setModalOpen(false); setEditing(null); }} onSubmit={submit} />}
    </section>
  );
}

export default PreventiveMaintenancePanel;
