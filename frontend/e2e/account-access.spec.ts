import { test, expect } from '@playwright/test';

test('recovery gives a generic response and submits only the email', async ({ page }) => {
  let payload: unknown;
  await page.route('**/api/auth/password-reset', async route => {
    payload = route.request().postDataJSON();
    await route.fulfill({ json: { message: 'Se houver uma conta ativa com esse e-mail, enviaremos as instruções.' } });
  });
  await page.goto('/LinePulse/');
  await page.getByRole('button', { name: 'Esqueci minha senha' }).click();
  await page.getByLabel('E-mail', { exact: true }).fill('samara@example.test');
  await page.getByRole('button', { name: 'Enviar instruções' }).click();
  await expect(page.getByRole('status')).toContainText('Se houver');
  expect(payload).toEqual({ email: 'samara@example.test' });
});

test('invite link checks confirmation, sets password and removes token from URL', async ({ page }) => {
  const token = 'a'.repeat(43);
  let calls = 0;
  await page.route('**/api/auth/complete-account', async route => {
    calls++;
    expect(route.request().postDataJSON()).toEqual({ token, password: 'MyNewPassword123!' });
    await route.fulfill({ json: { message: 'Senha definida. Entre com seu cadastro.' } });
  });
  await page.goto(`/LinePulse/#account-token=${token}`);
  await page.getByLabel('Nova senha', { exact: true }).fill('MyNewPassword123!');
  await page.getByLabel('Confirmar senha').fill('DifferentPassword123!');
  await page.getByRole('button', { name: 'Salvar senha' }).click();
  await expect(page.getByRole('alert')).toContainText('iguais');
  expect(calls).toBe(0);
  await page.getByLabel('Confirmar senha').fill('MyNewPassword123!');
  await page.getByRole('button', { name: 'Salvar senha' }).click();
  await expect(page.getByRole('status')).toContainText('Senha definida');
  await expect(page).not.toHaveURL(/account-token/);
  expect(calls).toBe(1);
});
