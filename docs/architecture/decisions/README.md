# Architecture Decision Records

Each ADR captures one **significant, hard-to-reverse** decision: the context, the choice,
and its consequences. ADRs are immutable once accepted — to change a decision, add a new
ADR that supersedes the old one (and mark the old one `Superseded by NNNN`).

## Process

1. Copy [`0000-adr-template.md`](0000-adr-template.md) to `NNNN-short-title.md` (next number).
2. Fill it in; open it as `Proposed`.
3. Once agreed, set `Accepted` with the date.
4. Keep it short — an ADR is a decision record, not a design doc.

## Index

| ADR | Title | Status |
|-----|-------|--------|
| [0001](0001-licensed-self-hosted-engine.md) | Licensed, self-hosted core ledger engine | Accepted |
| [0002](0002-federated-authentication.md) | Federated authentication at the gateway | Accepted |
| [0003](0003-database-per-service.md) | Database-per-service | Accepted |
| [0004](0004-separate-customer-party-master.md) | Separate Customer/Party master from login identity | Accepted (principle) |
| [0005](0005-event-sourced-ledger-with-snapshots.md) | Event-sourced ledger with periodic snapshots | Accepted |
| [0006](0006-party-service-and-authorization.md) | Dedicated party-service and finCore-owned authorization | Accepted |
| [0007](0007-fx-rates-and-conversion.md) | FX rates and conversion (internal table + quotes, configurable spread) | Accepted |
| [0008](0008-chart-of-accounts-template-seeded.md) | Template-seeded Chart of Accounts per tenant | Accepted |
| [0009](0009-tenancy-isolation-row-level.md) | Row-level tenant isolation with an escalation seam | Accepted |
| [0010](0010-agpl-dual-licensing.md) | AGPL-3.0 community edition with a commercial dual license | Accepted |
