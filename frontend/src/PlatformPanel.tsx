import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import api from './services/api';
import './platform.css';
import { clearActiveOrganization, setActiveOrganization } from './services/serviceNetworkApi';
import { getApiErrorMessage } from './utils/apiError';
import type { OrganizationType, OrganizationRole } from './types/serviceNetwork';

type Organization = { id: string; name: string; slug: string; type: OrganizationType; active: boolean };
type Audit = { id: number; actor_registration: string; organization_id: string | null; action: string; detail: string; created_at: string };
const empty = { name: '', slug: '', type: 'COMPANY' as OrganizationType, ownerName: '', ownerRegistration: '', ownerEmail: '' };

export default function PlatformPanel({ onLogout }: { onLogout: () => void }) {
  const client = useQueryClient();
  const [draft, setDraft] = useState(empty);
  const [role, setRole] = useState<OrganizationRole>('OWNER');
  const [success, setSuccess] = useState('');
  const [page, setPage] = useState(0);
  const organizations = useQuery({ queryKey: ['platform-organizations'], queryFn: async () => (await api.get<Organization[]>('/platform/organizations')).data });
  const audit = useQuery({ queryKey: ['platform-audit', page], queryFn: async () => (await api.get<Audit[]>('/platform/audit-events', { params: { page } })).data });
  const create = useMutation({ mutationFn: async () => api.post('/platform/organizations', draft), onSuccess: async () => {
    setDraft(empty); setSuccess('Organização criada. O proprietário receberá o convite para definir a senha.');
    await Promise.all([client.invalidateQueries({ queryKey: ['platform-organizations'] }), client.invalidateQueries({ queryKey: ['platform-audit'] })]);
  } });
  function submit(event: FormEvent) { event.preventDefault(); setSuccess(''); create.mutate(); }
  function enter(id: string) {
    setActiveOrganization(id); sessionStorage.setItem('linepulse-platform-support', 'true');
    sessionStorage.setItem('linepulse-support-role', role); client.clear(); window.location.reload();
  }
  return <main className="app-shell platform-shell"><section className="content">
    <header className="page-header"><div><span className="eyebrow">Administração da plataforma</span><h1>Organizações LinePulse</h1><p>Crie organizações, convide proprietários e acesse workspaces para suporte.</p></div><button className="secondary-button" onClick={() => { clearActiveOrganization(); onLogout(); }}>Sair</button></header>
    {(organizations.error || create.error || audit.error) && <div className="connection-banner">{getApiErrorMessage(organizations.error || create.error || audit.error, 'Não foi possível concluir a operação.')}</div>}
    {success && <p role="status">{success}</p>}
    <section className="panel"><h2>Nova organização</h2><form onSubmit={submit} className="platform-form">
      <label>Nome da organização<input required maxLength={160} value={draft.name} onChange={e => setDraft({ ...draft, name: e.target.value })} /></label>
      <label>Identificador<input required maxLength={120} pattern="[a-z0-9]+(-[a-z0-9]+)*" placeholder="empresa-exemplo" value={draft.slug} onChange={e => setDraft({ ...draft, slug: e.target.value.toLowerCase() })} /></label>
      <label>Tipo<select value={draft.type} onChange={e => setDraft({ ...draft, type: e.target.value as OrganizationType })}><option value="COMPANY">Empresa</option><option value="SERVICE_PROVIDER">Prestador</option></select></label>
      <label>Nome do proprietário<input required maxLength={120} value={draft.ownerName} onChange={e => setDraft({ ...draft, ownerName: e.target.value })} /></label>
      <label>Cadastro do proprietário<input required maxLength={40} value={draft.ownerRegistration} onChange={e => setDraft({ ...draft, ownerRegistration: e.target.value.toUpperCase() })} /></label>
      <label>E-mail do proprietário<input type="email" required maxLength={254} value={draft.ownerEmail} onChange={e => setDraft({ ...draft, ownerEmail: e.target.value })} /></label>
      <button className="primary-button" disabled={create.isPending}>{create.isPending ? 'Criando...' : 'Criar e convidar proprietário'}</button>
    </form></section>
    <section className="panel"><h2>Workspaces</h2><label>Visão de suporte<select value={role} onChange={e => setRole(e.target.value as OrganizationRole)}><option value="OWNER">Proprietário — acesso completo</option><option value="ADMIN">Administrador</option><option value="TECHNICIAN">Técnico</option><option value="MECHANIC">Mecânico</option><option value="OPERATOR">Operador</option></select></label>
      {organizations.isPending && <p>Carregando organizações...</p>}
      {organizations.data?.length === 0 && <p>Nenhuma organização cadastrada.</p>}
      {organizations.data?.map(item => <article key={item.id} className="work-order-row"><div><strong>{item.name}</strong><small>{item.type === 'COMPANY' ? 'Empresa' : 'Prestador'} · {item.slug}{!item.active && ' · Inativa'}</small></div><button className="secondary-button" disabled={!item.active} onClick={() => enter(item.id)}>Abrir workspace</button></article>)}
    </section>
    <section className="panel"><h2>Auditoria da plataforma</h2><p>Os acessos de suporte ficam registrados aqui. A equipe da empresa não inclui sua conta de plataforma.</p>
      {audit.data?.map(event => <article key={event.id} className="work-order-row"><div><strong>{event.action}</strong><small>{event.detail} · {event.actor_registration} · {event.organization_id ?? 'Plataforma'}</small></div><time>{new Date(event.created_at).toLocaleString('pt-BR')}</time></article>)}
      <button className="text-button" disabled={page === 0} onClick={() => setPage(value => value - 1)}>Anteriores</button><button className="text-button" disabled={(audit.data?.length ?? 0) < 100} onClick={() => setPage(value => value + 1)}>Próximos</button>
    </section>
  </section></main>;
}
