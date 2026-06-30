# finCore — Licensed Core Ledger Engine

A production-ready, cloud-native **core ledger engine** built on Java 21, Spring Boot 4.0.6, and gRPC. Designed by Harrified Tech as a **licensed, deployable** double-entry bookkeeping engine: a fintech obtains a license, runs its own instance and database in its own environment, and builds its product around the engine — the same way a bank deploys and integrates with a core banking platform like Finacle.

Harrified Tech issues and manages licenses centrally. Licensees operate the engine themselves; **finCore never hosts or accesses licensee data.**

---

## Service Architecture

```
                          ┌─────────────────────────────────────────────────────┐
                          │                   External Clients                  │
                          │            (Mobile, Web, Partner APIs)              │
                          └─────────────────────┬───────────────────────────────┘
                                                │ HTTPS
                          ┌─────────────────────▼───────────────────────────────┐
                          │                  API Gateway                         │
                          │    Spring Cloud Gateway — JWT validation, routing    │
                          │         Forwards: X-User-Id, X-Tenant-Id,           │
                          │                  X-User-Roles headers                │
                          │                  :8080 (HTTP)                        │
                          └──┬───┬──────┬──────┬──────┬──────┬──────┬───────────┘
                             │   │      │      │      │      │      │  gRPC
              ┌──────────────┘   │      │      │      │      │      └──────────────────┐
              │         ┌────────┘      │      │      │      │                         │
    ┌─────────▼──────┐  │  ┌──────────▼──┐  │  ┌──▼───────────┐   ┌───────────────▼──┐
    │  identity-     │  │  │ transaction │  │  │  transfer-   │   │   hold-service   │
    │  service       │  │  │  -service   │  │  │  service     │   │  :8085 / :9085   │
    │ :8092 / :9092  │  │  │:8083 / :9083│  │  │:8084 / :9084 │   └──────────────────┘
    │ (issues JWTs,  │  │  └─────────────┘  │  └──────────────┘
    │  user/customer │  │                   │
    │  management)   │  │  ┌──────────────▼─────────────────────────────────┐
    └────────────────┘  │  │                 ledger-core-service             │
                        │  │          Double-Entry Ledger Engine             │
              ┌─────────▼──┐│                 :8082 / :9082                  │
    ┌─────────▼──────────┐ │└─────────────────────────────────────────────────┘
    │  account-service   │              │            │           │           │
    │  :8081 / :9081     │  ┌───────────┘            │           │           └───────────────┐
    └────────────────────┘  │                        │           │                           │
                  ┌─────────▼──────┐   ┌─────────────▼──┐  ┌────▼────────────┐   ┌─────────▼──────┐
                  │  fx-service    │   │ scheduler-     │  │ audit-service   │   │  reporting-    │
                  │ :8086 / :9086  │   │ service        │  │ :8088 / :9088   │   │  service       │
                  └────────────────┘   │:8087 / :9087   │  └─────────────────┘   │:8091 / :9091   │
                                       └────────────────┘                         └────────────────┘

    ┌────────────────────┐   ┌────────────────────┐
    │ notification-svc   │   │  webhook-service   │
    │  :8089 (HTTP only) │   │  :8090 / :9090     │
    │  (Kafka-based)     │   └────────────────────┘
    └────────────────────┘

    ┌────────────────────────────────────────────────────────────────────────────────┐
    │                           Infrastructure Layer                                 │
    │  PostgreSQL 16  |  Apache Kafka  |  LocalStack (AWS)  |  Elasticsearch  |     │
    │  Zipkin (Tracing)  |  Kafka UI                                                 │
    └────────────────────────────────────────────────────────────────────────────────┘
```

### Auth Flow (Federated)

finCore does **not** own the licensee's users. Each licensee points the engine at **their own identity provider**; finCore validates the licensee's tokens rather than issuing its own. The bundled `iam-service` is **optional** — a default IdP for licensees who don't already have one.

The primary integration is **backend-to-backend**: the end user authenticates with the licensee's app/backend, and the licensee's *backend* calls finCore (never the end-user device directly) — mirroring how a bank's channels reach a core through the bank's own backend.

```
1. Licensee's backend authenticates to finCore via OAuth2 client-credentials
   (+ mTLS). Service-to-service, not end-user-to-engine.

2. Per-deployment config sets the trusted token issuer:
     - the licensee's own IdP (JWKS URL), OR
     - the optional bundled iam-service.
   API Gateway validates token signatures against that configured JWKS.

3. Gateway injects headers from the validated claims:
     X-Subject     ← opaque end-user id from the licensee's IdP (on-behalf-of)
     X-Program-Id  ← optional sub-program / brand segment (replaces tenant_id)
     X-Scopes      ← granted scopes / roles

4. Downstream services trust gateway headers — they do NOT validate tokens.
   ownerId is read from X-Subject, never from the request body.
```

