# finCore Architecture

The system-design home for finCore: what we're building, the requirements it must
satisfy, and the decisions we've committed to (with their rationale).

## Contents

| Document | Purpose |
|----------|---------|
| [requirements.md](requirements.md) | Functional & non-functional requirements, the tenancy & party model, non-goals, and open questions |
| [ledger-core-design.md](ledger-core-design.md) | Domain design of the double-entry, event-sourced ledger: accounts, journal, postings, balances, money/FX, holds |
| [party-domain-design.md](party-domain-design.md) | Customer/Party (CIF) domain: party aggregate, KYC/KYB, relationships, retail vs corporate/SME, authorization |
| [fx-and-chart-of-accounts.md](fx-and-chart-of-accounts.md) | FX rates/quotes/spread & conversion postings; per-tenant Chart of Accounts and standard internal accounts |
| [service-interaction-map.md](service-interaction-map.md) | How services call each other (sync gRPC) and communicate via events (outbox → Kafka); external settlement path |
| [build-plan.md](build-plan.md) | Phased, incremental build sequence mapped to requirements, with current status and perf-hardening track |
| [decisions/](decisions/) | Architecture Decision Records (ADRs) — one file per significant, hard-to-reverse decision |

## How we work

1. **Requirements** are captured in `requirements.md` with stable IDs (`FR-n`, `NFR-n`)
   so design and code can trace back to them.
2. **Decisions** that are significant and hard to reverse are recorded as **ADRs** under
   `decisions/` — see [decisions/README.md](decisions/README.md) for the process.
3. **Design** artifacts (domain model, service boundaries, per-flow designs) are added
   here as they are produced, each tracing to the requirements it satisfies.

`PLAN.md` at the repo root remains local planning scratch (git-ignored). Anything meant
to be shared or durable lives here under `docs/`.

## Status

Early design. Product shape is settled (licensed, self-hosted, federated — see ADRs
0001–0002). Requirements are drafted and under review. Domain model and service design
are the next artifacts to add.
