import { useQuery } from '@tanstack/react-query';
import { Activity, AlertTriangle, Factory, LogOut, Wrench } from 'lucide-react';
import { useState } from 'react';
import LoginPage from './LoginPage';
import { clearAuth, getDashboardMetrics, getIncidents, getStoredAuth } from './services/api';
import type { AuthResponse, IncidentPriority } from './types/api';

const priorityMeta: Record<IncidentPriority, { label: string; className: string }> = {
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

function Dashboard({ auth, onLogout }: { auth: AuthResponse; onLogout: () => void }) {
  const dashboardQuery = useQuery({ queryKey: ['dashboard'], queryFn: getDashboardMetrics });
  const incidentsQuery = useQuery({ queryKey: ['incidents'], queryFn: getIncidents });
  const dashboard = dashboardQuery.data;
  const availability = dashboard?.availabilityPercentage ?? 0;
  const recentIncidents = (incidentsQuery.data ?? [])
    .filter((incident) => incident.status === 'OPEN' || incident.status === 'IN_PROGRESS')
    .slice(0, 4);
  const metrics = [
    { label: 'Máquinas ativas', value: dashboard?.activeMachines ?? '—', icon: Factory },
    { label: 'Ocorrências abertas', value: dashboard?.openIncidents ?? '—', icon: AlertTriangle },
    { label: 'Ordens em andamento', value: dashboard?.activeWorkOrders ?? '—', icon: Wrench },
    { label: 'Disponibilidade', value: dashboard ? `${dashboard.availabilityPercentage.toFixed(1)}%` : '—', icon: Activity },
  ];
  const hasConnectionError = dashboardQuery.isError || incidentsQuery.isError;

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

      <section className="content">
        <header className="page-header">
          <div>
            <span className="eyebrow">Operações industriais</span>
            <h1>Visão geral</h1>
            <p>Acompanhe disponibilidade, ocorrências e manutenção da planta.</p>
          </div>
          <button className="primary-button">Nova ocorrência</button>
        </header>

        {hasConnectionError && (
          <div className="connection-banner">Não foi possível carregar os dados da API.</div>
        )}

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
          <section className="panel">
            <div className="panel-heading">
              <div>
                <span className="eyebrow">Prioridade</span>
                <h2>Ocorrências recentes</h2>
              </div>
              <button className="text-button">Ver todas</button>
            </div>

            {incidentsQuery.isLoading && <div className="empty-state">Carregando ocorrências...</div>}
            {!incidentsQuery.isLoading && recentIncidents.length === 0 && (
              <div className="empty-state">Nenhuma ocorrência aberta no momento.</div>
            )}
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
      </section>
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
