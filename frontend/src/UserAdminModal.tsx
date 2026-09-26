import { FormEvent, useState } from 'react';
import type { UserRole } from './types/api';
import './incident-modal.css';

type UserDraft = {
  name: string;
  email: string;
  password: string;
  role: UserRole;
};

type Props = {
  loading: boolean;
  onClose: () => void;
  onSubmit: (draft: UserDraft) => void;
};

function UserAdminModal({ loading, onClose, onSubmit }: Props) {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<UserRole>('OPERATOR');

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onSubmit({ name, email, password, role });
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="user-admin-title" onMouseDown={(event) => event.stopPropagation()}>
        <div className="modal-header">
          <div>
            <span className="eyebrow">Controle de acesso</span>
            <h2 id="user-admin-title">Criar usuário</h2>
          </div>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Fechar">×</button>
        </div>

        <form className="incident-form" onSubmit={handleSubmit}>
          <label>
            Nome
            <input value={name} onChange={(event) => setName(event.target.value)} required minLength={2} maxLength={120} />
          </label>

          <label>
            E-mail
            <input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required maxLength={180} />
          </label>

          <label>
            Senha inicial
            <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required minLength={8} maxLength={72} />
          </label>

          <label>
            Perfil
            <select value={role} onChange={(event) => setRole(event.target.value as UserRole)}>
              <option value="OPERATOR">Operador</option>
              <option value="TECHNICIAN">Técnico</option>
              <option value="ADMIN">Administrador</option>
            </select>
          </label>

          <div className="modal-actions">
            <button type="button" className="secondary-button" onClick={onClose}>Cancelar</button>
            <button className="primary-button" disabled={loading}>{loading ? 'Criando...' : 'Criar usuário'}</button>
          </div>
        </form>
      </section>
    </div>
  );
}

export default UserAdminModal;
