import { FormEvent, useState } from 'react';
import BrandLogo from './brand/BrandLogo';
import { login, register, storeAuth } from './services/api';
import type { AuthResponse } from './types/api';

type Props = {
  onAuthenticated: (auth: AuthResponse) => void;
};

function LoginPage({ onAuthenticated }: Props) {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError('');
    setLoading(true);

    try {
      const auth = mode === 'login'
        ? await login(email, password)
        : await register(name, email, password);
      storeAuth(auth);
      onAuthenticated(auth);
    } catch {
      setError(mode === 'login'
        ? 'Não foi possível entrar. Confira e-mail e senha.'
        : 'Não foi possível criar o acesso. Verifique os dados informados.');
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
          <h2>{mode === 'login' ? 'Entrar no LinePulse' : 'Criar acesso'}</h2>
          <p>{mode === 'login' ? 'Use suas credenciais para acessar a operação.' : 'Novos usuários entram inicialmente como operadores.'}</p>

          <form onSubmit={handleSubmit}>
            {mode === 'register' && (
              <label>
                Nome
                <input value={name} onChange={(event) => setName(event.target.value)} required minLength={2} />
              </label>
            )}
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
              {loading ? 'Processando...' : mode === 'login' ? 'Entrar' : 'Criar acesso'}
            </button>
          </form>

          <button className="auth-switch" type="button" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>
            {mode === 'login' ? 'Ainda não tenho acesso' : 'Já tenho uma conta'}
          </button>
        </div>
      </section>
    </main>
  );
}

export default LoginPage;
