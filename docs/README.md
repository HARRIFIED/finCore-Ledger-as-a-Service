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
| Security | [IAM ↔ API Gateway federation flow](security/iam-api-gateway-flow.md) | How auth flows client → gateway → service: token issuance, JWKS validation, identity-header injection & stripping, and downstream consumption |
