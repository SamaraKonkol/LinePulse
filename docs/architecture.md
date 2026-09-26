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

- `asset` — plants, sectors, production lines, machine hierarchy and asset lifecycle;
- `auth` — employee registrations, administrator-managed accounts, login, roles, JWT generation and authentication;
- `incident` — operational incident registration and lifecycle;
- `maintenance` — maintenance work orders and lifecycle transitions;
- `downtime` — machine downtime intervals;
- `dashboard` — operational indicators and incident trends;
- `audit` — persistent operational audit events;
- `common` — shared exceptions and API error handling;
- `config` — security, CORS and OpenAPI configuration.

## Request flow

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

1. An administrator creates a user with a unique employee registration, password and role.
2. Passwords are stored as BCrypt hashes.
3. The user logs in with registration and password through `/api/auth/login`.
4. Successful authentication returns a signed JWT whose subject is the employee registration.
5. The frontend stores the session locally and sends the token as `Authorization: Bearer <token>`.
6. `JwtAuthenticationFilter` validates the token and reloads the user from PostgreSQL by registration.
7. Spring Security evaluates endpoint authorization using the current database role.

Reloading the user on each authenticated request means disabling, deleting or changing the role of a user takes effect without waiting for an already-issued token to expire.

## Industrial structure

Assets belong to an explicit hierarchy:

```text
Plant
  ↓
Sector
  ↓
Production line
  ↓
Machine
```

Administrators create and manage the first three levels. Each level can be activated or deactivated without deleting its historical records. A machine can only be created or moved into a line when the line, sector and plant are active.

Machine `INACTIVE` status represents asset retirement from active operation. Technicians can manage operational states such as running, stopped and maintenance, while only administrators can retire or reactivate an asset.

## Roles

| Capability | OPERATOR | TECHNICIAN | ADMIN |
| --- | :---: | :---: | :---: |
| View dashboard | ✓ | ✓ | ✓ |
| View machines and machine details | ✓ | ✓ | ✓ |
| Open incidents | ✓ | ✓ | ✓ |
| Start/resolve/cancel incidents |  | ✓ | ✓ |
| Create work orders |  | ✓ | ✓ |
| Start/complete work orders |  | ✓ | ✓ |
| Register downtime |  | ✓ | ✓ |
| Close downtime |  | ✓ | ✓ |
| Change operational machine status |  | ✓ | ✓ |
| Retire/reactivate machines |  |  | ✓ |
| Register/edit machines |  |  | ✓ |
| Manage plants/sectors/lines |  |  | ✓ |
| Create/manage/delete users |  |  | ✓ |
| View audit trail |  | ✓ | ✓ |

## Operational indicators

### Availability

Availability is calculated over the last 24 hours using active machines and overlapping downtime intervals.

```text
availability = 100 × (1 - downtimeSeconds / availableMachineSeconds)
```

The frontend also derives contextual availability by machine and production line from the same downtime intervals for operational drill-down views.

### MTTR

MTTR is calculated from maintenance work orders with both start and completion timestamps.

```text
MTTR = average(completedAt - startedAt)
```

The global dashboard uses completed work orders from the last 30 days. Machine details derive an asset-specific MTTR from the machine's completed orders.

### Operational drill-down

The React application combines machine, incident, work-order and downtime data already loaded through the API to provide:

- per-line availability;
- downtime accumulated in the last 24 hours;
- machine attention/risk ordering;
- machine-specific incident, maintenance and downtime history.

These contextual calculations remain client-side because they do not introduce new persistence or authorization requirements.

## Audit trail

Operational actions generate persistent audit records containing:

- action;
- entity type;
- entity identifier;
- human-readable description;
- authenticated employee registration;
- timestamp.

Audit records are stored in PostgreSQL and exposed only to technician and admin roles. The actor registration remains in the audit record even if the user account is later deleted. The frontend can filter the loaded audit trail by registration, action, entity and time window.

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

```text
GitHub Pages / React
        ↓
Render / Spring Boot API
        ↓
Render / PostgreSQL
```

Docker Compose provides the equivalent local stack. GitHub Actions validates backend tests, the frontend production build and the complete Docker build on pushes and pull requests.

## Current tradeoffs

LinePulse intentionally does not currently use microservices, Kafka, Redis or Kubernetes. They would add operational complexity without a demonstrated requirement in the current scope.

Potential future extraction points include notifications, analytics and AI-assisted incident classification if those capabilities become independently scalable workloads.
