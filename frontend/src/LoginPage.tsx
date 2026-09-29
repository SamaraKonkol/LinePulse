import { FormEvent, useState } from 'react';
import AccountAccessPage from './AccountAccessPage';
import BrandLogo from './brand/BrandLogo';
import { login, storeAuth } from './services/api';
import type { AuthResponse } from './types/api';

type Props = {
  onAuthenticated: (auth: AuthResponse) => void;
};

const demoAccounts = [
  { label: 'Administrador', registration: 'ADM001' },
  { label: 'Técnico', registration: 'TEC001' },
  { label: 'Operador', registration: 'OPE001' },
];

function LoginPage({ onAuthenticated }: Props) {
  const [recover, setRecover] = useState(false);
  const [accountToken, setAccountToken] = useState(() => window.location.hash.startsWith('#account-token=') ? window.location.hash.slice(15) : '');
  const [registration, setRegistration] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError('');
    setLoading(true);

    try {
      const auth = await login(registration, password);
      storeAuth(auth);
      onAuthenticated(auth);
    } catch (requestError: unknown) {
      const status = typeof requestError === 'object' && requestError !== null && 'response' in requestError
        ? (requestError as { response?: { status?: number } }).response?.status
        : undefined;
      setError(status === 429 ? 'Muitas tentativas. Aguarde alguns minutos antes de tentar novamente.' : 'Não foi possível entrar. Confira cadastro e senha.');
    } finally {
      setLoading(false);
    }
  }

  if (recover || accountToken) return <AccountAccessPage token={accountToken || undefined} onBack={() => { setRecover(false); setAccountToken(''); window.history.replaceState(null, '', window.location.pathname + window.location.search); }} />;

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
          <p>Use seu cadastro e a senha definida no convite por e-mail.</p>

          {import.meta.env.DEV && <div className="demo-access">
            <span>Perfis demo locais</span>
            <div className="demo-access-actions">
              {demoAccounts.map((account) => (
                <button type="button" key={account.registration} onClick={() => setRegistration(account.registration)}>
                  {account.label}
                </button>
              ))}
            </div>
          </div>}

          <form onSubmit={handleSubmit}>
            <label>
              Cadastro
              <input value={registration} onChange={(event) => setRegistration(event.target.value.toUpperCase())} required maxLength={40} autoComplete="username" />
            </label>
            <label>
              Senha
              <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required minLength={8} autoComplete="current-password" />
            </label>

            {error && <div className="auth-error">{error}</div>}

            <button className="primary-button auth-submit" disabled={loading}>
              {loading ? 'Entrando...' : 'Entrar'}
            </button>
          </form>
          <button type="button" className="text-button" onClick={() => setRecover(true)}>Esqueci minha senha</button>
        </div>
      </section>
    </main>
  );
}

export default LoginPage;
