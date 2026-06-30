# finCore — Licensed Core Ledger Engine

A suite of independently deployable microservices forming a **double-entry bookkeeping engine** that fintechs license and run in their own environment — obtaining a license, operating their own instance and database, and building their product around the engine (the way banks deploy and integrate with a core platform like Finacle). Built on Java 21, Spring Boot 4, gRPC, and Apache Kafka.

Harrified Tech issues licenses centrally and never hosts or accesses licensee data.

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
   — ownerId comes from X-Subject, never from the request body
```

---

## Deployment Model

finCore is delivered as software the **licensee runs themselves** — signed Docker images + Helm charts deployed into the licensee's own environment, backed by their own PostgreSQL, Kafka, and object storage. No data leaves the licensee's infrastructure. (License issuance and runtime enforcement are detailed in `PLAN.md`.)

---

## Roadmap

Services are built in strict dependency order. Each phase produces working, tested services before the next begins.

### Phase 1 — Identity & Gateway (current focus)

The auth foundation everything else depends on.

- [x] Root Gradle multi-module project setup
- [x] `account-service` — domain model: entity, enums, DTOs, service interface
- [ ] `api-contracts` — shared Protobuf definitions for all services
- [ ] `iam-service` — **optional** bundled IdP: user registration, login, JWT issuance, JWKS endpoint (licensees may federate to their own IdP instead)
- [x] `api-gateway` — Spring Cloud Gateway: validates tokens against a **configurable** issuer JWKS (licensee IdP or bundled iam-service), injects `X-Subject` / `X-Program-Id` / `X-Scopes`, routing to all services

### Phase 2 — Account Management

Completes account-service now that real auth context flows from the gateway.

- [ ] `account-service` — repository, service implementation, REST controllers, gRPC server
  - `ownerId` sourced from `X-Subject` header (not request body)
  - Queries optionally segmented by `X-Program-Id` (sub-program / brand)
  - Sub-account hierarchy via `parentAccountId`

### Phase 3 — The Ledger Engine

The financial core that all money-movement services post into.

- [ ] `ledger-core-service` — double-entry journal, balance queries, point-in-time balances
- [ ] `transaction-service` — transaction posting, reversal, batch posting

### Phase 4 — Money Movement

- [ ] `transfer-service` — internal transfers between accounts
- [ ] `hold-service` — fund holds, captures, and releases
- [ ] `fx-service` — exchange rates and currency conversion

### Phase 5 — Reliability & Observability

- [ ] `audit-service` — immutable audit trail (DynamoDB-backed)
- [ ] `scheduler-service` — cron-based recurring job management
- [ ] `notification-service` — Kafka-driven email, SMS, push

### Phase 6 — Insights & Integrations

- [ ] `webhook-service` — merchant webhook delivery with retry
- [ ] `reporting-service` — ledger reports, statements, exports

### Infrastructure (progressive, alongside each phase)

- [ ] Docker Compose local dev environment (infra + all services)
- [ ] Kubernetes manifests with Kustomize overlays (dev / staging / prod)
- [ ] CI pipeline

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

Copyright (c) 2026 Harrified Tech. All rights reserved.
