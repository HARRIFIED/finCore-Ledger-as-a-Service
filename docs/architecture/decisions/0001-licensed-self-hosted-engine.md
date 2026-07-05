# 0001. Licensed, self-hosted core ledger engine

- **Status:** Accepted
- **Date:** 2026-06-29
- **Deciders:** Harrified Tech

## Context

finCore is positioned as a core ledger engine for fintechs, modeled on how banks license
and run a core like Finacle.

## Decision

We will ship finCore as **software the licensee deploys and runs in its own environment**
(its own instance and databases). Harrified Tech issues licenses centrally and **never
hosts or accesses licensee data**. Distribution is signed container images + Helm; the
engine must run air-gapped; behaviour is configured, not forked (see `NFR-12`, `NFR-13`).

## Consequences

- Positive: strong data-residency story (`NFR-10`); each licensee is isolated by being a
  separate deployment; aligns the product with the core-banking market.
- Trade-offs: we are a software vendor, not a SaaS operator — upgrades/migrations run on
  the licensee side; we need a licensing/entitlement mechanism (future ADR).
- Cross-licensee multi-tenancy is out of scope; intra-licensee multi-tenancy is in scope
  (see [ADR-0004] and requirements §2.1).

## Alternatives considered

- **Multi-tenant SaaS** — rejected: wrong market fit and data-residency posture for a core.
