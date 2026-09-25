# LinePulse

Industrial maintenance and operations platform for managing production assets, incidents, maintenance work orders, downtime and operational indicators.

## Stack

### Backend
- Java 21
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA
- Bean Validation
- JWT authentication
- PostgreSQL
- Flyway
- Maven

### Frontend
- React
- TypeScript
- Vite
- TanStack Query
- Axios
- Lucide React

### Infrastructure
- Docker
- Docker Compose
- GitHub Actions

## MVP scope

- Authentication with role-based access
- Plants, sectors and production lines
- Machine registry and operational status
- Incident management
- Maintenance work orders
- Downtime tracking
- Operational dashboard
- API error handling
- Responsive industrial UI

## Roles

- `ADMIN` — administrative access and machine registration
- `TECHNICIAN` — maintenance operations and machine status management
- `OPERATOR` — operational access and incident reporting

## Current features

- User registration and login with BCrypt password hashing and JWT
- Role-based API authorization
- Machine listing and status updates
- Incident registration from the dashboard
- Maintenance work order creation for technicians and administrators
- PostgreSQL schema managed with Flyway migrations
- Dashboard metrics backed by API data
- Availability calculation using downtime records
- Responsive React interface
- CI pipeline validating backend and frontend builds

## Project structure

```text
LinePulse/
├── backend/
│   └── src/main/java/com/linepulse/
│       ├── asset/
│       ├── auth/
│       ├── common/
│       ├── config/
│       ├── dashboard/
│       ├── downtime/
│       ├── incident/
│       └── maintenance/
├── frontend/
│   └── src/
├── docs/
├── .github/workflows/
└── docker-compose.yml
```

## Local development

Start PostgreSQL:

```bash
docker compose up -d
```

Run the backend:

```bash
cd backend
mvn spring-boot:run
```

Run the frontend:

```bash
cd frontend
npm install
npm run dev
```

The default local API URL is `http://localhost:8080/api` and the frontend runs on `http://localhost:5173`.

## Environment variables

Backend production environments should define at least:

```text
DATABASE_URL
DATABASE_USER
DATABASE_PASSWORD
JWT_SECRET
CORS_ALLOWED_ORIGINS
```

Frontend:

```text
VITE_API_URL
```

## Roadmap

- Complete maintenance lifecycle transitions
- Downtime registration from the interface
- MTTR and maintenance indicators
- Swagger / OpenAPI documentation
- Automated service and integration tests
- Audit history
- AI-assisted incident classification
- Production deployment

## Status

MVP in active development.

## Author

Samara Konkol
