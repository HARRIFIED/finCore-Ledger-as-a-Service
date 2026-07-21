# finCore — Core Ledger Engine

This repo contains a suite of independently deployable microservices forming a **double-entry bookkeeping engine** that fintechs license and run in their own environment — operating their own instance and database, and building their product around the engine. Built on Java 21, Spring Boot 4, gRPC, and Apache Kafka.

> **New here? Start with the [documentation index](docs/README.md)** — especially the
> [architecture requirements](docs/architecture/requirements.md) and the
> [build plan](docs/architecture/build-plan.md).

---
## Architecture Overview
```
        Licensee backend (integration client · mTLS + OAuth2)
                                  │
                           API Gateway :8080
            Validates licensee-IdP tokens via configurable JWKS · routing
                    Injects: X-Subject, X-Program-Id, X-Scopes
                                  │
      ┌────────────┬──────────────┼──────────────┬──────────────┐
      │            │              │              │              │
  iam-service  account-svc   ledger-core    txn-svc      transfer-svc
  :8092/9092   :8081/9081    :8082/9082    :8083/9083    :8084/9084
  (optional bundled
   IdP — licensees may
   federate their own)
                                  │
      ┌───────────────────────────┼───────────────────────────┐
      │                           │                           │
  hold-svc                     fx-svc                  scheduler-svc
  :8085/9085                  :8086/9086                :8087/9087
                                  │
              ┌───────────────────┼───────────────────┐
              │                   │                   │
          audit-svc        notification-svc      webhook-svc
          :8088/9088           :8089              :8090/9090
                                  │
                            reporting-svc
                             :8091/9091
```

> `party-service` (the Customer/Party master · CIF · [ADR-0006](docs/architecture/decisions/0006-party-service-and-authorization.md))
> is a peer service, omitted from this sketch. See the
> [service interaction map](docs/architecture/service-interaction-map.md) for the full, current picture.

### Auth Flow (Federated)

finCore validates the **licensee's own IdP tokens** — it does not own the licensee's users. The bundled `iam-service` is optional, for licensees without an existing IdP. The primary caller is the **licensee's backend**, not the end-user device.

```
1. Licensee's backend authenticates to finCore via OAuth2 client-credentials
   (+ mTLS). Service-to-service, not end-user-to-engine.

2. Per-deployment config sets the trusted issuer — the licensee's own IdP
   (JWKS URL) or the optional bundled iam-service. The API Gateway validates
   token signatures against that JWKS, then injects identity headers.

3. Downstream services read X-Subject / X-Program-Id from headers
   — they never validate tokens themselves
   — the acting party is resolved via party-service, never from the request body
```

Full write-up: [IAM ↔ API Gateway federation flow](docs/security/iam-api-gateway-flow.md).

---

## Deployment Model

finCore is delivered as software the **licensee runs themselves** — signed Docker images + Helm charts deployed into the licensee's own environment, backed by their own PostgreSQL, Kafka, and object storage. No data leaves the licensee's infrastructure. (Licensing & entitlement is a planned subsystem — see [build plan](docs/architecture/build-plan.md), Phase 10.)

---

## Documentation

All committed docs live under [`docs/`](docs/) — see the [index](docs/README.md) for conventions.

**Architecture & design** ([`docs/architecture/`](docs/architecture/)):

