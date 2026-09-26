import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Activity, AlertTriangle, Factory, LogOut, Repeat2, Timer, Wrench } from 'lucide-react';
import { useState } from 'react';
import AdminPanel from './AdminPanel';
import AlertPanel from './AlertPanel';
import AuditPanel from './AuditPanel';
import DowntimeModal from './DowntimeModal';
import DowntimePanel from './DowntimePanel';
import IncidentHistoryPanel from './IncidentHistoryPanel';
import IncidentModal from './IncidentModal';
import IncidentTrendChart from './IncidentTrendChart';
import LoginPage from './LoginPage';
import MachineDetailPanel from './MachineDetailPanel';
import MachinesPanel from './MachinesPanel';
import MaintenanceHistoryPanel from './MaintenanceHistoryPanel';
import OperationalInsightsPanel from './OperationalInsightsPanel';
import WorkOrderModal from './WorkOrderModal';
import { cancelIncident, clearAuth, closeDowntime, completeWorkOrder, createDowntime, createIncident, createWorkOrder, getAlerts, getAuditEvents, getDashboardMetrics, getDowntimes, getIncidents, getIncidentTrend, getMachines, getStoredAuth, getWorkOrders, resolveIncident, startIncident, startWorkOrder } from './services/api';
import type { AuthResponse, IncidentPriority, Machine, WorkOrderPriority } from './types/api';
import { getApiErrorMessage } from './utils/apiError';

const priorityMeta: Record<IncidentPriority | WorkOrderPriority, { label: string; className: string }> = {
  CRITICAL: { label: 'Crítica', className: 'critical' },
  HIGH: { label: 'Alta', className: 'critical' },
  MEDIUM: { label: 'Média', className: 'warning' },
  LOW: { label: 'Baixa', className: 'neutral' },
};

const roleLabel = { ADMIN: 'Administrador', TECHNICIAN: 'Técnico', OPERATOR: 'Operador' };
const maintenanceTypeLabel = { CORRECTIVE: 'Corretiva', PREVENTIVE: 'Preventiva', INSPECTION: 'Inspeção' };

