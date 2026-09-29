import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Building2, Clock3, Network, UserCog, Wrench } from 'lucide-react';
import { FormEvent, useMemo, useState } from 'react';
import type { Incident, Machine } from './types/api';
import type { OrganizationRole, ProviderOnboardingInput, ServiceRequestChannel, ServiceRequestPriority } from './types/serviceNetwork';
import { acceptServiceRequest, approveServiceRequest, assignServiceRequest, cancelServiceRequest, completeServiceRequest, createServiceRequest, declineServiceRequest, getCurrentOrganization, getMyOrganizations, getOrganizationMembers, getProviderRelationships, getProviders, getServiceRequests, markServiceRequestEnRoute, onboardProvider, setActiveOrganization, startServiceRequest, suspendProviderRelationship, trustProvider, updateOrganizationMemberRole, updateOrganizationMemberStatus, updateServiceRequestEta } from './services/serviceNetworkApi';
import api from './services/api';
import { getApiErrorMessage } from './utils/apiError';
import './service-network.css';

type Props = { machines: Machine[]; incidents: Incident[] };
type Action = () => Promise<unknown>;

const statusLabel = {
  REQUESTED: 'Solicitado', ACCEPTED: 'Aceito', EN_ROUTE: 'A caminho', IN_PROGRESS: 'Em atendimento', COMPLETED: 'Concluído', APPROVED: 'Aprovado', DECLINED: 'Recusado', CANCELLED: 'Cancelado',
};
const priorityLabel = { LOW: 'Baixa', MEDIUM: 'Média', HIGH: 'Alta', CRITICAL: 'Crítica' };
const roleLabel = { OWNER: 'Proprietário', ADMIN: 'Administrador', TECHNICIAN: 'Técnico', OPERATOR: 'Operador', MECHANIC: 'Mecânico' };
const technicalRoles: OrganizationRole[] = ['OWNER', 'ADMIN', 'TECHNICIAN', 'MECHANIC'];