---

## Deployment & Licensing Model

finCore is delivered as software, not a hosted service. Harrified Tech is a **vendor**; the licensee operates the data plane.

**Deployment** — The licensee runs the engine in their own environment (on-prem or their own cloud). Harrified Tech ships **signed Docker images + Helm charts**; the licensee supplies their own PostgreSQL, Kafka, and object storage (or managed equivalents). No data ever leaves the licensee's infrastructure.

**Licensing** — A central license-issuance service (run by Harrified Tech) mints **signed license files** that the engine validates at runtime:

| License field    | Purpose                                                        |
|------------------|----------------------------------------------------------------|
| `licensee_id`    | Identifies the licensed organization                           |
| `expiry`         | License validity window                                        |
| `modules`        | Enabled engine modules (e.g. fx, hold, reporting)              |
| `capacity`       | Caps: max accounts, max TPS, currencies                        |
| `environment`    | `prod` vs `non-prod` entitlement                               |
| `signature`      | Ed25519 signature over the above, verified by the engine       |

- The engine verifies the signature **on boot and periodically**, enforces capacity caps, and **degrades gracefully** (grace periods + warnings) — it never hard-stops a live ledger.
- Validation works **air-gapped**; there is **no mandatory phone-home**. Usage telemetry for billing is optional and opt-in.
- **API stability is a commitment:** the published `.proto` / OpenAPI surface is backward-compatible within a major version; breaking changes are gated to major releases. Distribution uses a private registry with stable/LTS channels, and DB migrations are run by the licensee on upgrade.

---

## Customer Integration Model

How a licensee plugs their product into the engine — the equivalent of how a bank integrates with Finacle.

```
   End user ──▶ Licensee app ──▶ Licensee backend ──▶ finCore API Gateway ──▶ engine
                                  (integration client)     (mTLS + OAuth2)
```

- **Integration surface** — gRPC (high-performance, service-to-service) and REST via the gateway, plus **webhooks** (`webhook-service`) for async events the licensee subscribes to. Contracts are published as `.proto` / OpenAPI with a sandbox environment.
- **Configuration, not forking** — Product definitions, fee schedules, GL / chart-of-accounts mapping, currencies, and business rules are **configuration the licensee supplies**. The engine is never forked, so Harrified Tech can ship updates cleanly.
- **Data ownership** — finCore is the **system of record for the ledger** (accounts, balances, journal, transactions). The licensee keeps its **own** application and user data and references finCore records via `X-Subject`. No shared schema, no shared database across licensees.

---

## Prerequisites

| Tool          | Version  | Purpose                         |
|---------------|----------|---------------------------------|
| Java JDK      | 21+      | Build & run services            |
| Docker        | 24+      | Container runtime               |
| Docker Compose| 2.20+    | Local dev environment           |
| kubectl       | 1.28+    | Kubernetes CLI                  |
| kustomize     | 5.0+     | K8s configuration management    |
| Gradle        | 8.9      | Build tool (wrapper included)   |
| protoc        | 3.25.5   | Proto compilation (auto-managed)|

---

## Quick Start (Local Development)

### 1. Start all infrastructure and services

```bash
docker-compose up -d
```

This starts:
- PostgreSQL 16 with all 11 databases pre-created
- Apache Kafka + Zookeeper
- LocalStack (DynamoDB, S3, SQS, SES, Secrets Manager, KMS)
- Elasticsearch 8
- Zipkin distributed tracing
- Kafka UI
- All 13 microservices

### 2. Verify services are healthy

```bash
docker-compose ps
```

### 3. Access the API

```
API Gateway:  http://localhost:8080
Kafka UI:     http://localhost:8888
Zipkin:       http://localhost:9411
Elasticsearch: http://localhost:9200
```

### 4. Tear down

```bash
docker-compose down          # Stop containers, keep volumes
docker-compose down -v       # Stop and delete all data volumes
```

---

## Service Catalog

| Service              | HTTP Port | gRPC Port | Database         | Description                                          |
|----------------------|-----------|-----------|------------------|------------------------------------------------------|
| api-gateway          | 8080      | —         | —                | Spring Cloud Gateway, JWT validation, routing        |
| account-service      | 8081      | 9081      | account_db       | Account lifecycle management                         |
| ledger-core-service  | 8082      | 9082      | ledger_core_db   | Double-entry journal engine                          |
| transaction-service  | 8083      | 9083      | transaction_db   | Transaction posting and reversal                     |
| transfer-service     | 8084      | 9084      | transfer_db      | Internal & external fund transfers                   |
| hold-service         | 8085      | 9085      | hold_db          | Fund holds, captures, releases                       |
| fx-service           | 8086      | 9086      | fx_db            | FX rates and currency conversion                     |
| scheduler-service    | 8087      | 9087      | scheduler_db     | Cron-based recurring job management                  |
| audit-service        | 8088      | 9088      | DynamoDB         | Immutable audit trail                                |
| notification-service | 8089      | —         | notification_db  | Kafka-driven email/SMS/push notifications            |
| webhook-service      | 8090      | 9090      | webhook_db       | Merchant webhook delivery                            |
| reporting-service    | 8091      | 9091      | reporting_db     | Statements, ledger reports, exports                  |
| iam-service     | 8092      | 9092      | identity_db      | **Optional** bundled IdP (user mgmt, JWT issuance) — licensees may federate to their own IdP instead |

