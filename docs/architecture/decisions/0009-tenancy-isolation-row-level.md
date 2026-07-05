# 0009. Row-level tenant isolation with an escalation seam

- **Status:** Accepted
- **Date:** 2026-07-05
- **Deciders:** Harrified Tech

## Context

finCore is multi-tenant **within** a licensee deployment (a tenant = a program/brand/entity,
§2.1, `TEN-1..4`). The hard isolation boundary is already the per-licensee deployment
([ADR-0001](0001-licensed-self-hosted-engine.md)), so intra-licensee tenancy is a softer
boundary. We want the simplest standard model now without foreclosing stronger isolation later.

## Decision

Use **row-level `tenant_id`** (shared schema per service database) — the industry-standard
default. Preserve a clean escalation path to schema- or database-per-tenant by building a
**seam** now, so a future move is data relocation + routing change, not a logic rewrite:

1. `tenant_id` on **every** table; **tenant-leading composite indexes**.
2. All data access via a **tenant-scoped repository layer** + one **tenant-context resolver**
   (from the gateway's `X-Program-Id`); no ambient/global queries.
3. **No cross-tenant joins or queries.**
4. Datasource access behind an abstraction, so a per-tenant `AbstractRoutingDataSource` can
   be introduced later without touching business logic.
5. Optional: **Postgres partitioning by `tenant_id`** as a stepping stone; **RLS** to harden.

## Consequences

- Positive: simplest ops and tenant onboarding; standard tooling; escalation to
  schema/DB-per-tenant remains feasible (migration, not redesign).
- Trade-offs: requires discipline — tenant scoping must be enforced at the repository choke
  point and reviewed; a missed `tenant_id` is a data-leak class of bug (mitigated by RLS).

## Alternatives considered

- **Schema-per-tenant / DB-per-tenant now** — rejected as the default: heavier ops for a
  soft (intra-licensee) boundary; available later via the seam for tenants that need it.