function ServiceNetworkPanel({ machines, incidents }: Props) {
  const queryClient = useQueryClient();
  const [selectedTechnician, setSelectedTechnician] = useState<Record<string, string>>({});
  const [requestDraft, setRequestDraft] = useState({ machineId: '', incidentId: '', title: '', description: '', channel: 'INTERNAL' as ServiceRequestChannel, priority: 'MEDIUM' as ServiceRequestPriority, providerOrganizationId: '' });
  const [memberDraft, setMemberDraft] = useState({ name: '', registration: '', email: '', role: 'TECHNICIAN' as OrganizationRole });
  const [providerDraft, setProviderDraft] = useState<ProviderOnboardingInput>({ name: '', slug: '', ownerName: '', ownerRegistration: '', ownerPassword: '' });
  const [providerToTrust, setProviderToTrust] = useState('');

  const organizationsQuery = useQuery({ queryKey: ['organizations'], queryFn: getMyOrganizations });
  const currentOrganizationQuery = useQuery({ queryKey: ['organization-current'], queryFn: getCurrentOrganization });
  const requestsQuery = useQuery({ queryKey: ['service-requests'], queryFn: () => getServiceRequests() });
  const current = currentOrganizationQuery.data;
  const isCompany = current?.type === 'COMPANY';
  const canAdminOrganization = current?.role === 'OWNER' || current?.role === 'ADMIN';
  const canExecute = current ? technicalRoles.includes(current.role) : false;

  const providersQuery = useQuery({ queryKey: ['service-providers'], queryFn: getProviders, enabled: isCompany });
  const relationshipsQuery = useQuery({ queryKey: ['service-relationships'], queryFn: getProviderRelationships, enabled: isCompany });
  const membersQuery = useQuery({ queryKey: ['organization-members', current?.id], queryFn: getOrganizationMembers, enabled: Boolean(current && current.role !== 'OPERATOR') });

  const activeRelationships = (relationshipsQuery.data ?? []).filter((item) => item.status === 'ACTIVE');
  const technicalMembers = (membersQuery.data ?? []).filter((member) => member.active && technicalRoles.includes(member.role));
  const linkedProviderIds = new Set((relationshipsQuery.data ?? []).map((item) => item.providerOrganizationId));
  const unlinkedProviders = (providersQuery.data ?? []).filter((provider) => !linkedProviderIds.has(provider.id));
  const selectedMachineIncidents = useMemo(() => incidents.filter((incident) => incident.machineId === requestDraft.machineId), [incidents, requestDraft.machineId]);
  const memberRoleOptions = useMemo<OrganizationRole[]>(() => {
    const base: OrganizationRole[] = current?.type === 'SERVICE_PROVIDER'
      ? ['ADMIN', 'TECHNICIAN', 'MECHANIC']
      : ['ADMIN', 'TECHNICIAN', 'OPERATOR'];
    return current?.role === 'OWNER' ? ['OWNER', ...base] : base;
  }, [current?.role, current?.type]);

  const refreshNetwork = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['service-requests'] }),
      queryClient.invalidateQueries({ queryKey: ['service-relationships'] }),
      queryClient.invalidateQueries({ queryKey: ['service-providers'] }),
      queryClient.invalidateQueries({ queryKey: ['organization-members'] }),
      queryClient.invalidateQueries({ queryKey: ['work-orders'] }),
      queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
      queryClient.invalidateQueries({ queryKey: ['alerts'] }),
    ]);
  };

  const actionMutation = useMutation({ mutationFn: (action: Action) => action(), onSuccess: refreshNetwork });
  const createRequestMutation = useMutation({
    mutationFn: createServiceRequest,
    onSuccess: async () => {
      setRequestDraft({ machineId: '', incidentId: '', title: '', description: '', channel: 'INTERNAL', priority: 'MEDIUM', providerOrganizationId: '' });
      await refreshNetwork();
    },
  });
  const memberMutation = useMutation({
    mutationFn: (input: typeof memberDraft) => api.post('/organization-members/invitations', input),
    onSuccess: async () => {
      setMemberDraft({ name: '', registration: '', email: '', role: current?.type === 'SERVICE_PROVIDER' ? 'MECHANIC' : 'TECHNICIAN' });
      await refreshNetwork();
    },
  });
  const providerMutation = useMutation({
    mutationFn: onboardProvider,
    onSuccess: async () => {
      setProviderDraft({ name: '', slug: '', ownerName: '', ownerRegistration: '', ownerPassword: '' });
      await refreshNetwork();
    },
  });

  const error = [organizationsQuery.error, currentOrganizationQuery.error, requestsQuery.error, providersQuery.error, relationshipsQuery.error, membersQuery.error, actionMutation.error, createRequestMutation.error, memberMutation.error, providerMutation.error].find(Boolean);

  function submitRequest(event: FormEvent) {
    event.preventDefault();
    if (!requestDraft.machineId || !requestDraft.title.trim() || !requestDraft.description.trim()) return;
    createRequestMutation.mutate({
      machineId: requestDraft.machineId,
      incidentId: requestDraft.incidentId || undefined,
      title: requestDraft.title.trim(),
      description: requestDraft.description.trim(),
      channel: requestDraft.channel,
      priority: requestDraft.priority,
      providerOrganizationId: requestDraft.channel === 'EXTERNAL' ? requestDraft.providerOrganizationId || undefined : undefined,
    });
  }

  function promptEta(id: string) {
    const value = window.prompt('Informe a previsão de chegada (ex.: 2026-09-27T18:30)');
    if (!value) return;
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) { window.alert('Data inválida.'); return; }
    actionMutation.mutate(() => updateServiceRequestEta(id, parsed.toISOString()));
  }

  function promptDecline(id: string) {
    const reason = window.prompt('Motivo da recusa:');
    if (reason?.trim()) actionMutation.mutate(() => declineServiceRequest(id, reason.trim()));
  }

  function promptComplete(id: string) {
    const notes = window.prompt('Descreva o serviço realizado:');
    if (!notes?.trim()) return;
    const parts = window.prompt('Peças utilizadas (opcional):') ?? undefined;
    actionMutation.mutate(() => completeServiceRequest(id, notes.trim(), parts?.trim() || undefined));
  }

  if (!current && currentOrganizationQuery.isLoading) return <section className="panel service-network-panel"><div className="empty-state">Carregando rede de serviços...</div></section>;

  return (
    <section className="panel service-network-panel" id="service-network">
      <div className="panel-heading service-network-heading">
        <div><span className="eyebrow">LinePulse V3</span><h2>Rede de serviços</h2><p>{isCompany ? 'Acione manutenção interna ou prestadores externos e acompanhe o atendimento.' : 'Receba chamados de clientes, distribua aos mecânicos e registre a execução.'}</p></div>
        <Network size={22} />
      </div>

      {error && <div className="connection-banner">{getApiErrorMessage(error, 'Não foi possível concluir uma operação da rede de serviços.')}</div>}

      <div className="workspace-selector">
        <Building2 size={18} />
        <label><span>Workspace ativo</span><select value={current?.id ?? ''} onChange={(event) => { setActiveOrganization(event.target.value); window.location.reload(); }} disabled={(organizationsQuery.data?.length ?? 0) <= 1}>{(organizationsQuery.data ?? []).map((organization) => <option value={organization.id} key={organization.id}>{organization.name} · {organization.type === 'COMPANY' ? 'Empresa' : 'Prestador'}</option>)}</select></label>
        {current && <small>{roleLabel[current.role]}</small>}
      </div>

      {isCompany && current?.role !== 'OPERATOR' && (
        <form className="service-form" onSubmit={submitRequest}>
          <div className="service-section-title"><Wrench size={18} /><div><strong>Novo chamado</strong><small>O aceite gera automaticamente uma ordem de manutenção vinculada.</small></div></div>
          <div className="service-form-grid">
            <label>Máquina<select value={requestDraft.machineId} onChange={(event) => setRequestDraft((draft) => ({ ...draft, machineId: event.target.value, incidentId: '' }))} required><option value="">Selecione</option>{machines.filter((machine) => machine.status !== 'INACTIVE').map((machine) => <option key={machine.id} value={machine.id}>{machine.assetCode} · {machine.name}</option>)}</select></label>
            <label>Ocorrência<select value={requestDraft.incidentId} onChange={(event) => setRequestDraft((draft) => ({ ...draft, incidentId: event.target.value }))}><option value="">Sem vínculo</option>{selectedMachineIncidents.map((incident) => <option key={incident.id} value={incident.id}>{incident.title}</option>)}</select></label>
            <label>Atendimento<select value={requestDraft.channel} onChange={(event) => setRequestDraft((draft) => ({ ...draft, channel: event.target.value as ServiceRequestChannel, providerOrganizationId: '' }))}><option value="INTERNAL">Equipe interna</option><option value="EXTERNAL">Prestador externo</option></select></label>
            <label>Prioridade<select value={requestDraft.priority} onChange={(event) => setRequestDraft((draft) => ({ ...draft, priority: event.target.value as ServiceRequestPriority }))}><option value="LOW">Baixa</option><option value="MEDIUM">Média</option><option value="HIGH">Alta</option><option value="CRITICAL">Crítica</option></select></label>
            {requestDraft.channel === 'EXTERNAL' && <label>Prestador<select value={requestDraft.providerOrganizationId} onChange={(event) => setRequestDraft((draft) => ({ ...draft, providerOrganizationId: event.target.value }))} required><option value="">Selecione</option>{activeRelationships.map((relationship) => <option key={relationship.id} value={relationship.providerOrganizationId}>{relationship.providerName}</option>)}</select></label>}
            <label className="wide-field">Título<input value={requestDraft.title} onChange={(event) => setRequestDraft((draft) => ({ ...draft, title: event.target.value }))} maxLength={180} required /></label>
            <label className="wide-field">Descrição<textarea value={requestDraft.description} onChange={(event) => setRequestDraft((draft) => ({ ...draft, description: event.target.value }))} rows={3} required /></label>
          </div>
          <button className="primary-button" type="submit" disabled={createRequestMutation.isPending || (requestDraft.channel === 'EXTERNAL' && !requestDraft.providerOrganizationId)}>Enviar chamado</button>
        </form>
      )}

      <div className="service-section-title"><Clock3 size={18} /><div><strong>{isCompany ? 'Chamados da empresa' : 'Fila de atendimento'}</strong><small>{requestsQuery.data?.totalElements ?? 0} registros visíveis neste workspace</small></div></div>
      <div className="service-request-list">
        {(requestsQuery.data?.content ?? []).length === 0 && !requestsQuery.isLoading && <div className="empty-state">Nenhum chamado neste workspace.</div>}
        {(requestsQuery.data?.content ?? []).map((request) => {
          const executor = (isCompany && request.channel === 'INTERNAL') || !isCompany;
          const terminal = ['APPROVED', 'DECLINED', 'CANCELLED'].includes(request.status);
          return <article className="service-request-card" key={request.id}>
            <div className="service-request-main"><div className="service-request-title"><strong>{request.machineAssetCode} · {request.title}</strong><span className={`service-status status-${request.status.toLowerCase()}`}>{statusLabel[request.status]}</span></div><small>{request.companyName}{request.providerName ? ` → ${request.providerName}` : ' · equipe interna'} · {priorityLabel[request.priority]}</small><p>{request.description}</p><div className="service-request-meta"><span>OS: {request.workOrderId ? request.workOrderId.slice(0, 8) : 'aguardando aceite'}</span><span>Técnico: {request.assignedTechnicianName ?? 'não atribuído'}</span><span>ETA: {request.eta ? new Date(request.eta).toLocaleString('pt-BR') : 'não informada'}</span></div>{request.serviceNotes && <div className="service-result"><b>Serviço:</b> {request.serviceNotes}{request.partsUsed && <><br /><b>Peças:</b> {request.partsUsed}</>}</div>}{request.declineReason && <div className="service-result"><b>Recusa:</b> {request.declineReason}</div>}</div>
            <div className="service-request-actions">
              {executor && canExecute && request.status === 'REQUESTED' && <><button onClick={() => actionMutation.mutate(() => acceptServiceRequest(request.id))}>Aceitar</button><button onClick={() => promptDecline(request.id)}>Recusar</button></>}
              {executor && canExecute && !terminal && request.status !== 'COMPLETED' && technicalMembers.length > 0 && <><select value={selectedTechnician[request.id] ?? request.assignedTechnicianId ?? ''} onChange={(event) => setSelectedTechnician((currentMap) => ({ ...currentMap, [request.id]: event.target.value }))}><option value="">Técnico</option>{technicalMembers.map((member) => <option value={member.userId} key={member.userId}>{member.name} · {roleLabel[member.role]}</option>)}</select><button disabled={!selectedTechnician[request.id]} onClick={() => selectedTechnician[request.id] && actionMutation.mutate(() => assignServiceRequest(request.id, selectedTechnician[request.id]))}>Atribuir</button></>}
              {executor && canExecute && ['ACCEPTED', 'EN_ROUTE'].includes(request.status) && <button onClick={() => promptEta(request.id)}>Atualizar ETA</button>}
              {executor && canExecute && request.channel === 'EXTERNAL' && request.status === 'ACCEPTED' && <button onClick={() => actionMutation.mutate(() => markServiceRequestEnRoute(request.id))}>A caminho</button>}
              {executor && canExecute && ['ACCEPTED', 'EN_ROUTE'].includes(request.status) && <button onClick={() => actionMutation.mutate(() => startServiceRequest(request.id))}>Iniciar</button>}
              {executor && canExecute && request.status === 'IN_PROGRESS' && <button className="complete" onClick={() => promptComplete(request.id)}>Concluir</button>}
              {isCompany && canAdminOrganization && request.status === 'COMPLETED' && <button className="complete" onClick={() => actionMutation.mutate(() => approveServiceRequest(request.id))}>Aprovar</button>}
              {isCompany && current?.role !== 'OPERATOR' && ['REQUESTED', 'ACCEPTED'].includes(request.status) && <button onClick={() => actionMutation.mutate(() => cancelServiceRequest(request.id))}>Cancelar</button>}
            </div>
          </article>;
        })}
      </div>

      {canAdminOrganization && (
        <div className="service-admin-grid">
          <form className="service-form compact-service-form" onSubmit={(event) => { event.preventDefault(); memberMutation.mutate(memberDraft); }}>
            <div className="service-section-title"><UserCog size={18} /><div><strong>Equipe do workspace</strong><small>Cadastre e gerencie os papéis desta organização.</small></div></div>
            <label>Nome<input value={memberDraft.name} onChange={(event) => setMemberDraft((draft) => ({ ...draft, name: event.target.value }))} required /></label>
            <label>Cadastro<input value={memberDraft.registration} onChange={(event) => setMemberDraft((draft) => ({ ...draft, registration: event.target.value }))} required /></label>
            <label>E-mail do convite<input type="email" maxLength={254} value={memberDraft.email} onChange={(event) => setMemberDraft((draft) => ({ ...draft, email: event.target.value }))} required /></label>
            <label>Papel<select value={memberDraft.role} onChange={(event) => setMemberDraft((draft) => ({ ...draft, role: event.target.value as OrganizationRole }))}>{memberRoleOptions.map((role) => <option value={role} key={role}>{roleLabel[role]}</option>)}</select></label>
            <button className="secondary-button" type="submit" disabled={memberMutation.isPending}>Enviar convite</button>
            {memberMutation.isSuccess && <p role="status">Convite enviado. O membro define a própria senha pelo e-mail.</p>}

            <div className="member-management-list">
              <div className="member-management-heading"><strong>Membros atuais</strong><small>{membersQuery.data?.length ?? 0} cadastrados</small></div>
              {(membersQuery.data ?? []).length === 0 && !membersQuery.isLoading && <div className="empty-state compact-empty-state">Nenhum membro cadastrado.</div>}
              {(membersQuery.data ?? []).map((member) => {
                const ownerProtectedFromAdmin = current?.role !== 'OWNER' && member.role === 'OWNER';
                const allowedRoles = current?.role === 'OWNER'
                  ? (current.type === 'SERVICE_PROVIDER' ? ['OWNER', 'ADMIN', 'TECHNICIAN', 'MECHANIC'] : ['OWNER', 'ADMIN', 'TECHNICIAN', 'OPERATOR']) as OrganizationRole[]
                  : memberRoleOptions;
                return <div className={`member-management-row${member.active ? '' : ' inactive-member'}`} key={member.membershipId}>
                  <div className="member-identity"><strong>{member.name}</strong><small>{member.registration} · {member.active ? 'Ativo' : 'Inativo'}</small></div>
                  <select aria-label={`Papel de ${member.name}`} value={member.role} disabled={ownerProtectedFromAdmin || actionMutation.isPending} onChange={(event) => actionMutation.mutate(() => updateOrganizationMemberRole(member.userId, event.target.value as OrganizationRole))}>
                    {allowedRoles.map((role) => <option value={role} key={role}>{roleLabel[role]}</option>)}
                    {!allowedRoles.includes(member.role) && <option value={member.role}>{roleLabel[member.role]}</option>}
                  </select>
                  <button type="button" className="text-button" disabled={ownerProtectedFromAdmin || actionMutation.isPending} onClick={() => actionMutation.mutate(() => updateOrganizationMemberStatus(member.userId, !member.active))}>{member.active ? 'Desativar' : 'Reativar'}</button>
                </div>;
              })}
              {current?.role === 'OWNER' && <small className="ownership-hint">A organização sempre precisa manter pelo menos um Proprietário ativo.</small>}
            </div>
          </form>

          {isCompany && <div className="service-form compact-service-form">
            <div className="service-section-title"><Building2 size={18} /><div><strong>Rede de prestadores</strong><small>Vincule uma organização existente ou cadastre um novo prestador.</small></div></div>
            {unlinkedProviders.length > 0 && <div className="inline-provider-link"><select value={providerToTrust} onChange={(event) => setProviderToTrust(event.target.value)}><option value="">Prestador existente</option>{unlinkedProviders.map((provider) => <option key={provider.id} value={provider.id}>{provider.name}</option>)}</select><button className="secondary-button" disabled={!providerToTrust || actionMutation.isPending} onClick={() => actionMutation.mutate(() => trustProvider(providerToTrust))}>Vincular</button></div>}
            <div className="relationship-list">{(relationshipsQuery.data ?? []).map((relationship) => <div key={relationship.id}><span><strong>{relationship.providerName}</strong><small>{relationship.status === 'ACTIVE' ? 'Ativo' : 'Suspenso'}</small></span>{relationship.status === 'ACTIVE' && <button className="text-button" onClick={() => actionMutation.mutate(() => suspendProviderRelationship(relationship.id))}>Suspender</button>}</div>)}</div>
            <form className="provider-onboarding-form" onSubmit={(event) => { event.preventDefault(); providerMutation.mutate(providerDraft); }}>
              <strong>Cadastrar novo prestador</strong>
              <input placeholder="Nome da oficina/prestador" value={providerDraft.name} onChange={(event) => setProviderDraft((draft) => ({ ...draft, name: event.target.value, slug: event.target.value.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') }))} required />
              <input placeholder="Identificador (slug)" value={providerDraft.slug} onChange={(event) => setProviderDraft((draft) => ({ ...draft, slug: event.target.value }))} required />
              <input placeholder="Responsável" value={providerDraft.ownerName} onChange={(event) => setProviderDraft((draft) => ({ ...draft, ownerName: event.target.value }))} required />
              <input placeholder="Cadastro do responsável" value={providerDraft.ownerRegistration} onChange={(event) => setProviderDraft((draft) => ({ ...draft, ownerRegistration: event.target.value }))} required />
              <input type="password" minLength={8} placeholder="Senha inicial" value={providerDraft.ownerPassword} onChange={(event) => setProviderDraft((draft) => ({ ...draft, ownerPassword: event.target.value }))} required />
              <button className="secondary-button" type="submit" disabled={providerMutation.isPending}>Criar prestador e vincular</button>
            </form>
          </div>}
        </div>
      )}
    </section>
  );
}

export default ServiceNetworkPanel;
