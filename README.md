# finCore — Ledger-as-a-Service

This project intent to build a suite of independently deployable microservices that together form a **Ledger-as-a-Service** platform. At its core it will be a double-entry bookkeeping engine. Around it will be services that a real fintech product would need: account management, fund transfers, holds, FX conversion, audit logging, scheduled jobs, notifications, and reporting.

Services communicate internally over **gRPC** for low-latency calls and **Apache Kafka** for event-driven workflows. The full system is containerised with Docker and deployable to Kubernetes.

---

## Architecture Overview

```
                External Clients (Mobile, Web, Partner APIs)
                                    │
                             API Gateway :8080
                                    │
        ┌──────────┬───────────┬────┴────┬──────────┬──────────┐
        │          │           │         │          │          │
  account-svc  ledger-core  txn-svc  transfer  hold-svc   fx-svc
   :8081/9081  :8082/9082  :8083/9083 :8084/9084 :8085/9085 :8086/9086
                                    │
              ┌─────────────────────┼─────────────────────┐
              │                     │                     │
        scheduler-svc          audit-svc           reporting-svc
         :8087/9087             :8088/9088           :8091/9091
                                    │
                    notification-svc    webhook-svc
                       :8089               :8090/9090
```


## Roadmap

Services are built in dependency order — each phase produces working, tested services before the next begins.

### Phase 1 — Foundation
- [x] Root Gradle multi-module project setup
- [x] `account-service` — account lifecycle (create, get, freeze, close)
- [ ] `api-contracts` — shared Protobuf definitions for all services
- [ ] `api-gateway` — Spring Cloud Gateway with JWT auth and routing

### Phase 2 — The Ledger Engine
- [ ] `ledger-core-service` — double-entry journal, balance queries, point-in-time balances
- [ ] `transaction-service` — transaction posting, reversal, batch posting

### Phase 3 — Money Movement
- [ ] `transfer-service` — internal transfers between accounts
- [ ] `hold-service` — fund holds, captures, and releases
- [ ] `fx-service` — exchange rates and currency conversion

### Phase 4 — Reliability & Observability
- [ ] `audit-service` — immutable audit trail (DynamoDB-backed)
- [ ] `scheduler-service` — cron-based recurring job management
- [ ] `notification-service` — Kafka-driven email, SMS, push

### Phase 5 — Insights & Integrations
- [ ] `webhook-service` — merchant webhook delivery with retry
- [ ] `reporting-service` — ledger reports, statements, exports

### Infrastructure (progressive)
- [ ] Docker Compose local dev environment (all infra + services)
- [ ] Kubernetes manifests with Kustomize overlays (dev / staging / prod)
- [ ] CI pipeline

---

## Tech Stack

| Concern          | Choice                                        |
|------------------|-----------------------------------------------|
| Language         | Java 21                                       |
| Framework        | Spring Boot 4.0.6                             |
| Service Mesh     | Spring Cloud 2023.0.3                         |
| Inter-service RPC| gRPC (net.devh grpc-spring-boot-starter)      |
| Messaging        | Apache Kafka                                  |
| Primary DB       | PostgreSQL 16                                 |
| Audit Store      | DynamoDB (LocalStack locally)                 |
| Tracing          | Zipkin + Micrometer                           |
| Build            | Gradle 9 (Groovy DSL, multi-module)           |
| Containers       | Docker + Docker Compose                       |
| Orchestration    | Kubernetes + Kustomize                        |

---

## License

Copyright (c) 2026 Harrified Tech. All rights reserved.
