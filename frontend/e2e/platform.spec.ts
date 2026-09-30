import { expect, test } from '@playwright/test';

test('platform provisions owner and simulates operator without leaking authority', async ({ page }) => {
  const organization = { id: 'platform-company', name: 'Empresa Teste', slug: 'empresa-teste', type: 'COMPANY', active: true };
  let provisioned = false;
  await page.route('http://localhost:8080/api/**', async route => {
    const request = route.request(); const path = new URL(request.url()).pathname.replace('/api', '');
    if (path === '/auth/login') {
      expect(request.postDataJSON().otp).toBe('123456');
      return route.fulfill({ json: { token: 'platform-test-token', user: { id: 'founder', name: 'Plataforma', registration: 'PLATFORM001', role: 'OPERATOR', platformAdmin: true } } });
    }
    if (path === '/platform/organizations') {
      if (request.method() === 'POST') { expect(request.postDataJSON().ownerEmail).toBe('owner@example.test'); provisioned = true; return route.fulfill({ json: organization }); }
      expect(request.headers()['x-linepulse-organization']).toBeUndefined();
      return route.fulfill({ json: provisioned ? [organization] : [] });
    }
    if (path === '/platform/audit-events') return route.fulfill({ json: [] });
    expect(request.headers()['x-linepulse-organization']).toBe(organization.id);
    expect(request.headers()['x-linepulse-support-role']).toBe('OPERATOR');
    if (path === '/organizations/current') return route.fulfill({ json: { ...organization, role: 'OPERATOR' } });
    if (path === '/organizations/my') return route.fulfill({ json: [{ ...organization, role: 'OWNER' }] });
    if (path === '/dashboard') return route.fulfill({ json: { totalMachines: 0, activeMachines: 0, openIncidents: 0, activeWorkOrders: 0, availabilityPercentage: 100, mttrMinutes: 0 } });
    if (path === '/service-requests') return route.fulfill({ json: { content: [] } });
    return route.fulfill({ json: [] });
  });
  await page.goto('/LinePulse/');
  await page.getByLabel('Cadastro', { exact: true }).fill('PLATFORM001');
  await page.getByLabel('Senha', { exact: true }).fill('PlatformPassword123!');
  await page.getByRole('button', { name: 'Usar código do autenticador' }).click();
  await page.getByLabel('Código do autenticador', { exact: true }).fill('123456');
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Organizações LinePulse' })).toBeVisible();
  expect((await page.locator('main > section.content').boundingBox())?.width).toBeGreaterThan(800);
  await page.getByLabel('Nome da organização').fill('Empresa Teste');
  await page.getByLabel('Identificador').fill('empresa-teste');
  await page.getByLabel('Nome do proprietário').fill('Owner');
  await page.getByLabel('Cadastro do proprietário').fill('OWNER001');
  await page.getByLabel('E-mail do proprietário').fill('owner@example.test');
  await page.getByRole('button', { name: 'Criar e convidar proprietário' }).click();
  await expect(page.getByRole('status')).toContainText('Organização criada');
  await page.getByLabel('Visão de suporte').selectOption('OPERATOR');
  await page.getByRole('button', { name: 'Abrir workspace' }).click();
  await expect(page.getByRole('heading', { name: 'Visão geral' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Administração', exact: true })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Nova ordem', exact: true })).toHaveCount(0);
  await expect(page.getByRole('status')).toContainText('Suporte da plataforma');
  await page.getByRole('button', { name: 'Voltar à plataforma' }).click();
  await expect(page.getByRole('heading', { name: 'Organizações LinePulse' })).toBeVisible();
  expect(await page.evaluate(() => sessionStorage.getItem('linepulse-support-role'))).toBeNull();
});

test('tenant owner does not see platform administration', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('linepulse-auth-v2', JSON.stringify({ token: 'owner-token', user: { id: 'owner', name: 'Owner', role: 'ADMIN', platformAdmin: false } })));
  await page.route('http://localhost:8080/api/**', route => {
    const path = new URL(route.request().url()).pathname;
    expect(path).not.toContain('/platform/');
    if (path.endsWith('/organizations/current')) return route.fulfill({ json: { id: 'company', name: 'Company', type: 'COMPANY', role: 'OWNER' } });
    if (path.endsWith('/dashboard')) return route.fulfill({ json: { totalMachines: 0, activeMachines: 0, openIncidents: 0, activeWorkOrders: 0, availabilityPercentage: 100, mttrMinutes: 0 } });
    if (path.endsWith('/service-requests')) return route.fulfill({ json: { content: [] } });
    return route.fulfill({ json: [] });
  });
  await page.goto('/LinePulse/');
  await expect(page.getByRole('heading', { name: 'Visão geral' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Organizações LinePulse' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Voltar à plataforma' })).toHaveCount(0);
});
