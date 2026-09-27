# LinePulse v3 — Multi-organization service network

## Product direction

LinePulse is evolving from a single-company internal maintenance system into an online platform where industrial companies and maintenance service providers can collaborate without sharing all internal operational data.

The target delivery model is a web platform (SaaS), not software installed separately at each customer. Companies, employees and external mechanics access the same platform through the browser. A future PWA can add installable mobile behavior, camera access for QR workflows and push notifications without replacing the web architecture.

## Core concepts

### Organization

An organization is the security and ownership boundary of the platform.

Types:

- `COMPANY` — an industrial company that owns plants, sectors, lines, machines and incidents.
- `SERVICE_PROVIDER` — an external mechanic, maintenance shop or service company that receives requests from one or more companies.

### Membership

A user can belong to one or more organizations. Permissions are progressively moving from a single global role to a role within an organization.

Planned organization roles:

- `OWNER`
- `ADMIN`
- `TECHNICIAN`
- `OPERATOR`
- `MECHANIC`

### Service request

A service request is not the same object as an incident or a work order.

- Incident: the operational problem reported on a machine.
- Service request: the request sent to an internal or external maintenance provider.
- Work order: the technical work that is actually executed.

Target lifecycle:

```text
REQUESTED → ACCEPTED → EN_ROUTE → IN_PROGRESS → COMPLETED
     └────→ DECLINED
     └────→ CANCELLED
```

A provider should only see requests explicitly shared with that provider and the minimum machine/incident context required to perform the service.

## Example flow

1. A company records an incident on a machine.
2. The company chooses internal maintenance or an external provider.
3. If external, a service request is sent to a trusted provider.
4. The provider sees requests from multiple client companies in one queue.
5. The provider accepts or declines and can provide an ETA.
6. A mechanic travels to the customer and starts the service.
7. The mechanic records diagnosis, solution, parts and service time.
8. The provider completes the service.
9. The company receives the result and keeps the history linked to its machine.

## Client models for service providers

### Connected client

The company also uses LinePulse and sends requests directly through the platform.

### Managed client

The company does not yet use LinePulse. The provider keeps a lightweight customer and equipment record for its own scheduling and service history. The client can later be invited to become a connected organization without losing the prior service history.

## Security model

Multi-organization access must be enforced in the backend, not only hidden in the frontend.

Required rules before external provider access is enabled:

- every company-owned asset must have an organization owner;
- every operational query must be scoped to an authorized organization;
- a provider must not see a company's full plant, machine, incident or audit dataset;
- shared service requests must expose only explicitly permitted context;
- organization membership and service-request assignment must be validated on every protected action;
- direct API calls using IDs from another tenant must return access denied or not found.

## Migration strategy

The migration is intentionally incremental to protect the existing v2 deployment.

### Phase 1 — organization foundation

- create `organizations`;
- create `organization_memberships`;
- migrate the current LinePulse installation into one default `COMPANY` organization;
- link existing plants to that organization;
- automatically associate existing and newly-created users with the current organization;
- expose the authenticated user's organizations.

### Phase 2 — workspace isolation

- introduce an active organization context;
- scope plants, machines, incidents, work orders, downtime, maintenance plans, dashboard metrics and audit queries;
- add cross-tenant authorization tests.

### Phase 3 — provider network

- create trusted provider relationships;
- create service requests and lifecycle transitions;
- create provider queue and company request tracking;
- allow provider mechanics to work across multiple clients without gaining general access to client data.

### Phase 4 — managed clients and mobile experience

- lightweight provider-managed customers;
- customer invitations and conversion to connected organizations;
- PWA installability;
- mobile QR workflow;
- push notifications when infrastructure is configured.

## Compatibility principle

The existing v2 flows remain functional during the migration. New multi-organization behavior is introduced behind explicit organization ownership and authorization instead of changing every domain at once.
