# 0003. Database-per-service

- **Status:** Accepted
- **Date:** 2026-06-29
- **Deciders:** Harrified Tech

## Context

finCore is a set of independently deployable services. Sharing one schema would couple
their release cycles and blur data ownership.

## Decision

Each service owns its **own database** (`account_db`, `ledger_core_db`, `identity_db`, …)
and is the sole writer of its data. Cross-service data flows via APIs and events, never by
another service reading/writing a foreign schema. Per-service configuration reflects this
(each service's own `DB_URL`; see the per-service `.env` convention).

## Consequences

- Positive: clear ownership, independent migrations/scaling, no hidden coupling.
- Trade-offs: no cross-service SQL joins or DB transactions — consistency across services
  is achieved with events / outbox (`FR-25`, `NFR-2`); some data is duplicated by design.

## Alternatives considered

- **Shared database** — rejected: couples services and violates ownership boundaries.
