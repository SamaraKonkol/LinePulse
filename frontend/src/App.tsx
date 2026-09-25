import { Activity, AlertTriangle, Factory, Wrench } from 'lucide-react';

const metrics = [
  { label: 'Máquinas ativas', value: '42', icon: Factory },
  { label: 'Ocorrências abertas', value: '7', icon: AlertTriangle },
  { label: 'Ordens em andamento', value: '5', icon: Wrench },
  { label: 'Disponibilidade', value: '96,8%', icon: Activity },
];

function App() {
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
            <div className="incident-row">
              <span className="status-dot critical" />
              <div><strong>Prensa PR-04</strong><small>Temperatura acima do limite</small></div>
              <span className="badge">Crítica</span>
            </div>
            <div className="incident-row">
              <span className="status-dot warning" />
              <div><strong>Esteira ES-12</strong><small>Ruído irregular no acionamento</small></div>
              <span className="badge neutral">Média</span>
            </div>
          </section>

          <aside className="panel availability-panel">
            <span className="eyebrow">Disponibilidade</span>
            <h2>96,8%</h2>
            <p>Últimas 24 horas</p>
            <div className="availability-bar"><span /></div>
            <small>Meta operacional: 95%</small>
          </aside>
        </div>
      </section>
    </main>
  );
}

export default App;