function Dashboard({ auth, onLogout, onSwitchAccount }: { auth: AuthResponse; onLogout: () => void; onSwitchAccount: () => void }) {
  const queryClient = useQueryClient();
  const [showAccountMenu, setShowAccountMenu] = useState(false);
  const [showIncidentModal, setShowIncidentModal] = useState(false);
  const [showIncidentHistory, setShowIncidentHistory] = useState(false);
  const [showWorkOrderModal, setShowWorkOrderModal] = useState(false);
  const [showMaintenanceHistory, setShowMaintenanceHistory] = useState(false);
  const [showDowntimeModal, setShowDowntimeModal] = useState(false);
  const [selectedMachine, setSelectedMachine] = useState<Machine | null>(null);
  const isAdmin = auth.user.role === 'ADMIN';
  const canManageOperations = isAdmin || auth.user.role === 'TECHNICIAN';
  const dashboardQuery = useQuery({ queryKey: ['dashboard'], queryFn: getDashboardMetrics });
  const trendQuery = useQuery({ queryKey: ['incident-trend'], queryFn: getIncidentTrend });
  const alertsQuery = useQuery({ queryKey: ['alerts'], queryFn: getAlerts, refetchInterval: 30_000 });
  const incidentsQuery = useQuery({ queryKey: ['incidents'], queryFn: getIncidents });
  const machinesQuery = useQuery({ queryKey: ['machines'], queryFn: getMachines });
  const workOrdersQuery = useQuery({ queryKey: ['work-orders'], queryFn: getWorkOrders });
  const downtimesQuery = useQuery({ queryKey: ['downtimes'], queryFn: getDowntimes });
  const auditQuery = useQuery({ queryKey: ['audit-events'], queryFn: getAuditEvents, enabled: canManageOperations, refetchInterval: 15_000 });

  const refreshAudit = () => queryClient.invalidateQueries({ queryKey: ['audit-events'] });
  const refreshAlerts = () => queryClient.invalidateQueries({ queryKey: ['alerts'] });
  const refreshDashboard = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
    queryClient.invalidateQueries({ queryKey: ['work-orders'] }),
    queryClient.invalidateQueries({ queryKey: ['downtimes'] }),
    refreshAlerts(),
    refreshAudit(),
  ]);
  const refreshIncidents = () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ['incidents'] }),
    queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
    refreshAlerts(),
    refreshAudit(),
  ]);

  const createIncidentMutation = useMutation({
    mutationFn: createIncident,
    onSuccess: async () => {
      setShowIncidentModal(false);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['incidents'] }),
        queryClient.invalidateQueries({ queryKey: ['incident-trend'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
        refreshAlerts(),
        refreshAudit(),
      ]);
    },
  });
  const incidentTransitionMutation = useMutation({
    mutationFn: ({ id, action }: { id: string; action: 'start' | 'resolve' | 'cancel' }) => {
      if (action === 'start') return startIncident(id);
      if (action === 'resolve') return resolveIncident(id);
      return cancelIncident(id);
    },
    onSuccess: refreshIncidents,
  });
  const createWorkOrderMutation = useMutation({ mutationFn: createWorkOrder, onSuccess: async () => { setShowWorkOrderModal(false); await refreshDashboard(); } });
  const workOrderTransitionMutation = useMutation({ mutationFn: ({ id, action }: { id: string; action: 'start' | 'complete' }) => action === 'start' ? startWorkOrder(id) : completeWorkOrder(id), onSuccess: refreshDashboard });
  const createDowntimeMutation = useMutation({ mutationFn: createDowntime, onSuccess: async () => { setShowDowntimeModal(false); await refreshDashboard(); } });
  const closeDowntimeMutation = useMutation({ mutationFn: closeDowntime, onSuccess: refreshDashboard });

  function leaveSession(action: () => void) {
    setShowAccountMenu(false);
    queryClient.clear();
    action();
  }

  function scrollToSection(id: string) {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  const dashboard = dashboardQuery.data;
  const availability = dashboard?.availabilityPercentage ?? 0;
  const machines = machinesQuery.data ?? [];
  const incidents = incidentsQuery.data ?? [];
  const orders = workOrdersQuery.data ?? [];
  const downtimes = downtimesQuery.data ?? [];
  const recentIncidents = incidents.filter((incident) => incident.status === 'OPEN' || incident.status === 'IN_PROGRESS').slice(0, 4);
  const recentWorkOrders = orders.filter((order) => order.status === 'OPEN' || order.status === 'IN_PROGRESS').slice(0, 5);
  const selectedMachineData = selectedMachine ? machines.find((machine) => machine.id === selectedMachine.id) ?? selectedMachine : null;
  const metrics = [
    { label: 'Máquinas ativas', value: dashboard?.activeMachines ?? '—', icon: Factory, action: () => scrollToSection('machines') },
    { label: 'Ocorrências abertas', value: dashboard?.openIncidents ?? '—', icon: AlertTriangle, action: () => setShowIncidentHistory(true) },
    { label: 'Ordens em andamento', value: dashboard?.activeWorkOrders ?? '—', icon: Wrench, action: () => setShowMaintenanceHistory(true) },
    { label: 'Disponibilidade', value: dashboard ? `${dashboard.availabilityPercentage.toFixed(1)}%` : '—', icon: Activity, action: () => scrollToSection('downtime') },
    { label: 'MTTR · 30 dias', value: dashboard ? `${dashboard.mttrMinutes.toFixed(1)} min` : '—', icon: Timer, action: () => setShowMaintenanceHistory(true) },
  ];
  const connectionError = [dashboardQuery.error, trendQuery.error, alertsQuery.error, incidentsQuery.error, machinesQuery.error, workOrdersQuery.error, downtimesQuery.error, canManageOperations ? auditQuery.error : null].find(Boolean);
  const mutationError = [createIncidentMutation.error, incidentTransitionMutation.error, createWorkOrderMutation.error, workOrderTransitionMutation.error, createDowntimeMutation.error, closeDowntimeMutation.error].find(Boolean);

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="account-menu">
          <button type="button" className="account-menu-trigger" onClick={() => setShowAccountMenu((open) => !open)} aria-expanded={showAccountMenu} aria-haspopup="menu" title="Conta"><span className="brand-mark">LP</span></button>
          {showAccountMenu && (
            <div className="account-menu-popover" role="menu">
              <div className="account-menu-user"><strong>{auth.user.name}</strong><span>{roleLabel[auth.user.role]}</span></div>
              <button type="button" role="menuitem" onClick={() => leaveSession(onSwitchAccount)}><Repeat2 size={16} />Trocar conta</button>
              <button type="button" role="menuitem" className="account-menu-logout" onClick={() => leaveSession(onLogout)}><LogOut size={16} />Sair</button>
            </div>
          )}
        </div>
        <nav>
          <a className="nav-item active" href="#dashboard">Dashboard</a>
          <a className="nav-item" href="#machines">Máquinas</a>
          <a className="nav-item" href="#incidents">Ocorrências</a>
          <a className="nav-item" href="#maintenance">Manutenção</a>
          {isAdmin && <a className="nav-item admin-nav-item" href="#admin">Administração</a>}
        </nav>
        <div className="sidebar-user"><strong>{auth.user.name}</strong><span>{roleLabel[auth.user.role]}</span></div>
      </aside>

      <section className="content" id="dashboard">
        <header className="page-header">
          <div><span className="eyebrow">Operações industriais</span><h1>Visão geral</h1><p>Acompanhe disponibilidade, ocorrências e manutenção da planta.</p></div>
          <button className="primary-button" onClick={() => setShowIncidentModal(true)} disabled={machinesQuery.isLoading || machines.length === 0}>Nova ocorrência</button>
        </header>

        {connectionError && <div className="connection-banner">{getApiErrorMessage(connectionError, 'Não foi possível carregar todos os dados da API.')}</div>}
        {mutationError && <div className="connection-banner">{getApiErrorMessage(mutationError, 'Uma operação não pôde ser concluída. Revise os dados e tente novamente.')}</div>}

        <div className="metrics-grid">
          {metrics.map(({ label, value, icon: Icon, action }) => (
            <button type="button" className="metric-card metric-card-button" key={label} onClick={action}><div className="metric-icon"><Icon size={18} /></div><span>{label}</span><strong>{value}</strong><small>Ver detalhes</small></button>
          ))}
        </div>

        <AlertPanel alerts={alertsQuery.data ?? []} loading={alertsQuery.isLoading} />
        <IncidentTrendChart data={trendQuery.data ?? []} loading={trendQuery.isLoading} />

        <div className="workspace-grid">
          <section className="panel" id="incidents">
            <div className="panel-heading"><div><span className="eyebrow">Prioridade</span><h2>Ocorrências recentes</h2></div><button className="text-button" type="button" onClick={() => setShowIncidentHistory(true)}>Ver todas</button></div>
            {incidentsQuery.isLoading && <div className="empty-state">Carregando ocorrências...</div>}
            {!incidentsQuery.isLoading && recentIncidents.length === 0 && <div className="empty-state">Nenhuma ocorrência aberta no momento.</div>}
            {recentIncidents.map((incident) => {
              const priority = priorityMeta[incident.priority];
              const machine = machines.find((item) => item.id === incident.machineId);
              return <button type="button" className="incident-row incident-row-button" key={incident.id} onClick={() => machine && setSelectedMachine(machine)}><span className={`status-dot ${priority.className}`} /><div><strong>{incident.assetCode} · {incident.machineName}</strong><small>{incident.title}</small></div><span className={`badge ${priority.className}`}>{priority.label}</span></button>;
            })}
          </section>

          <aside className="panel availability-panel">
            <span className="eyebrow">Disponibilidade</span><h2>{dashboard ? `${availability.toFixed(1)}%` : '—'}</h2><p>Últimas 24 horas</p>
            <div className="availability-bar"><span style={{ width: `${Math.max(0, Math.min(100, availability))}%` }} /></div>
            <small>{dashboard ? `${dashboard.totalMachines} máquinas consideradas no cálculo` : 'Calculando disponibilidade...'}</small>
          </aside>
        </div>

        {showIncidentHistory && <IncidentHistoryPanel incidents={incidents} canManage={canManageOperations} busy={incidentTransitionMutation.isPending} onClose={() => setShowIncidentHistory(false)} onTransition={(id, action) => incidentTransitionMutation.mutate({ id, action })} />}

        <DowntimePanel downtimes={downtimes} canManage={canManageOperations} closing={closeDowntimeMutation.isPending} onNew={() => setShowDowntimeModal(true)} onCloseDowntime={(id) => closeDowntimeMutation.mutate(id)} />
        <MachinesPanel machines={machines} canManage={canManageOperations} isAdmin={isAdmin} onSelect={setSelectedMachine} />
        <OperationalInsightsPanel machines={machines} incidents={incidents} orders={orders} downtimes={downtimes} onSelectMachine={setSelectedMachine} />

        <section className="panel maintenance-panel" id="maintenance">
          <div className="panel-heading">
            <div><span className="eyebrow">Execução</span><h2>Ordens de manutenção</h2></div>
            <div className="maintenance-heading-actions"><button className="text-button" type="button" onClick={() => setShowMaintenanceHistory(true)}>Ver histórico</button>{canManageOperations && <button className="secondary-button" onClick={() => setShowWorkOrderModal(true)} disabled={machines.length === 0}>Nova ordem</button>}</div>
          </div>
          {workOrdersQuery.isLoading && <div className="empty-state">Carregando ordens...</div>}
          {!workOrdersQuery.isLoading && recentWorkOrders.length === 0 && <div className="empty-state">Nenhuma ordem em andamento.</div>}
          <div className="work-order-list">
            {recentWorkOrders.map((order) => {
              const priority = priorityMeta[order.priority];
              return (
                <article className="work-order-row" key={order.id}>
                  <button type="button" className="work-order-copy work-order-machine-link" onClick={() => { const machine = machines.find((item) => item.id === order.machineId); if (machine) setSelectedMachine(machine); }}><strong>{order.assetCode} · {order.title}</strong><small>{maintenanceTypeLabel[order.type]} · {order.machineName}</small></button>
                  <div className="work-order-actions"><span className={`badge ${priority.className}`}>{priority.label}</span>{canManageOperations && order.status === 'OPEN' && <button className="order-action" disabled={workOrderTransitionMutation.isPending} onClick={() => workOrderTransitionMutation.mutate({ id: order.id, action: 'start' })}>Iniciar</button>}{canManageOperations && order.status === 'IN_PROGRESS' && <button className="order-action complete" disabled={workOrderTransitionMutation.isPending} onClick={() => workOrderTransitionMutation.mutate({ id: order.id, action: 'complete' })}>Concluir</button>}</div>
                </article>
              );
            })}
          </div>
        </section>

        {showMaintenanceHistory && <MaintenanceHistoryPanel orders={orders} onClose={() => setShowMaintenanceHistory(false)} />}
        {canManageOperations && <AuditPanel events={auditQuery.data ?? []} loading={auditQuery.isLoading} />}
        {isAdmin && <AdminPanel currentUserId={auth.user.id} machines={machines} />}
      </section>

      {showIncidentModal && <IncidentModal machines={machines.filter((machine) => machine.status !== 'INACTIVE')} loading={createIncidentMutation.isPending} onClose={() => setShowIncidentModal(false)} onSubmit={(draft) => createIncidentMutation.mutate(draft)} />}
      {showWorkOrderModal && canManageOperations && <WorkOrderModal machines={machines.filter((machine) => machine.status !== 'INACTIVE')} loading={createWorkOrderMutation.isPending} onClose={() => setShowWorkOrderModal(false)} onSubmit={(draft) => createWorkOrderMutation.mutate(draft)} />}
      {showDowntimeModal && canManageOperations && <DowntimeModal machines={machines.filter((machine) => machine.status !== 'INACTIVE')} loading={createDowntimeMutation.isPending} onClose={() => setShowDowntimeModal(false)} onSubmit={(draft) => createDowntimeMutation.mutate(draft)} />}
      {selectedMachineData && <MachineDetailPanel machine={selectedMachineData} incidents={incidents} orders={orders} downtimes={downtimes} onClose={() => setSelectedMachine(null)} />}
    </main>
  );
}

function App() {
  const [auth, setAuth] = useState<AuthResponse | null>(() => getStoredAuth());
  function logout() { clearAuth(); setAuth(null); }
  function switchAccount() { clearAuth(); setAuth(null); }
  if (!auth) return <LoginPage onAuthenticated={setAuth} />;
  return <Dashboard auth={auth} onLogout={logout} onSwitchAccount={switchAccount} />;
}

export default App;
