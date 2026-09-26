import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Plus, ShieldCheck, UserRoundCog } from 'lucide-react';
import { useState } from 'react';
import MachineAdminModal, { type MachineDraft } from './MachineAdminModal';
import UserAdminModal from './UserAdminModal';
import { createAdminUser, createMachine, getAdminUsers, getProductionLines, updateAdminUserRole, updateAdminUserStatus, updateMachine } from './services/api';
import type { Machine, ProductionLine, UserRole } from './types/api';
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
  const [userModalOpen, setUserModalOpen] = useState(false);
  const [editingMachine, setEditingMachine] = useState<Machine | null>(null);
  const usersQuery = useQuery({ queryKey: ['admin-users'], queryFn: getAdminUsers });
  const linesQuery = useQuery({ queryKey: ['admin-production-lines'], queryFn: getProductionLines });

  const fallbackProductionLines: ProductionLine[] = Array.from(
    new Map(
      machines.map((machine) => [
        machine.productionLineId,
        {
          id: machine.productionLineId,
          name: machine.productionLine,
          code: machine.productionLine,
          active: true,
        },
      ])
    ).values()
  );

  const productionLines = (linesQuery.data?.length ?? 0) > 0 ? linesQuery.data! : fallbackProductionLines;

  const refreshAdmin = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ['admin-users'] }),
    queryClient.invalidateQueries({ queryKey: ['audit-events'] }),
  ]);

  const refreshMachines = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ['machines'] }),
    queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
    queryClient.invalidateQueries({ queryKey: ['audit-events'] }),
  ]);

  const createUserMutation = useMutation({
    mutationFn: createAdminUser,
    onSuccess: async () => {
      setUserModalOpen(false);
      await refreshAdmin();
    },
  });

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

  const mutationError = createUserMutation.isError || roleMutation.isError || statusMutation.isError || createMachineMutation.isError || editMachineMutation.isError;
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

      {mutationError && <div className="connection-banner">Uma ação administrativa não pôde ser concluída.</div>}
      {linesQuery.isError && productionLines.length > 0 && <div className="admin-inline-note">Linhas carregadas a partir dos ativos existentes. A sincronização administrativa será retomada quando a API estiver disponível.</div>}

      <div className="admin-grid">
        <article className="panel admin-card">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">Estrutura industrial</span>
              <h3>Cadastro de máquinas</h3>
            </div>
            <button className="secondary-button" type="button" onClick={() => setMachineModalOpen(true)} disabled={productionLines.length === 0}>
              <Plus size={16} /> Nova máquina
            </button>
          </div>

          {productionLines.length === 0 && <div className="empty-state">Nenhuma linha de produção disponível para cadastrar uma máquina.</div>}

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
            <button className="secondary-button" type="button" onClick={() => setUserModalOpen(true)}>
              <Plus size={16} /> Novo usuário
            </button>
          </div>

          {usersQuery.isLoading && <div className="empty-state">Carregando usuários...</div>}
          {usersQuery.isError && <div className="empty-state">A gestão de usuários ainda não respondeu pela API.</div>}
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
          productionLines={productionLines}
          loading={savingMachine}
          onClose={() => { setMachineModalOpen(false); setEditingMachine(null); }}
          onSubmit={submitMachine}
        />
      )}

      {userModalOpen && (
        <UserAdminModal
          loading={createUserMutation.isPending}
          onClose={() => setUserModalOpen(false)}
          onSubmit={(draft) => createUserMutation.mutate(draft)}
        />
      )}
    </section>
  );
}

export default AdminPanel;
