# LinePulse Architecture

## Overview

LinePulse is implemented as a modular monolith. The application is split into domain-focused packages while sharing one Spring Boot runtime and one PostgreSQL database.

This structure keeps the project simple to deploy and reason about while still separating responsibilities clearly enough for future growth.

## Why a modular monolith

A microservice architecture would add network communication, service discovery, distributed tracing, message delivery concerns and more deployment infrastructure without solving a current product requirement.

For the current scope, a modular monolith provides:

- clear domain boundaries;
- transactional consistency across related operations;
- simpler local development;
- lower operational cost;
- easier debugging and testing;
- a straightforward path to extract services later if scale or ownership boundaries require it.

## Backend modules

- `asset` — plants, sectors, production lines and machines;
- `auth` — registration, login, roles, JWT generation and authentication;
- `incident` — operational incident registration and lifecycle;
- `maintenance` — maintenance work orders and lifecycle transitions;
- `downtime` — machine downtime intervals;
- `dashboard` — operational indicators and incident trends;
- `audit` — persistent operational audit events;
- `common` — shared exceptions and API error handling;
- `config` — security, CORS and OpenAPI configuration.

## Request flow

The main backend flow is:

```text
HTTP request
    ↓
Controller
    ↓
Service
    ↓
Repository
    ↓
PostgreSQL
```

Controllers translate HTTP requests into application calls. Services contain business rules and transaction boundaries. Repositories abstract persistence through Spring Data JPA.

## Authentication flow

1. A user registers or logs in through `/api/auth`.
2. Passwords are stored as BCrypt hashes.
3. Successful authentication returns a signed JWT.
4. The frontend stores the session locally and sends the token as `Authorization: Bearer <token>`.
5. `JwtAuthenticationFilter` validates the token and reloads the user from PostgreSQL.
6. Spring Security evaluates endpoint authorization using the current database role.

Reloading the user on each authenticated request means disabling a user or changing their role takes effect without waiting for an already-issued token to expire.

## Roles

| Capability | OPERATOR | TECHNICIAN | ADMIN |
| --- | :---: | :---: | :---: |
| View dashboard | ✓ | ✓ | ✓ |
| View machines | ✓ | ✓ | ✓ |
| Open incidents | ✓ | ✓ | ✓ |
| Start/resolve/cancel incidents |  | ✓ | ✓ |
| Create work orders |  | ✓ | ✓ |
| Start/complete work orders |  | ✓ | ✓ |
| Register downtime |  | ✓ | ✓ |
| Close downtime |  | ✓ | ✓ |
| Change machine status |  | ✓ | ✓ |
| Register machines |  |  | ✓ |
| View audit trail |  | ✓ | ✓ |

## Operational indicators

### Availability

Availability is calculated over the last 24 hours using active machines and overlapping downtime intervals.

```text
availability = 100 × (1 - downtimeSeconds / availableMachineSeconds)
```

Where:

```text
availableMachineSeconds = activeMachineCount × 24 hours
```

Downtime intervals are clipped to the 24-hour calculation window so an event that started before the window only contributes the overlapping duration.

### MTTR

MTTR is calculated from maintenance work orders completed during the last 30 days.

```text
MTTR = average(completedAt - startedAt)
```

Only work orders with both timestamps are considered.

## Audit trail

Operational actions generate persistent audit records containing:

- action;
- entity type;
- entity identifier;
- human-readable description;
- authenticated user email;
- timestamp.

Audit records are stored in PostgreSQL and the most recent events are exposed only to technician and admin roles.

## Database evolution

Flyway owns schema evolution. The application runs Hibernate with `ddl-auto: validate`, so Hibernate checks the entity/schema contract without modifying the database automatically.

This keeps schema changes explicit and reviewable through versioned SQL migrations.

## Frontend data flow

The React frontend uses TanStack Query for server state.

```text
React component
    ↓
TanStack Query / Mutation
    ↓
Axios API client
    ↓
Spring Boot REST API
```

After write operations, related query keys are invalidated so the dashboard refreshes from the backend rather than manually mutating duplicated local state.

## Deployment model

The local stack contains three services:

```text
Nginx + React
      ↓
Spring Boot API
      ↓
PostgreSQL
```

Docker Compose builds and connects all three. GitHub Actions validates backend tests, the frontend production build and the complete Docker build on pushes and pull requests.

## Current tradeoffs

LinePulse intentionally does not currently use microservices, Kafka, Redis or Kubernetes. They would add operational complexity without a demonstrated requirement in the current scope.

Potential future extraction points include notifications, analytics and AI-assisted incident classification if those capabilities become independently scalable workloads.
