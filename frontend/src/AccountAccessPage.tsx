import { useState, type FormEvent } from 'react';
import api from './services/api';
import { getApiErrorMessage } from './utils/apiError';

export default function AccountAccessPage({ token, onBack }: { token?: string; onBack: () => void }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent) {
    event.preventDefault(); setError(''); setMessage('');
    if (token && password !== confirmation) { setError('As senhas precisam ser iguais.'); return; }
    setBusy(true);
    try {
      const response = await api.post<{ message: string }>(token ? '/auth/complete-account' : '/auth/password-reset', token ? { token, password } : { email });
      setMessage(response.data.message); setPassword(''); setConfirmation('');
      if (token) window.history.replaceState(null, '', window.location.pathname + window.location.search);
    } catch (e) { setError(getApiErrorMessage(e, 'Não foi possível concluir. Tente novamente.')); }
    finally { setBusy(false); }
  }
  return <main className="auth-shell"><section className="auth-panel"><div className="auth-card">
    <h1>{token ? 'Definir senha' : 'Recuperar senha'}</h1>
    <form onSubmit={submit}>
      {token ? <><label>Nova senha<input type="password" autoComplete="new-password" minLength={12} maxLength={64} value={password} onChange={e => setPassword(e.target.value)} required /></label>
        <label>Confirmar senha<input type="password" autoComplete="new-password" minLength={12} maxLength={64} value={confirmation} onChange={e => setConfirmation(e.target.value)} required /></label></>
        : <label>E-mail<input type="email" autoComplete="email" maxLength={254} value={email} onChange={e => setEmail(e.target.value)} required /></label>}
      {error && <p role="alert">{error}</p>}{message && <p role="status">{message}</p>}
      <button className="primary-button" disabled={busy || Boolean(token && message)}>{busy ? 'Enviando...' : token ? 'Salvar senha' : 'Enviar instruções'}</button>
    </form><button className="text-button" onClick={onBack}>Voltar para entrar</button>
  </div></section></main>;
}