- [Requirements](docs/architecture/requirements.md) — functional & non-functional requirements, tenancy & party model, non-goals.
- [Build plan](docs/architecture/build-plan.md) — the phased, incremental build sequence (**start here for what's next**).
- [ledger-core design](docs/architecture/ledger-core-design.md) — double-entry, event-sourced ledger: accounts, journal, postings, balances.
- [Party/customer domain](docs/architecture/party-domain-design.md) — the CIF: parties, KYC/KYB, relationships, authorization.
- [FX & Chart of Accounts](docs/architecture/fx-and-chart-of-accounts.md) — rates/quotes/spread, conversion postings, per-tenant CoA.
- [Service interaction map](docs/architecture/service-interaction-map.md) — sync calls + event/outbox topology.
- [Decisions (ADRs)](docs/architecture/decisions/) — significant, hard-to-reverse choices with rationale.

**Feature flows** ([`docs/features/`](docs/features/)): end-to-end walkthroughs of how the services work together — e.g. [Opening an account](docs/features/opening-an-account.md).

**Security**: [IAM ↔ API Gateway federation flow](docs/security/iam-api-gateway-flow.md).

---

## Build status & plan

The phased, incremental build sequence is maintained in
**[docs/architecture/build-plan.md](docs/architecture/build-plan.md)** — the source of truth
(this README no longer keeps a separate roadmap, to avoid drift).

Current status:

- ✅ **api-gateway** — federated JWT validation, identity-header inject/strip, routing.
- ✅ **platform-security / -tenancy / -outbox** — shared identity, row-level tenancy seam, transactional outbox.
- ✅ **ledger-core** — double-entry posting + invariant, balances, snapshots, point-in-time, reversals, events.
- ✅ **party-service** — CIF: parties (retail + corporate) + memberships & authorization.
- 🟡 **account-service** — open-account works (authorize via party → create ledger account, keyed on `partyId`); lifecycle/CoA pending.
- 🟡 **iam-service** — optional bundled IdP (register/login/JWT/JWKS); KYC to move to party-service.

**Next:** finish party (KYC/KYB), flesh out account-service (CoA/products, lifecycle), or Phase 4 money movement — see the [build plan](docs/architecture/build-plan.md).

---

## Service Catalog

| Service              | HTTP  | gRPC  | Database        |
|----------------------|-------|-------|-----------------|
| api-gateway          | 8080  | —     | —               |
| account-service      | 8081  | 9081  | account_db      |
| ledger-core-service  | 8082  | 9082  | ledger_core_db  |
| transaction-service  | 8083  | 9083  | transaction_db  |
| transfer-service     | 8084  | 9084  | transfer_db     |
| hold-service         | 8085  | 9085  | hold_db         |
| fx-service           | 8086  | 9086  | fx_db           |
| scheduler-service    | 8087  | 9087  | scheduler_db    |
| audit-service        | 8088  | 9088  | DynamoDB        |
| notification-service | 8089  | —     | notification_db |
| webhook-service      | 8090  | 9090  | webhook_db      |
| reporting-service    | 8091  | 9091  | reporting_db    |
| iam-service          | 8092  | 9092  | identity_db     |
| party-service        | 8093  | 9093  | party_db        |

> `platform-security` is a shared **library** module (no runtime service, ports, or DB) that
> every servlet service uses to consume gateway-injected identity ([CallerContext](docs/security/iam-api-gateway-flow.md)).

---

## Tech Stack

| Concern           | Choice                                       |
|-------------------|----------------------------------------------|
| Language          | Java 21                                      |
| Framework         | Spring Boot 4.0.6                            |
| Service Mesh      | Spring Cloud 2025.1.2 (Oakwood)              |
| API Gateway       | Spring Cloud Gateway                         |
| Inter-service RPC | gRPC (net.devh grpc-spring-boot-starter)     |
| Messaging         | Apache Kafka                                 |
| Primary DB        | PostgreSQL 16                                |
| Audit Store       | DynamoDB (LocalStack locally)                |
| Tracing           | Zipkin + Micrometer                          |
| Build             | Gradle 8.9 (Groovy DSL, multi-module)        |
| Containers        | Docker + Docker Compose                      |
| Orchestration     | Kubernetes + Kustomize                       |

---

## License

finCore is **dual-licensed** — a free/open-source community edition and a paid commercial edition.

- **Community Edition — [AGPL-3.0-only](LICENSE).** You may use, modify, and redistribute finCore
  under the terms of the [GNU Affero General Public License v3.0](LICENSE). The copyleft is
  deliberate and covers derivative works in full: any modified version you distribute **or run as a
  network service** must be released under the AGPL-3.0, with its complete corresponding source made
  available to users (AGPL §13). You may **not** re-license finCore or any derivative under different
  or proprietary terms.

- **Commercial Edition.** Organizations that cannot meet the AGPL's obligations — e.g. embedding
  finCore in a closed-source product, or operating a modified finCore as a hosted service without
  publishing their changes — can obtain a separate commercial license that lifts the copyleft
  requirements. See **[COMMERCIAL-LICENSE.md](COMMERCIAL-LICENSE.md)**.

Both editions are built from the same source. Contributions are accepted under the terms in
**[CONTRIBUTING.md](CONTRIBUTING.md)**, which allow finCore to be offered under both licenses.

Copyright © 2026–present Harrison Ikpefua (Harrified Tech) and contributors.
