# V3 release validation

## Implemented in this release candidate

- Dashboard privileges and role labels come from the active organization, resolved before any operational panels mount. Resolution errors keep the dashboard closed.
- OWNER/ADMIN manage company assets; company TECHNICIAN/MECHANIC manage operations; OPERATOR cannot see privileged controls. Provider ownership grants team/network management, without granting company asset administration.
- Workspace changes reload the application, removing cached data, draft forms and selected machine state. Every scoped request uses the organization header.
- Service network mounts directly in the dashboard.
- Team invitations use email; new accounts and memberships remain inactive until acceptance. Invitations expire after 48 hours.
- Password recovery expires after 30 minutes; request responses do not disclose whether an account exists. Existing legacy accounts without an email cannot use email recovery yet.
- Tokens contain 256 bits of randomness and only SHA-256 digests are stored. Password updates serialize on the account row and invalidate all outstanding account links and previous JWT sessions.
- Invitation acceptance requires the issuing administrator still to have an active, authorized membership in an active organization.

## Email deployment configuration

For the existing Render Free deployment, use the HTTPS Resend transport:

```
RESEND_API_KEY
MAIL_FROM
FRONTEND_URL=https://samarakonkol.github.io/LinePulse/
```

The sender/domain must be authorized by the email provider. Resend is used when its API key is configured; otherwise SMTP is available. No live email was sent during verification. Render Free blocks outbound SMTP on ports 25, 465 and 587 ([Render documentation](https://render.com/docs/free)); the HTTPS integration avoids those ports ([Resend email API](https://resend.com/docs/api-reference/emails/send-email)).

For SMTP-compatible infrastructure, configure:

```
SPRING_MAIL_HOST
SPRING_MAIL_PORT
SPRING_MAIL_USERNAME
SPRING_MAIL_PASSWORD
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_CONNECTIONTIMEOUT=10000
SPRING_MAIL_PROPERTIES_MAIL_SMTP_TIMEOUT=10000
SPRING_MAIL_PROPERTIES_MAIL_SMTP_WRITETIMEOUT=10000
MAIL_FROM
FRONTEND_URL=https://samarakonkol.github.io/LinePulse/
```

Use the SMTP provider's port/TLS settings. Credentials belong in deployment secrets. No real email is sent by the automated tests. Local demo/legacy administrative creation remains available for compatibility; the normal team UI sends invitations.

Flyway V11 adds optional email, credential-version timestamp and single-use account-token storage. Existing user accounts remain compatible until a password is changed; thereafter their previous JWTs are rejected.

## Verification requirements

1. Frontend production build and Playwright role, workspace switching, service network, invitation and recovery tests.
2. Backend unit tests plus PostgreSQL integration tests, including migration and account-token replay/expiration.
3. CI backend/frontend/browser/Docker smoke checks.
4. Live frontend/backend health, followed by an authenticated company/provider round trip against the deployed release.
5. Email invitation acceptance and password recovery using dedicated test accounts.

A passing build or public health endpoint alone does not satisfy requirements 4 and 5. Production acceptance remains pending until the deployed version, authenticated workflow and real email delivery are verified. Destructive cybersecurity review is a separate next phase.

## Live checks on 2026-09-29

- GitHub Pages frontend: HTTP 200.
- Backend configured in the published frontend: `https://linepulse-api-8ndc.onrender.com/api`.
- Backend health: HTTP 200 after cold-start delay.
- CORS preflight from `https://samarakonkol.github.io`: HTTP 200, permits `authorization` and `x-linepulse-organization`.
- Authenticated production workflows and real email delivery: pending test account access and email-provider environment configuration. These public checks were against the existing deployed main, not the PR release candidate.
