# Production deployment

LinePulse is prepared for a split deployment:

- Frontend: GitHub Pages
- Backend: Render web service using Docker
- Database: Render PostgreSQL

## 1. Backend and database on Render

Create a new Blueprint in Render using this repository. Render reads `render.yaml` from the repository root and provisions:

- `linepulse-api`
- `linepulse-db`

The backend receives database host, port, database name, user and password from the Render PostgreSQL resource. `JWT_SECRET` is generated automatically by Render.

The production Blueprint explicitly keeps demo accounts disabled:

```text
DEMO_USERS_ENABLED=false
```

Do not enable public demo credentials in the production service.

The allowed production frontend origin is:

```text
https://samarakonkol.github.io
```

The health endpoint is:

```text
/api/health
```

## 2. Connect GitHub Pages to the API

After Render finishes deploying the backend, copy the public API URL and append `/api`.

Example format:

```text
https://<render-service>.onrender.com/api
```

In GitHub, create or update the repository Actions variable:

```text
VITE_API_URL
```

Set it to the public API URL including `/api`, then run the `Deploy frontend to GitHub Pages` workflow again.

## 3. Production users and organizations

Production users must be created through authenticated administration/onboarding flows. The V3 authorization model is organization-scoped:

- a user can belong to one or more organizations;
- the active organization is selected with the `X-LinePulse-Organization` request header;
- the effective role is resolved from the active organization membership, not only from the legacy global JWT role;
- company and service-provider data are isolated by organization.

Provider organizations should be onboarded through the service-network administration flow instead of enabling shared demo accounts.

## 4. Production environment

The backend supports these environment variables:

```text
DATABASE_URL
DATABASE_HOST
DATABASE_PORT
DATABASE_NAME
DATABASE_USER
DATABASE_PASSWORD
JWT_SECRET
JWT_EXPIRATION_MINUTES
CORS_ALLOWED_ORIGINS
DEMO_USERS_ENABLED
DEMO_USERS_PASSWORD
PORT
```

For production, keep `DEMO_USERS_ENABLED=false`. `DEMO_USERS_PASSWORD` is only relevant to local/demo environments where demo initialization is deliberately enabled.

The frontend uses:

```text
VITE_API_URL
```

## 5. Local development

The existing Docker Compose setup remains supported:

```bash
docker compose up --build
```

Local frontend:

```text
http://localhost:5173
```

Local API:

```text
http://localhost:8080/api
```

Docker Compose may enable local demo users for development. Those credentials are not part of the production deployment contract.

## 6. Production verification checklist

After every production deployment:

1. Confirm the GitHub Pages workflow completed successfully.
2. Confirm the Render service is healthy at `/api/health`.
3. Verify `VITE_API_URL` points to the current Render API URL including `/api`.
4. Confirm `DEMO_USERS_ENABLED=false` in Render.
5. Sign in with a real production account and verify the active organization/workspace.
6. Confirm a company cannot access another company's machines, incidents, work orders, audit events or service requests.
7. Confirm a service provider only sees requests explicitly routed to that provider.
8. Verify service-request creation, acceptance, execution and company approval end to end.
