# LinePulse V3 — Multi-organization service network

## Goal

V3 evolves LinePulse from a single-company industrial maintenance workspace into a multi-organization platform where industrial companies can operate their own maintenance teams and securely collaborate with external maintenance providers.

## Organization model

Organizations have one of two types:

- `COMPANY` — owns plants, sectors, production lines, machines, incidents, downtime, maintenance plans and work orders.
- `SERVICE_PROVIDER` — receives external service requests from trusted client companies and manages technicians/mechanics.

Users belong to organizations through `organization_memberships`. Membership roles are:

- `OWNER`
- `ADMIN`
- `TECHNICIAN`
- `MECHANIC`
- `OPERATOR`

The active workspace is selected with the `X-LinePulse-Organization` request header. The backend validates that the authenticated user has an active membership in that organization before applying the tenant context.

## Authorization model

Legacy JWT `UserRole` is no longer trusted as the effective request authority in multi-organization requests. After JWT authentication, `OrganizationAuthorityFilter` derives Spring Security authorities from the selected organization membership.

For company workspaces:

- `OWNER` / `ADMIN` -> `ROLE_ADMIN`
- `TECHNICIAN` / `MECHANIC` -> `ROLE_TECHNICIAN`
- `OPERATOR` -> `ROLE_OPERATOR`

For service-provider workspaces, provider administrators do not become platform-wide administrators. Non-operator provider roles receive the effective technical authority required for service execution, while organization administration is enforced by membership checks in the V3 services.

Tenant-owned queries for industrial structure, machines, incidents, work orders, downtime, maintenance plans, dashboard metrics and audit events are scoped to the active organization.

## Trusted provider network

A company can link an existing service provider or onboard a new one. A relationship is stored in `organization_service_relationships` and can be `ACTIVE` or `SUSPENDED`.

An external service request can only target a provider with an active trusted relationship to the company.

Provider-network endpoints:

```text
GET   /api/provider-network/providers
POST  /api/provider-network/providers
GET   /api/provider-network/relationships
POST  /api/provider-network/relationships
PATCH /api/provider-network/relationships/{id}/suspend
```

## Service request workflow

A company opens a service request for one of its own machines and can optionally link an incident.

Channels:

- `INTERNAL` — executed by the company's technical team.
- `EXTERNAL` — executed by the selected trusted service provider.

Priorities:

- `LOW`
- `MEDIUM`
- `HIGH`
- `CRITICAL`

Lifecycle:

```text
REQUESTED
  -> ACCEPTED
      -> EN_ROUTE        (external only)
      -> IN_PROGRESS
          -> COMPLETED
              -> APPROVED

REQUESTED -> DECLINED
REQUESTED/ACCEPTED -> CANCELLED
```

Main endpoints:

```text
GET   /api/service-requests
GET   /api/service-requests/{id}
POST  /api/service-requests
PATCH /api/service-requests/{id}/accept
PATCH /api/service-requests/{id}/decline
PATCH /api/service-requests/{id}/assign
PATCH /api/service-requests/{id}/eta
PATCH /api/service-requests/{id}/en-route
PATCH /api/service-requests/{id}/start
PATCH /api/service-requests/{id}/complete
PATCH /api/service-requests/{id}/approve
PATCH /api/service-requests/{id}/cancel
```

## V2 work-order integration

Service requests do not replace maintenance work orders.

When a request is accepted, LinePulse creates and links a corrective work order owned by the company. Service execution then synchronizes the work order lifecycle:

- request accepted -> corrective work order created
- request started -> work order `IN_PROGRESS`
- request completed -> work order `COMPLETED`
- accepted request cancelled before execution -> work order `CANCELLED`

This keeps MTTR, maintenance history and company reliability data compatible with V2.

## Shared information boundary

A service provider receives only the service context required to execute its assigned requests: company identity, machine identity/asset code, linked incident title, priority, ETA, assigned technician and execution notes. The provider does not gain access to the client's full industrial structure or unrelated incidents/work orders.

Direct UUID access is tenant-scoped. Integration tests verify that a second provider cannot list, read or accept a request assigned to another provider.

## Organization member management

Organization owners/admins can manage their own team through:

```text
GET   /api/organization-members
POST  /api/organization-members
PATCH /api/organization-members/{userId}/role
PATCH /api/organization-members/{userId}/status
```

Provider owners can create mechanics/technicians without receiving the legacy global `ADMIN` authority.

## Audit trail

Audit events are organization-scoped. Migration V10 assigns historical events to the legacy default organization and requires `organization_id` for new events.

External service lifecycle events are written to both the company and provider audit trails so each organization has a trace of the shared operation without receiving unrelated tenant events.

The preventive scheduler records audit events using the organization of the plan's machine and therefore does not depend on an authenticated browser workspace.

## Authentication hardening

Public production deployment disables demo accounts. Demo controls remain available only in local development.

Login failures are throttled per normalized registration: five failed attempts within a 15-minute window cause HTTP `429 Too Many Requests` until the sliding window permits another attempt. The current implementation is process-local; a horizontally scaled deployment should move this state to Redis or another shared rate-limit store.

## Database migrations

- `V8__organizations_and_memberships.sql` — organization/membership foundation.
- `V9__provider_network_and_service_requests.sql` — trusted-provider relationships and service requests.
- `V10__scope_audit_events_by_organization.sql` — tenant-scoped audit trail.

## Frontend

The React dashboard includes a V3 service-network panel with:

- workspace selector
- internal/external service-request creation
- trusted-provider selection and onboarding
- provider queue
- accept/decline actions
- ETA and en-route status
- technician/mechanic assignment
- start/complete execution
- service and parts notes
- company approval/cancellation
- organization member management

The layout is responsive for desktop, tablet and mobile access.

## Test coverage

V3 includes integration coverage with PostgreSQL/Testcontainers for:

- cross-organization industrial-structure isolation
- external service-request isolation between providers
- full external request lifecycle and linked work-order creation
- effective role derivation from selected organization membership
- rejection of unknown workspace IDs
- legacy user creation into the selected workspace
- prevention of automatic default-membership recreation after membership deactivation

The repository CI continues to validate backend tests, frontend build, Playwright E2E and the Docker application stack.