---

## Technology Stack

| Category         | Technology                              |
|------------------|-----------------------------------------|
| Language         | Java 21                                 |
| Framework        | Spring Boot 4.0.6                       |
| Service Mesh     | Spring Cloud 2025.1.2 (Oakwood)         |
| API Gateway      | Spring Cloud Gateway                    |
| RPC              | gRPC (net.devh:grpc-spring-boot-starter:3.1.0.RELEASE) |
| Serialization    | Protocol Buffers 3.25.5                 |
| Messaging        | Apache Kafka (Confluent 7.7.0)          |
| Databases        | PostgreSQL 16, DynamoDB (via LocalStack)|
| Object Storage   | AWS S3 (via LocalStack)                 |
| Search           | Elasticsearch 8.15                      |
| Tracing          | Zipkin 3.4 + Micrometer                 |
| Build Tool       | Gradle 8.9 (Groovy DSL)                 |
| Containerization | Docker + Docker Compose                 |
| Orchestration    | Kubernetes + Kustomize                  |
| AWS SDK          | AWS SDK for Java 2.28.4                 |
| Testing          | JUnit 5, Testcontainers 1.20.3          |

---

## Development Guide

### Building the entire project

```bash
./gradlew build
```

### Running a single service locally

Each service can run independently. Ensure PostgreSQL and Kafka are running first (use `docker-compose up postgres kafka zookeeper -d`).

```bash
# Run account-service
./gradlew :account-service:bootRun --args='--spring.profiles.active=dev'

# Or build and run the JAR
./gradlew :account-service:bootJar
java -jar account-service/build/libs/account-service-0.0.1-SNAPSHOT.jar
```

### Running tests

```bash
# All tests
./gradlew test

# Tests for a specific module
./gradlew :account-service:test

# Integration tests (requires Docker for Testcontainers)
./gradlew :account-service:integrationTest
```

### Checking service health

```bash
curl http://localhost:8081/actuator/health
```

---

## gRPC & Protobuf

### Proto files location

All `.proto` files live in `api-contracts/src/main/proto/`. The `api-contracts` module is a pure shared library — it has no HTTP server.

### Available proto definitions

| Proto File       | Service           | RPCs                                                                              |
|------------------|-------------------|-----------------------------------------------------------------------------------|
| common.proto     | —                 | Shared types: Money, PageRequest, PageMeta, Error                                 |
| identity.proto   | IdentityService   | RegisterUser, GetUser, UpdateUser, ListUsers, DeactivateUser, GetJwks             |
| account.proto    | AccountService    | CreateAccount, GetAccount, ListAccounts, UpdateAccount, CloseAccount, FreezeAccount |
| ledger.proto     | LedgerService     | PostJournalEntry, GetBalance, GetBalanceAtPointInTime, GetLedgerEntries           |
| transaction.proto| TransactionService| PostTransaction, GetTransaction, ReverseTransaction, BatchPostTransactions        |
| transfer.proto   | TransferService   | InitiateTransfer, GetTransfer, ListTransfers                                      |
| hold.proto       | HoldService       | CreateHold, ReleaseHold, CaptureHold, GetHold                                     |
| fx.proto         | FxService         | GetExchangeRate, ConvertCurrency, GetConversionHistory                            |
| audit.proto      | AuditService      | RecordEvent, GetEvent, QueryEvents                                                |
| scheduler.proto  | SchedulerService  | CreateJob, UpdateJob, PauseJob, ResumeJob, DeleteJob, GetJob                     |
| webhook.proto    | WebhookService    | RegisterWebhook, UpdateWebhook, DeleteWebhook, GetWebhook, ListWebhooks           |
| reporting.proto  | ReportingService  | GenerateLedgerReport, GenerateStatement, GetReport                                |

### Generating protobuf stubs

Stubs are generated automatically during the build:

```bash
./gradlew :api-contracts:generateProto
```

Generated sources appear in `api-contracts/build/generated/source/proto/main/`.

### Adding api-contracts as a dependency

In any service's `build.gradle`:

```groovy
dependencies {
    implementation project(':api-contracts')
}
```

---

## Kubernetes Deployment

