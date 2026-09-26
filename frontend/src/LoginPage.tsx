import { FormEvent, useState } from 'react';
import BrandLogo from './brand/BrandLogo';
import { login, storeAuth } from './services/api';
import type { AuthResponse } from './types/api';

type Props = {
  onAuthenticated: (auth: AuthResponse) => void;
};

const demoAccounts = [
  { label: 'Administrador', email: 'admin@linepulse.local' },
  { label: 'Técnico', email: 'technician@linepulse.local' },
  { label: 'Operador', email: 'operator@linepulse.local' },
];

function LoginPage({ onAuthenticated }: Props) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError('');
    setLoading(true);

    try {
      const auth = await login(email, password);
      storeAuth(auth);
      onAuthenticated(auth);
    } catch {
      setError('Não foi possível entrar. Confira e-mail e senha.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth-shell">
      <section className="auth-intro">
        <BrandLogo variant="horizontal" className="auth-brand-lockup" />
        <span className="eyebrow">Operação conectada</span>
        <h1>Operações industriais com contexto, prioridade e histórico.</h1>
        <p>Centralize ocorrências, ordens de manutenção e disponibilidade da planta em uma única visão.</p>
        <div className="auth-note">
          <strong>MVP industrial</strong>
          <span>Máquinas · manutenção · downtime · indicadores</span>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <span className="eyebrow">Acesso ao sistema</span>
          <h2>Entrar no LinePulse</h2>
          <p>Use as credenciais fornecidas pelo administrador da operação.</p>

          <div className="demo-access">
            <span>Perfis demo</span>
            <div className="demo-access-actions">
              {demoAccounts.map((account) => (
                <button type="button" key={account.email} onClick={() => setEmail(account.email)}>
                  {account.label}
                </button>
              ))}
            </div>
          </div>

          <form onSubmit={handleSubmit}>
            <label>
              E-mail
              <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
            </label>
            <label>
              Senha
              <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required minLength={8} />
            </label>

            {error && <div className="auth-error">{error}</div>}

            <button className="primary-button auth-submit" disabled={loading}>
              {loading ? 'Entrando...' : 'Entrar'}
            </button>
          </form>
        </div>
      </section>
    </main>
  );
}

export default LoginPage;
