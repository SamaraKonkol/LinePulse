import { expect, test } from '@playwright/test';

async function mockApi(page: Parameters<typeof test>[0]['page']) {
  await page.route('http://localhost:8080/api/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/api', '');

    if (path === '/auth/login' && request.method() === 'POST') {
      const body = request.postDataJSON() as { registration: string };
      const role = body.registration === 'ADM001' ? 'ADMIN' : body.registration === 'TEC001' ? 'TECHNICIAN' : 'OPERATOR';
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ token: 'e2e-token', user: { id: `${role}-id`, name: `${role} E2E`, registration: body.registration, role } }) });
      return;
    }

    const responses: Record<string, unknown> = {
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
    };
    if (path in responses) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(responses[path]) });
      return;
    }
    await route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ message: 'Not mocked' }) });
  });
}

async function login(page: Parameters<typeof test>[0]['page'], registration: string) {
  await page.goto('/LinePulse/');
  await page.getByLabel('Cadastro').fill(registration);
  await page.getByLabel('Senha').fill('LinePulse123!');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByRole('heading', { name: 'Visão geral' })).toBeVisible();
}

test('admin sees administrative workspace', async ({ page }) => {
  await mockApi(page);
  await login(page, 'ADM001');
  await expect(page.getByRole('link', { name: 'Administração' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Administração' })).toBeVisible();
});

test('operator cannot see administrative workspace', async ({ page }) => {
  await mockApi(page);
  await login(page, 'OPE001');
  await expect(page.getByRole('link', { name: 'Administração' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Nova ocorrência' })).toBeVisible();
});