The `k8s/` directory uses Kustomize overlays for environment-specific configuration.

### Directory structure

```
k8s/
├── base/                    # Base manifests (all services)
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── kustomization.yaml
│   └── {service-name}/
│       ├── deployment.yaml
│       └── service.yaml
└── overlays/
    ├── dev/                 # 1 replica, lower resources
    ├── staging/             # 2 replicas, standard resources
    └── prod/                # 3 replicas, higher resources, HPA, PDB
```

### Deploy to dev

```bash
kubectl apply -k k8s/overlays/dev
```

### Deploy to staging

```bash
kubectl apply -k k8s/overlays/staging
```

### Deploy to production

```bash
kubectl apply -k k8s/overlays/prod
```

### Verify deployment

```bash
kubectl get pods -n ledger-platform-prod
kubectl get hpa -n ledger-platform-prod
kubectl get pdb -n ledger-platform-prod
```

### Secrets management

Each service expects a Kubernetes Secret with database credentials. Create them before deploying:

```bash
kubectl create secret generic account-service-secret \
  --from-literal=db-url=jdbc:postgresql://postgres:5432/account_db \
  --from-literal=db-username=ledger \
  --from-literal=db-password=<your-secure-password> \
  -n ledger-platform-prod
```

Repeat for each service that requires DB access.

---

## Environment Variables Reference

| Variable                      | Description                                              | Example                                      |
|-------------------------------|----------------------------------------------------------|----------------------------------------------|
| `SPRING_PROFILES_ACTIVE`      | Active Spring profiles                                   | `dev`, `staging`, `prod`, `k8s`              |
| `DB_URL`                      | JDBC connection string                                   | `jdbc:postgresql://postgres:5432/account_db` |
| `DB_USERNAME`                 | Database username                                        | `ledger`                                     |
| `DB_PASSWORD`                 | Database password                                        | `ledger_secret`                              |
| `KAFKA_BOOTSTRAP_SERVERS`     | Kafka broker addresses                                   | `kafka:29092`                                |
| `AWS_ENDPOINT`                | AWS / LocalStack endpoint                                | `http://localstack:4566`                     |
| `AWS_REGION`                  | AWS region                                               | `us-east-1`                                  |
| `ZIPKIN_BASE_URL`             | Zipkin tracing server URL                                | `http://zipkin:9411`                         |
| `SERVER_PORT`                 | HTTP server port                                         | `8081`                                       |
| `GRPC_SERVER_PORT`            | gRPC server port                                         | `9081`                                       |
| `LOG_LEVEL`                   | Root log level                                           | `INFO`, `WARN`, `DEBUG`                      |
| `ELASTICSEARCH_URIS`          | Elasticsearch connection URIs                            | `http://elasticsearch:9200`                  |
| `JWT_PRIVATE_KEY`             | RS256 signing private key, PEM (iam-service; ephemeral key generated if blank in dev) | `/certs/private.pem` |
| `JWT_PUBLIC_KEY`              | RS256 verification public key, PEM (iam-service)         | `/certs/public.pem`                          |
| `JWT_ISSUER`                  | Issuer (`iss`) iam-service stamps on tokens — its externally reachable base URL | `https://auth.licensee.com`        |
| `JWT_EXPIRY_SECONDS`          | Access token TTL (iam-service)                           | `900` (15 min)                               |
| `AUTH_ISSUER_JWKS_URL`        | Trusted issuer's JWKS the **gateway** validates tokens against. Defaults to the bundled iam-service; point at the licensee IdP to federate | `https://auth.licensee.com/.well-known/jwks.json` |
| `LICENSE_FILE_PATH`           | Path to the signed finCore license file                  | `/etc/fincore/license.lic`                   |
| `LICENSE_VERIFY_KEY`          | Public key used to verify the license signature          | `/etc/fincore/license-pub.pem`               |

---

## Infrastructure Services (Local)

| Service       | URL                        | Credentials                    |
|---------------|----------------------------|--------------------------------|
| PostgreSQL    | `localhost:5432`           | `ledger` / `ledger_secret`     |
| Kafka         | `localhost:9092`           | No auth (local)                |
| LocalStack    | `http://localhost:4566`    | Any key/secret (fake)          |
| Elasticsearch | `http://localhost:9200`    | No auth (local)                |
| Zipkin UI     | `http://localhost:9411`    | —                              |
| Kafka UI      | `http://localhost:8888`    | —                              |

---

## Contributing

1. Create a feature branch from `main`.
2. Add or modify the relevant service module.
3. Write unit and integration tests (Testcontainers).
4. Update the proto files if the API contract changes and run `./gradlew :api-contracts:generateProto`.
5. Submit a pull request — CI must pass before merge.

---

## License

Copyright (c) 2024 Harrified Tech. All rights reserved.
