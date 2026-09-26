import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Plus, ShieldCheck, Trash2 } from 'lucide-react';
import { useMemo, useState } from 'react';
import ConfirmDialog from './ConfirmDialog';
import MachineAdminModal, { type MachineDraft } from './MachineAdminModal';
import StructureAdminPanel from './StructureAdminPanel';
import UserAdminModal from './UserAdminModal';
import { createAdminUser, createMachine, deleteAdminUser, getAdminUsers, getProductionLines, updateAdminUserRole, updateAdminUserStatus, updateMachine, updateMachineStatus } from './services/api';
import type { Machine, ProductionLine, UserRole } from './types/api';
import { getApiErrorMessage } from './utils/apiError';
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
  const [deleteTarget, setDeleteTarget] = useState<{ id: string; name: string; registration: string } | null>(null);
  const [userSearch, setUserSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState<'ALL' | UserRole>('ALL');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  const usersQuery = useQuery({ queryKey: ['admin-users'], queryFn: getAdminUsers, retry: 1 });
  const linesQuery = useQuery({ queryKey: ['admin-production-lines'], queryFn: getProductionLines, retry: 1 });

  const fallbackProductionLines: ProductionLine[] = Array.from(
    new Map(
      machines.map((machine) => [
        machine.productionLineId,
        {
          id: machine.productionLineId,
          sectorId: machine.sectorId,
          sector: machine.sector,
          plantId: machine.plantId,
          plant: machine.plant,
          name: machine.productionLine,
          code: machine.productionLine,
          active: true,
        },
      ])
    ).values()
  );

  const productionLines = ((linesQuery.data?.length ?? 0) > 0 ? linesQuery.data! : fallbackProductionLines).filter((line) => line.active);

  const filteredUsers = useMemo(() => {
    const search = userSearch.trim().toLowerCase();
    return (usersQuery.data ?? []).filter((user) => {
      const matchesSearch = !search || user.name.toLowerCase().includes(search) || user.registration.toLowerCase().includes(search);
      const matchesRole = roleFilter === 'ALL' || user.role === roleFilter;
      const matchesStatus = statusFilter === 'ALL' || (statusFilter === 'ACTIVE' ? user.active : !user.active);
      return matchesSearch && matchesRole && matchesStatus;
    });
  }, [usersQuery.data, userSearch, roleFilter, statusFilter]);

  const refreshAdmin = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ['admin-users'] }),
    queryClient.invalidateQueries({ queryKey: ['audit-events'] }),
  ]);

  const refreshMachines = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ['machines'] }),
    queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
    queryClient.invalidateQueries({ queryKey: ['alerts'] }),
    queryClient.invalidateQueries({ queryKey: ['audit-events'] }),
  ]);

  const createUserMutation = useMutation({
    mutationFn: createAdminUser,
    onSuccess: async () => { setUserModalOpen(false); await refreshAdmin(); },
  });
  const roleMutation = useMutation({ mutationFn: ({ userId, role }: { userId: string; role: UserRole }) => updateAdminUserRole(userId, role), onSuccess: refreshAdmin });
  const statusMutation = useMutation({ mutationFn: ({ userId, active }: { userId: string; active: boolean }) => updateAdminUserStatus(userId, active), onSuccess: refreshAdmin });
  const deleteUserMutation = useMutation({
    mutationFn: deleteAdminUser,
    onSuccess: async () => { setDeleteTarget(null); await refreshAdmin(); },
  });
  const createMachineMutation = useMutation({
    mutationFn: createMachine,
    onSuccess: async () => { setMachineModalOpen(false); await refreshMachines(); },
  });
  const editMachineMutation = useMutation({
    mutationFn: ({ machineId, draft }: { machineId: string; draft: Omit<MachineDraft, 'status'> }) => updateMachine(machineId, draft),
    onSuccess: async () => { setEditingMachine(null); await refreshMachines(); },
  });
  const machineLifecycleMutation = useMutation({
    mutationFn: ({ machineId, inactive }: { machineId: string; inactive: boolean }) => updateMachineStatus(machineId, inactive ? 'INACTIVE' : 'RUNNING'),
    onSuccess: refreshMachines,
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

  const mutationError = [createUserMutation.error, roleMutation.error, statusMutation.error, deleteUserMutation.error, createMachineMutation.error, editMachineMutation.error, machineLifecycleMutation.error].find(Boolean);
  const savingMachine = createMachineMutation.isPending || editMachineMutation.isPending;

  return (
    <section className="admin-section" id="admin">
      <div className="admin-section-heading">
        <div>
          <span className="eyebrow">Acesso restrito</span>
          <h2>Administração</h2>
          <p>Gerencie estrutura industrial, ativos, acessos e responsabilidades do LinePulse.</p>
        </div>
        <ShieldCheck size={30} />
      </div>

      {mutationError && <div className="connection-banner">{getApiErrorMessage(mutationError, 'Uma ação administrativa não pôde ser concluída.')}</div>}
      {linesQuery.isError && productionLines.length > 0 && <div className="admin-inline-note">{getApiErrorMessage(linesQuery.error, 'Linhas carregadas a partir dos ativos existentes porque a sincronização administrativa falhou.')}</div>}

      <StructureAdminPanel />

      <div className="admin-grid">
        <article className="panel admin-card">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">Ciclo de vida</span>
              <h3>Cadastro de máquinas</h3>
            </div>
            <button className="secondary-button" type="button" onClick={() => setMachineModalOpen(true)} disabled={productionLines.length === 0}>
              <Plus size={16} /> Nova máquina
            </button>
          </div>

          {productionLines.length === 0 && <div className="empty-state">Cadastre e ative uma linha de produção antes de adicionar máquinas.</div>}

          <div className="admin-list">
            {machines.map((machine) => (
              <div className={`admin-machine-row ${machine.status === 'INACTIVE' ? 'inactive' : ''}`} key={machine.id}>
                <div>
                  <strong>{machine.assetCode} · {machine.name}</strong>
                  <span>{machine.plant} · {machine.sector} · {machine.productionLine}</span>
                </div>
                <button className="admin-status-button" type="button" disabled={machineLifecycleMutation.isPending} onClick={() => machineLifecycleMutation.mutate({ machineId: machine.id, inactive: machine.status !== 'INACTIVE' })}>
                  {machine.status === 'INACTIVE' ? 'Reativar' : 'Desativar'}
                </button>
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

          <div className="admin-filters">
            <input type="search" placeholder="Buscar nome ou cadastro" value={userSearch} onChange={(event) => setUserSearch(event.target.value)} />
            <select value={roleFilter} onChange={(event) => setRoleFilter(event.target.value as 'ALL' | UserRole)}>
              <option value="ALL">Todos os perfis</option>
              <option value="OPERATOR">Operadores</option>
              <option value="TECHNICIAN">Técnicos</option>
              <option value="ADMIN">Administradores</option>
            </select>
            <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as 'ALL' | 'ACTIVE' | 'INACTIVE')}>
              <option value="ALL">Ativos e inativos</option>
              <option value="ACTIVE">Ativos</option>
              <option value="INACTIVE">Inativos</option>
            </select>
          </div>

          {usersQuery.isLoading && <div className="empty-state">Carregando usuários...</div>}
          {usersQuery.isError && (
            <div className="empty-state">
              {getApiErrorMessage(usersQuery.error, 'Não foi possível carregar os usuários.')}
              {' '}
              <button className="text-button" type="button" disabled={usersQuery.isFetching} onClick={() => usersQuery.refetch()}>
                {usersQuery.isFetching ? 'Tentando novamente...' : 'Tentar novamente'}
              </button>
            </div>
          )}
          {!usersQuery.isLoading && !usersQuery.isError && filteredUsers.length === 0 && <div className="empty-state">Nenhum usuário encontrado com esses filtros.</div>}
          <div className="admin-users">
            {filteredUsers.map((user) => {
              const locked = user.demoAccount || user.id === currentUserId;
              return (
                <div className={`admin-user-row ${!user.active ? 'inactive' : ''}`} key={user.id}>
                  <div className="admin-user-copy">
                    <strong>{user.name}</strong>
                    <span>Cadastro {user.registration}</span>
                    {user.demoAccount && <small>Conta demo fixa</small>}
                    {user.id === currentUserId && <small>Sua conta</small>}
                  </div>
                  <select value={user.role} disabled={locked || roleMutation.isPending} onChange={(event) => roleMutation.mutate({ userId: user.id, role: event.target.value as UserRole })} aria-label={`Perfil de ${user.name}`}>
                    {Object.entries(roleLabel).map(([role, label]) => <option key={role} value={role}>{label}</option>)}
                  </select>
                  <button type="button" className={`admin-status-button ${user.active ? 'active' : 'inactive'}`} disabled={locked || statusMutation.isPending} onClick={() => statusMutation.mutate({ userId: user.id, active: !user.active })}>
                    {user.active ? 'Ativo' : 'Inativo'}
                  </button>
                  <button type="button" className="admin-delete-button" disabled={locked || deleteUserMutation.isPending} onClick={() => setDeleteTarget({ id: user.id, name: user.name, registration: user.registration })} aria-label={`Excluir ${user.name}`} title={locked ? 'Esta conta não pode ser excluída' : 'Excluir usuário'}>
                    <Trash2 size={16} />
                  </button>
                </div>
              );
            })}
          </div>
        </article>
      </div>

      {(machineModalOpen || editingMachine) && <MachineAdminModal machine={editingMachine} productionLines={productionLines} loading={savingMachine} onClose={() => { setMachineModalOpen(false); setEditingMachine(null); }} onSubmit={submitMachine} />}
      {userModalOpen && <UserAdminModal loading={createUserMutation.isPending} onClose={() => setUserModalOpen(false)} onSubmit={(draft) => createUserMutation.mutate(draft)} />}
      {deleteTarget && <ConfirmDialog title={`Excluir ${deleteTarget.name}?`} description={`O cadastro ${deleteTarget.registration} perderá o acesso imediatamente. O histórico de auditoria será preservado.`} confirmLabel="Excluir usuário" destructive busy={deleteUserMutation.isPending} onClose={() => setDeleteTarget(null)} onConfirm={() => deleteUserMutation.mutate(deleteTarget.id)} />}
    </section>
  );
}

export default AdminPanel;
