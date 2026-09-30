# Administração da plataforma

A conta da plataforma tem uma permissão separada, `users.platform_admin`, concedida somente pela configuração inicial do servidor. OWNER e ADMIN de organizações não podem concedê-la. Ela não possui memberships e não aparece em listas de equipe ou na administração legada de usuários. A auditoria da empresa identifica intervenções como `PLATFORM_SUPPORT`; a auditoria exclusiva da plataforma guarda o cadastro real e os acessos por método, caminho, organização e papel simulado.

## Primeiro acesso no Render

Depois de implantar a migration V12, adicionar ao serviço da API:

| Variável | Valor |
| --- | --- |
| `PLATFORM_BOOTSTRAP_ENABLED` | `true` |
| `PLATFORM_BOOTSTRAP_EMAIL` | E-mail da administradora |
| `PLATFORM_BOOTSTRAP_PASSWORD` | Senha exclusiva de 12 a 64 caracteres (até 72 bytes UTF-8) |
| `PLATFORM_TOTP_SECRET` | Chave aleatória Base32 com pelo menos 32 caracteres |

Gerar a chave no PowerShell da própria administradora, sem compartilhar o resultado:

```powershell
$seedBytes = New-Object byte[] 32
$seedGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$seedGenerator.GetBytes($seedBytes)
$seedGenerator.Dispose()
$seedAlphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'
-join ($seedBytes | ForEach-Object { $seedAlphabet[([int]$_ -band 31)] })
```

Adicionar essa mesma chave em um aplicativo autenticador usando entrada manual: nome LinePulse, código baseado em tempo, SHA1, seis dígitos, período de 30 segundos. O algoritmo é TOTP conforme RFC 6238 (https://www.rfc-editor.org/rfc/rfc6238.html); os testes usam os vetores SHA1 oficiais. Nunca colocar a chave em screenshots, commits, URLs públicas ou chamados de suporte.

Salvar e implantar. Entrar com cadastro `PLATFORM001`, senha escolhida e o código do autenticador (clicar em “Usar código do autenticador”). Após o primeiro acesso, remover `PLATFORM_BOOTSTRAP_ENABLED`, `PLATFORM_BOOTSTRAP_EMAIL` e `PLATFORM_BOOTSTRAP_PASSWORD`. **Manter `PLATFORM_TOTP_SECRET`**, necessária nos próximos logins, e guardar uma cópia privada para recuperação. Um código aceito não pode ser reutilizado; aguardar o próximo código ao fazer outro login.

O bootstrap é transacional e serializado por advisory lock do PostgreSQL. Ele não sobrescreve senhas em reinicializações, não promove contas existentes e não adiciona memberships. O cadastro global continua OPERATOR: a permissão da plataforma é independente. JWT anteriores à promoção não são aceitos como sessão da plataforma. Alterações de senha invalidam JWT anteriores; a recuperação por e-mail não remove o segundo fator.

## Uso

O painel lista organizações, cria empresas e prestadores e envia convite ao primeiro OWNER. Se a entrega falhar, a organização, a conta pendente e o token são revertidos na mesma transação. É possível escolher visão OWNER, ADMIN, TECHNICIAN, MECHANIC ou OPERATOR antes de abrir o workspace. A simulação usa permissões reais no backend, sem entrar na conta de outra pessoa. Cada acesso de suporte exige um workspace válido e ativo; não há fallback silencioso para outra empresa. “Voltar à plataforma” limpa workspace, papel e cache.

Os convites atuais criam contas novas: cadastro e e-mail já existentes são rejeitados. A conta da plataforma não pode ser usada como dono de um cliente. Com o remetente de teste `onboarding@resend.dev`, o Resend limita o destinatário ao e-mail da própria conta Resend. Para testar inicialmente a entrega, usar “Esqueci minha senha” no e-mail da plataforma; para convidar proprietários em outros e-mails, configurar um domínio verificado no Resend.

O acesso de suporte deve constar dos termos do produto. Ocultar a conta da equipe não significa ausência de auditoria. A revisão destrutiva de segurança permanece uma etapa posterior; esta mudança exige os testes funcionais e de isolamento antes de publicação.
