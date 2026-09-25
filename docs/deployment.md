# Production deployment

LinePulse is prepared for a split deployment:

- Frontend: GitHub Pages
- Backend: Render web service using Docker
- Database: Render PostgreSQL

## 1. Backend and database on Render

Create a new Blueprint in Render using this repository. Render will read `render.yaml` from the repository root and provision:

- `linepulse-api`
- `linepulse-db`

During Blueprint creation, provide a value for `DEMO_USERS_PASSWORD` when prompted.

The backend receives database host, port, database name, user and password from the Render PostgreSQL resource. `JWT_SECRET` is generated automatically by Render.

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

In GitHub, create the repository Actions variable:

```text
VITE_API_URL
```

Set it to the public API URL including `/api`, then run the `Deploy frontend to GitHub Pages` workflow again.

## 3. Demo accounts

When `DEMO_USERS_ENABLED=true`, the backend creates these accounts if they do not exist:

```text
admin@linepulse.local
technician@linepulse.local
operator@linepulse.local
```

All three use the password configured through `DEMO_USERS_PASSWORD`.

The login page contains shortcuts that fill the e-mail for each demo role.

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
