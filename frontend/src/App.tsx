import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Activity, AlertTriangle, Factory, LogOut, Wrench } from 'lucide-react';
import { useState } from 'react';
import IncidentModal from './IncidentModal';
import LoginPage from './LoginPage';
import WorkOrderModal from './WorkOrderModal';
import { clearAuth, createIncident, createWorkOrder, getDashboardMetrics, getIncidents, getMachines, getStoredAuth, getWorkOrders } from './services/api';
import type { AuthResponse, IncidentPriority, WorkOrderPriority } from './types/api';

const priorityMeta: Record<IncidentPriority | WorkOrderPriority, { label: string; className: string }> = {
  CRITICAL: { label: 'Crítica', className: 'critical' },
  HIGH: { label: 'Alta', className: 'critical' },
  MEDIUM: { label: 'Média', className: 'warning' },
  LOW: { label: 'Baixa', className: 'neutral' },
};

const roleLabel = {
  ADMIN: 'Administrador',
  TECHNICIAN: 'Técnico',
  OPERATOR: 'Operador',
};

const maintenanceTypeLabel = {
  CORRECTIVE: 'Corretiva',
  PREVENTIVE: 'Preventiva',
  INSPECTION: 'Inspeção',
};

function Dashboard({ auth, onLogout }: { auth: AuthResponse; onLogout: () => void }) {
  const queryClient = useQueryClient();
  const [showIncidentModal, setShowIncidentModal] = useState(false);
  const [showWorkOrderModal, setShowWorkOrderModal] = useState(false);
  const dashboardQuery = useQuery({ queryKey: ['dashboard'], queryFn: getDashboardMetrics });
  const incidentsQuery = useQuery({ queryKey: ['incidents'], queryFn: getIncidents });
  const machinesQuery = useQuery({ queryKey: ['machines'], queryFn: getMachines });
  const workOrdersQuery = useQuery({ queryKey: ['work-orders'], queryFn: getWorkOrders });
  const canManageMaintenance = auth.user.role === 'ADMIN' || auth.user.role === 'TECHNICIAN';

  const createIncidentMutation = useMutation({
    mutationFn: createIncident,
    onSuccess: async () => {
      setShowIncidentModal(false);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['incidents'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
      ]);
    },
  });

  const createWorkOrderMutation = useMutation({
    mutationFn: createWorkOrder,
    onSuccess: async () => {
      setShowWorkOrderModal(false);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['work-orders'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
      ]);
    },
  });

  const dashboard = dashboardQuery.data;
  const availability = dashboard?.availabilityPercentage ?? 0;
  const recentIncidents = (incidentsQuery.data ?? [])
    .filter((incident) => incident.status === 'OPEN' || incident.status === 'IN_PROGRESS')
    .slice(0, 4);
  const recentWorkOrders = (workOrdersQuery.data ?? [])
    .filter((order) => order.status === 'OPEN' || order.status === 'IN_PROGRESS')
    .slice(0, 5);
  const metrics = [
    { label: 'Máquinas ativas', value: dashboard?.activeMachines ?? '—', icon: Factory },
    { label: 'Ocorrências abertas', value: dashboard?.openIncidents ?? '—', icon: AlertTriangle },
    { label: 'Ordens em andamento', value: dashboard?.activeWorkOrders ?? '—', icon: Wrench },
    { label: 'Disponibilidade', value: dashboard ? `${dashboard.availabilityPercentage.toFixed(1)}%` : '—', icon: Activity },
  ];
  const hasConnectionError = dashboardQuery.isError || incidentsQuery.isError || machinesQuery.isError || workOrdersQuery.isError;

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="brand-mark">LP</div>
        <nav>
          <a className="nav-item active" href="#dashboard">Dashboard</a>
          <a className="nav-item" href="#machines">Máquinas</a>
          <a className="nav-item" href="#incidents">Ocorrências</a>
          <a className="nav-item" href="#maintenance">Manutenção</a>
        </nav>
        <div className="sidebar-user">
          <strong>{auth.user.name}</strong>
          <span>{roleLabel[auth.user.role]}</span>
          <button type="button" onClick={onLogout}><LogOut size={15} /> Sair</button>
        </div>
      </aside>

      <section className="content" id="dashboard">
        <header className="page-header">
          <div>
            <span className="eyebrow">Operações industriais</span>
            <h1>Visão geral</h1>
            <p>Acompanhe disponibilidade, ocorrências e manutenção da planta.</p>
          </div>
          <button className="primary-button" onClick={() => setShowIncidentModal(true)} disabled={machinesQuery.isLoading || (machinesQuery.data?.length ?? 0) === 0}>
            Nova ocorrência
          </button>
        </header>

        {hasConnectionError && <div className="connection-banner">Não foi possível carregar todos os dados da API.</div>}
        {createIncidentMutation.isError && <div className="connection-banner">Não foi possível registrar a ocorrência.</div>}
        {createWorkOrderMutation.isError && <div className="connection-banner">Não foi possível criar a ordem de manutenção.</div>}

        <div className="metrics-grid">
          {metrics.map(({ label, value, icon: Icon }) => (
            <article className="metric-card" key={label}>
              <div className="metric-icon"><Icon size={18} /></div>
              <span>{label}</span>
              <strong>{value}</strong>
            </article>
          ))}
        </div>

        <div className="workspace-grid">
          <section className="panel" id="incidents">
            <div className="panel-heading">
              <div>
                <span className="eyebrow">Prioridade</span>
                <h2>Ocorrências recentes</h2>
              </div>
              <button className="text-button">Ver todas</button>
            </div>

            {incidentsQuery.isLoading && <div className="empty-state">Carregando ocorrências...</div>}
            {!incidentsQuery.isLoading && recentIncidents.length === 0 && <div className="empty-state">Nenhuma ocorrência aberta no momento.</div>}
            {recentIncidents.map((incident) => {
              const priority = priorityMeta[incident.priority];
              return (
                <div className="incident-row" key={incident.id}>
                  <span className={`status-dot ${priority.className}`} />
                  <div>
                    <strong>{incident.assetCode} · {incident.machineName}</strong>
                    <small>{incident.title}</small>
                  </div>
                  <span className={`badge ${priority.className}`}>{priority.label}</span>
                </div>
              );
            })}
          </section>

          <aside className="panel availability-panel">
            <span className="eyebrow">Disponibilidade</span>
            <h2>{dashboard ? `${availability.toFixed(1)}%` : '—'}</h2>
            <p>Últimas 24 horas</p>
            <div className="availability-bar">
              <span style={{ width: `${Math.max(0, Math.min(100, availability))}%` }} />
            </div>
            <small>{dashboard ? `${dashboard.totalMachines} máquinas consideradas no cálculo` : 'Calculando disponibilidade...'}</small>
          </aside>
        </div>

        <section className="panel maintenance-panel" id="maintenance">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">Execução</span>
              <h2>Ordens de manutenção</h2>
            </div>
            {canManageMaintenance && (
              <button className="secondary-button" onClick={() => setShowWorkOrderModal(true)} disabled={(machinesQuery.data?.length ?? 0) === 0}>
                Nova ordem
              </button>
            )}
          </div>

          {workOrdersQuery.isLoading && <div className="empty-state">Carregando ordens...</div>}
          {!workOrdersQuery.isLoading && recentWorkOrders.length === 0 && <div className="empty-state">Nenhuma ordem em andamento.</div>}
          <div className="work-order-list">
            {recentWorkOrders.map((order) => {
              const priority = priorityMeta[order.priority];
              return (
                <article className="work-order-row" key={order.id}>
                  <div>
                    <strong>{order.assetCode} · {order.title}</strong>
                    <small>{maintenanceTypeLabel[order.type]} · {order.machineName}</small>
                  </div>
                  <span className={`badge ${priority.className}`}>{priority.label}</span>
                </article>
              );
            })}
          </div>
        </section>
      </section>

      {showIncidentModal && (
        <IncidentModal
          machines={machinesQuery.data ?? []}
          loading={createIncidentMutation.isPending}
          onClose={() => setShowIncidentModal(false)}
          onSubmit={(draft) => createIncidentMutation.mutate(draft)}
        />
      )}

      {showWorkOrderModal && canManageMaintenance && (
        <WorkOrderModal
          machines={machinesQuery.data ?? []}
          loading={createWorkOrderMutation.isPending}
          onClose={() => setShowWorkOrderModal(false)}
          onSubmit={(draft) => createWorkOrderMutation.mutate(draft)}
        />
      )}
    </main>
  );
}

function App() {
  const [auth, setAuth] = useState<AuthResponse | null>(() => getStoredAuth());

  function logout() {
    clearAuth();
    setAuth(null);
  }

  if (!auth) {
    return <LoginPage onAuthenticated={setAuth} />;
  }

  return <Dashboard auth={auth} onLogout={logout} />;
}

export default App;
