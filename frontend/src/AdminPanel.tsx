import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Plus, ShieldCheck, UserRoundCog } from 'lucide-react';
import { useState } from 'react';
import MachineAdminModal, { type MachineDraft } from './MachineAdminModal';
import { createMachine, getAdminUsers, getProductionLines, updateAdminUserRole, updateAdminUserStatus, updateMachine } from './services/api';
import type { Machine, UserRole } from './types/api';
import './admin.css';

type Props = {
  currentUserId: string;
  machines: Machine[];
};

const roleLabel: Record<UserRole, string> = {
  ADMIN: 'Administrador',
  TECHNICIAN: 'Técnico',
  OPERATOR: 'Operador',
};

function AdminPanel({ currentUserId, machines }: Props) {
  const queryClient = useQueryClient();
  const [machineModalOpen, setMachineModalOpen] = useState(false);
  const [editingMachine, setEditingMachine] = useState<Machine | null>(null);
  const usersQuery = useQuery({ queryKey: ['admin-users'], queryFn: getAdminUsers });
  const linesQuery = useQuery({ queryKey: ['admin-production-lines'], queryFn: getProductionLines });

  const refreshAdmin = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ['admin-users'] }),
    queryClient.invalidateQueries({ queryKey: ['audit-events'] }),
  ]);

  const refreshMachines = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ['machines'] }),
    queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
    queryClient.invalidateQueries({ queryKey: ['audit-events'] }),
  ]);

  const roleMutation = useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: UserRole }) => updateAdminUserRole(userId, role),
    onSuccess: refreshAdmin,
  });

  const statusMutation = useMutation({
    mutationFn: ({ userId, active }: { userId: string; active: boolean }) => updateAdminUserStatus(userId, active),
    onSuccess: refreshAdmin,
  });

  const createMachineMutation = useMutation({
    mutationFn: createMachine,
    onSuccess: async () => {
      setMachineModalOpen(false);
      await refreshMachines();
    },
  });

  const editMachineMutation = useMutation({
    mutationFn: ({ machineId, draft }: { machineId: string; draft: Omit<MachineDraft, 'status'> }) => updateMachine(machineId, draft),
    onSuccess: async () => {
      setEditingMachine(null);
      await refreshMachines();
    },
  });

  function submitMachine(draft: MachineDraft) {
    if (editingMachine) {
      editMachineMutation.mutate({
        machineId: editingMachine.id,
        draft: {
          productionLineId: draft.productionLineId,
          name: draft.name,
          assetCode: draft.assetCode,
          manufacturer: draft.manufacturer,
          model: draft.model,
          serialNumber: draft.serialNumber,
          installedAt: draft.installedAt,
        },
      });
      return;
    }
    createMachineMutation.mutate(draft);
  }

  const hasError = usersQuery.isError || linesQuery.isError || roleMutation.isError || statusMutation.isError || createMachineMutation.isError || editMachineMutation.isError;
  const savingMachine = createMachineMutation.isPending || editMachineMutation.isPending;

  return (
    <section className="admin-section" id="admin">
      <div className="admin-section-heading">
        <div>
          <span className="eyebrow">Acesso restrito</span>
          <h2>Administração</h2>
          <p>Gerencie ativos, acessos e responsabilidades do LinePulse.</p>
        </div>
        <ShieldCheck size={30} />
      </div>

      {hasError && <div className="connection-banner">Uma ação administrativa não pôde ser concluída.</div>}

      <div className="admin-grid">
        <article className="panel admin-card">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">Estrutura industrial</span>
              <h3>Cadastro de máquinas</h3>
            </div>
            <button className="secondary-button" type="button" onClick={() => setMachineModalOpen(true)} disabled={linesQuery.isLoading || (linesQuery.data?.length ?? 0) === 0}>
              <Plus size={16} /> Nova máquina
            </button>
          </div>

          <div className="admin-list">
            {machines.map((machine) => (
              <div className="admin-machine-row" key={machine.id}>
                <div>
                  <strong>{machine.assetCode} · {machine.name}</strong>
                  <span>{machine.productionLine} · {machine.manufacturer ?? 'Fabricante não informado'}</span>
                </div>
                <button className="admin-icon-button" type="button" onClick={() => setEditingMachine(machine)} aria-label={`Editar ${machine.assetCode}`}>
                  <Pencil size={16} />
                </button>
              </div>
            ))}
          </div>
        </article>

        <article className="panel admin-card">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">Controle de acesso</span>
              <h3>Usuários e perfis</h3>
            </div>
            <UserRoundCog size={22} />
          </div>

          {usersQuery.isLoading && <div className="empty-state">Carregando usuários...</div>}
          <div className="admin-users">
            {(usersQuery.data ?? []).map((user) => {
              const locked = user.demoAccount || user.id === currentUserId;
              return (
                <div className={`admin-user-row ${!user.active ? 'inactive' : ''}`} key={user.id}>
                  <div className="admin-user-copy">
                    <strong>{user.name}</strong>
                    <span>{user.email}</span>
                    {user.demoAccount && <small>Conta demo fixa</small>}
                    {user.id === currentUserId && <small>Sua conta</small>}
                  </div>
                  <select
                    value={user.role}
                    disabled={locked || roleMutation.isPending}
                    onChange={(event) => roleMutation.mutate({ userId: user.id, role: event.target.value as UserRole })}
                    aria-label={`Perfil de ${user.name}`}
                  >
                    {Object.entries(roleLabel).map(([role, label]) => <option key={role} value={role}>{label}</option>)}
                  </select>
                  <button
                    type="button"
                    className={`admin-status-button ${user.active ? 'active' : 'inactive'}`}
                    disabled={locked || statusMutation.isPending}
                    onClick={() => statusMutation.mutate({ userId: user.id, active: !user.active })}
                  >
                    {user.active ? 'Ativo' : 'Inativo'}
                  </button>
                </div>
              );
            })}
          </div>
        </article>
      </div>

      {(machineModalOpen || editingMachine) && (
        <MachineAdminModal
          machine={editingMachine}
          productionLines={linesQuery.data ?? []}
          loading={savingMachine}
          onClose={() => { setMachineModalOpen(false); setEditingMachine(null); }}
          onSubmit={submitMachine}
        />
      )}
    </section>
  );
}

export default AdminPanel;
