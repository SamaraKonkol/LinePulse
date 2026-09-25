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
- OpenAPI / Swagger UI
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
- Nginx

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
- Machine listing and operational status updates
- Incident registration from the dashboard
- Maintenance work order creation and lifecycle transitions
- Downtime registration and closing from the dashboard
- Availability calculation using real downtime intervals
- MTTR calculation using completed maintenance orders from the last 30 days
- PostgreSQL schema managed with Flyway migrations
- OpenAPI documentation with Bearer JWT authentication
- Unit tests for maintenance lifecycle and dashboard indicators
- Responsive React interface
- CI pipeline validating backend tests and frontend builds
- Full local stack with Docker Compose

## Project structure

```text
LinePulse/
├── backend/
│   ├── src/main/java/com/linepulse/
│   │   ├── asset/
│   │   ├── auth/
│   │   ├── common/
│   │   ├── config/
│   │   ├── dashboard/
│   │   ├── downtime/
│   │   ├── incident/
│   │   └── maintenance/
│   ├── src/test/java/com/linepulse/
│   └── Dockerfile
├── frontend/
│   ├── src/
│   ├── Dockerfile
│   └── nginx.conf
├── docs/
├── .github/workflows/
└── docker-compose.yml
```

## Run the complete stack with Docker

```bash
docker compose up --build
```

After the containers start:

```text
Frontend: http://localhost:5173
API: http://localhost:8080/api
Swagger: http://localhost:8080/swagger-ui/index.html
PostgreSQL: localhost:5432
```

Stop the stack with:

```bash
docker compose down
```

To also remove the local PostgreSQL volume:

```bash
docker compose down -v
```

## Run services manually

Start only PostgreSQL:

```bash
docker compose up -d postgres
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

## API documentation

With the backend running locally:

```text
http://localhost:8080/swagger-ui/index.html
```

The OpenAPI specification is available at:

```text
http://localhost:8080/v3/api-docs
```

Authenticate through `/api/auth/login`, copy the returned token and use the Swagger `Authorize` control for protected endpoints.

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

The Docker Compose configuration uses local development defaults and supports overriding `JWT_SECRET` and `VITE_API_URL` through environment variables.

## Roadmap

- Incident and maintenance history views
- More service and integration tests
- Audit history
- Operational charts and trend analysis
- AI-assisted incident classification
- Production deployment

## Status

MVP in active development.

## Author

Samara Konkol
