import { expect, test, type Page } from '@playwright/test';

const apiBase = 'http://localhost:8080/api';
const authStorageKey = 'linepulse-auth-v2';
const organizationStorageKey = 'linepulse-active-organization-v3';

const company = {
  id: '11111111-1111-1111-1111-111111111111',
  name: 'Fábrica E2E',
  slug: 'fabrica-e2e',
  type: 'COMPANY',
  role: 'OWNER',
};

const provider = {
  id: '22222222-2222-2222-2222-222222222222',
  name: 'Manutenção E2E',
  slug: 'manutencao-e2e',
  type: 'SERVICE_PROVIDER',
  role: 'OWNER',
};

const emptyPage = {
  content: [],
  page: 0,
  size: 100,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
};

const companyMembers = [
  { membershipId: 1, userId: 'owner-1', name: 'Owner E2E', registration: 'OWN001', role: 'OWNER', active: true },
  { membershipId: 2, userId: 'tech-1', name: 'Técnico E2E', registration: 'TEC001', role: 'TECHNICIAN', active: true },
];

async function installSession(page: Page, user: { id: string; name: string; registration: string; role: 'ADMIN' | 'TECHNICIAN' | 'OPERATOR' }, organizationId?: string) {
  await page.addInitScript(({ authKey, orgKey, userData, activeOrganization }) => {
    localStorage.setItem(authKey, JSON.stringify({ token: 'e2e-token', user: userData }));
    if (activeOrganization) localStorage.setItem(orgKey, activeOrganization);
  }, { authKey: authStorageKey, orgKey: organizationStorageKey, userData: user, activeOrganization: organizationId });
}

async function mockCommonApi(
  page: Page,
  currentOrganization: typeof company | typeof provider,
  requests = emptyPage,
  seenOrganizationHeaders: string[] = [],
  organizationMembers: unknown[] = [],
  memberMutationPaths: string[] = [],
) {
  await page.route(`${apiBase}/**`, async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/api', '');
    const organizationHeader = request.headers()['x-linepulse-organization'];
    if (organizationHeader) seenOrganizationHeaders.push(organizationHeader);

    if (request.method() === 'PATCH' && path.startsWith('/organization-members/')) {
      memberMutationPaths.push(path);
      const userId = path.split('/')[2];
      const member = organizationMembers.find((candidate) => typeof candidate === 'object' && candidate !== null && 'userId' in candidate && candidate.userId === userId) ?? {};
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(member) });
      return;
    }

    const commonResponses: Record<string, unknown> = {
      '/dashboard': { totalMachines: 0, activeMachines: 0, openIncidents: 0, activeWorkOrders: 0, availabilityPercentage: 100, mttrMinutes: 0 },
      '/dashboard/incident-trend': [],
      '/alerts': [],
      '/incidents': [],
      '/machines': [],
      '/work-orders': [],
      '/maintenance-plans': [],
      '/downtimes': [],
      '/audit-events': [],
      '/admin/users': [],
      '/admin/production-lines': [],
      '/admin/structure/plants': [],
      '/admin/structure/sectors': [],
      '/admin/structure/lines': [],
      '/organizations/my': [currentOrganization],
      '/organizations/current': currentOrganization,
      '/service-requests': requests,
      '/provider-network/providers': [],
      '/provider-network/relationships': [],
      '/organization-members': organizationMembers,
    };

    if (path in commonResponses) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(commonResponses[path]) });
      return;
    }

    await route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ message: `Not mocked: ${path}` }) });
  });
}

test('company owner sees the V3 service-request workspace', async ({ page }) => {
  await installSession(page, { id: 'admin-1', name: 'Admin E2E', registration: 'ADM001', role: 'ADMIN' }, company.id);
  await mockCommonApi(page, company);

  await page.goto('/LinePulse/');

  await expect(page.getByRole('heading', { name: 'Rede de serviços' })).toBeVisible();
  await expect(page.getByText('Novo chamado', { exact: true })).toBeVisible();
  await expect(page.getByText('Chamados da empresa', { exact: true })).toBeVisible();
  await expect(page.locator('.workspace-selector select')).toHaveValue(company.id);
});

test('provider sees routed calls and every authenticated request carries the active organization header', async ({ page }) => {
  const seenOrganizationHeaders: string[] = [];
  const requestedAt = new Date().toISOString();
  const providerQueue = {
    ...emptyPage,
    content: [{
      id: '33333333-3333-3333-3333-333333333333',
      companyOrganizationId: company.id,
      companyName: company.name,
      providerOrganizationId: provider.id,
      providerName: provider.name,
      machineId: '44444444-4444-4444-4444-444444444444',
      machineName: 'Prensa 04',
      machineAssetCode: 'PR-04',
      incidentId: null,
      incidentTitle: null,
      workOrderId: null,
      assignedTechnicianId: null,
      assignedTechnicianName: null,
      title: 'Falha hidráulica',
      description: 'Perda de pressão no circuito principal.',
      channel: 'EXTERNAL',
      priority: 'CRITICAL',
      status: 'REQUESTED',
      eta: null,
      serviceNotes: null,
      partsUsed: null,
      declineReason: null,
      requestedAt,
      acceptedAt: null,
      enRouteAt: null,
      startedAt: null,
      completedAt: null,
      approvedAt: null,
      declinedAt: null,
      cancelledAt: null,
    }],
    totalElements: 1,
    totalPages: 1,
    last: true,
  };

  await installSession(page, { id: 'tech-1', name: 'Responsável E2E', registration: 'TEC001', role: 'TECHNICIAN' }, provider.id);
  await mockCommonApi(page, provider, providerQueue, seenOrganizationHeaders);

  await page.goto('/LinePulse/');

  await expect(page.getByText('Fila de atendimento', { exact: true })).toBeVisible();
  await expect(page.getByText('PR-04 · Falha hidráulica', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Aceitar' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Recusar' })).toBeVisible();

  await expect.poll(() => seenOrganizationHeaders.length).toBeGreaterThan(0);
  expect(new Set(seenOrganizationHeaders)).toEqual(new Set([provider.id]));
});

test('company owner can manage team roles and membership status from the V3 workspace', async ({ page }) => {
  const memberMutationPaths: string[] = [];
  await installSession(page, { id: 'owner-1', name: 'Owner E2E', registration: 'OWN001', role: 'ADMIN' }, company.id);
  await mockCommonApi(page, company, emptyPage, [], companyMembers, memberMutationPaths);

  await page.goto('/LinePulse/');

  await expect(page.getByText('Membros atuais', { exact: true })).toBeVisible();
  await expect(page.getByText('Owner E2E', { exact: true })).toBeVisible();
  await expect(page.getByText('Técnico E2E', { exact: true })).toBeVisible();
  await expect(page.getByLabel('Papel de Owner E2E')).toHaveValue('OWNER');
  await expect(page.getByLabel('Papel de Técnico E2E')).toHaveValue('TECHNICIAN');

  await page.getByLabel('Papel de Técnico E2E').selectOption('ADMIN');
  await expect.poll(() => memberMutationPaths.includes('/organization-members/tech-1/role')).toBeTruthy();

  const technicianRow = page.locator('.member-management-row').filter({ hasText: 'Técnico E2E' });
  await technicianRow.getByRole('button', { name: 'Desativar' }).click();
  await expect.poll(() => memberMutationPaths.includes('/organization-members/tech-1/status')).toBeTruthy();
});
