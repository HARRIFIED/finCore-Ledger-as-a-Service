# finCore Documentation

Committed, shareable documentation for the finCore engine — the docs that ship with
the repo.


## Structure

```
docs/
├── README.md              ← this index
├── architecture/          ← cross-cutting architecture & platform decisions
├── security/              ← authentication, federation, licensing
│   └── iam-api-gateway-flow.md
└── services/              ← per-service references (one file per service)
```

## Conventions

- **One concern per file.** A feature/flow that spans services (e.g. auth federation)
  lives under the area folder (`security/`), not inside a single service's doc.
- **Per-service docs** go in `services/<service-name>.md` and cover that service's
  API surface, config, and data ownership.
- Keep diagrams as fenced ASCII so they render everywhere without tooling.
- When behaviour is verified by running it, note *what was verified* and *when*.

## Index

| Area | Document | What it covers |
|------|----------|----------------|
| Architecture | [Architecture home](architecture/README.md) | System design entry point: requirements, decisions (ADRs), and design artifacts |
| Architecture | [Requirements](architecture/requirements.md) | Functional & non-functional requirements, tenancy & party model, non-goals, open questions |
| Architecture | [ledger-core design](architecture/ledger-core-design.md) | Double-entry, event-sourced ledger: accounts, journal, postings, balances, money/FX, holds |
| Architecture | [Party/customer domain design](architecture/party-domain-design.md) | Customer/Party (CIF): party aggregate, KYC/KYB, relationships, retail vs corporate/SME, authorization |
| Architecture | [FX & Chart of Accounts](architecture/fx-and-chart-of-accounts.md) | FX rates/quotes/spread & conversion postings; per-tenant CoA and standard internal accounts |
| Architecture | [Service interaction map](architecture/service-interaction-map.md) | Sync calls + event/outbox topology + external settlement path |
| Architecture | [Build plan](architecture/build-plan.md) | Phased, incremental build sequence mapped to requirements, with status |
| Architecture | [Decisions (ADRs)](architecture/decisions/) | Significant, hard-to-reverse decisions with rationale |
| Security | [IAM ↔ API Gateway federation flow](security/iam-api-gateway-flow.md) | How auth flows client → gateway → service: token issuance, JWKS validation, identity-header injection & stripping, and downstream consumption |
