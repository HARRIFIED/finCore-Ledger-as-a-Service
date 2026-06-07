# finCore — Ledger-as-a-Service

A suite of independently deployable microservices forming a **multi-tenant, double-entry bookkeeping engine** capable of powering fintech products at scale. Built on Java 21, Spring Boot 3, gRPC, and Apache Kafka.

Services communicate internally over **gRPC** for low-latency calls and **Kafka** for event-driven workflows. The full system is containerised with Docker and deployable to Kubernetes.

---

## Architecture Overview

```
             External Clients (Mobile, Web, Partner APIs)
                                  │
                           API Gateway :8080
                    JWT validation via JWKS · routing
                    Injects: X-User-Id, X-Tenant-Id, X-User-Roles
                                  │
      ┌────────────┬──────────────┼──────────────┬──────────────┐
      │            │              │              │              │
  iam-service  account-svc   ledger-core    txn-svc      transfer-svc
  :8092/9092   :8081/9081    :8082/9082    :8083/9083    :8084/9084
  (issues JWTs,
   user/customer
   management)
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

### Auth Flow

```
1. Client POSTs credentials  →  iam-service issues JWT
   JWT payload: { sub: userId, tenant_id, email, roles: [...] }

2. All subsequent requests carry Bearer <token>  →  API Gateway validates
   against iam-service JWKS endpoint, then injects identity headers

3. Downstream services read X-User-Id / X-Tenant-Id from headers
   — they never validate JWTs themselves
   — ownerId is never accepted from the request body
```

---

## Roadmap

Services are built in strict dependency order. Each phase produces working, tested services before the next begins.

### Phase 1 — Identity & Gateway (current focus)

The auth foundation everything else depends on.

- [x] Root Gradle multi-module project setup
- [x] `account-service` — domain model: entity, enums, DTOs, service interface
- [ ] `api-contracts` — shared Protobuf definitions for all services
- [ ] `iam-service` — user/customer registration, login, JWT issuance, JWKS endpoint, tenant management
- [ ] `api-gateway` — Spring Cloud Gateway: JWT validation via JWKS, header propagation, routing to all services

### Phase 2 — Account Management

Completes account-service now that real auth context flows from the gateway.

- [ ] `account-service` — repository, service implementation, REST controllers, gRPC server
  - `ownerId` sourced from `X-User-Id` header (not request body)
  - All queries scoped by `tenantId`
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
| Framework         | Spring Boot 3.3.5                            |
| Service Mesh      | Spring Cloud 2023.0.3                        |
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
