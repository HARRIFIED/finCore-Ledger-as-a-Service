# SPEC-001 — IAM Service

**Service:** `iam-service`
**Port:** HTTP `8092` · gRPC `9082`
**Database:** `identity_db` (PostgreSQL 16)
**Status:** In development

---

## 1. Purpose

The IAM service is the authentication and identity foundation for the entire finCore platform. It is the **only** service that issues JWTs. Every other service trusts the headers the API Gateway injects after validating those JWTs.

Responsibilities:
- Tenant onboarding (create tenant + first admin user)
- User registration and authentication
- JWT issuance (RS256, access + refresh tokens)
- JWKS public key exposure (consumed by the API Gateway)
- User and tenant profile management
- KYC data collection and document tracking

---

## 2. Domain Model

### 2.1 Tenant

Represents a business or fintech product plugged into finCore. Every user, account, and transaction is scoped to a tenant.

| Field       | Type          | Notes                                      |
|-------------|---------------|--------------------------------------------|
| id          | UUID          | PK                                         |
| name        | VARCHAR(255)  | Display name                               |
| slug        | VARCHAR(100)  | Unique, URL-safe identifier e.g. `acme-pay`|
| status      | TenantStatus  | ACTIVE \| SUSPENDED                        |
| plan        | TenantPlan    | STARTER \| GROWTH \| ENTERPRISE            |
| version     | BIGINT        | Optimistic lock                            |
| created_at  | TIMESTAMPTZ   |                                            |
| updated_at  | TIMESTAMPTZ   |                                            |

### 2.2 User

A person (customer or admin) within a tenant. Email uniqueness is **per tenant** — the same email may belong to users in different tenants.

| Field                | Type         | Notes                                            |
|----------------------|--------------|--------------------------------------------------|
| id                   | UUID         | PK                                               |
| tenant_id            | UUID         | FK → tenants                                     |
| email                | VARCHAR(255) | Unique per tenant                                |
| password_hash        | VARCHAR(255) | BCrypt                                           |
| first_name           | VARCHAR(100) |                                                  |
| last_name            | VARCHAR(100) |                                                  |
| phone_number         | VARCHAR(30)  | Nullable                                         |
| address_street       | VARCHAR(255) | Embedded address — nullable                      |
| address_city         | VARCHAR(100) |                                                  |
| address_state        | VARCHAR(100) |                                                  |
| address_country      | VARCHAR(100) |                                                  |
| address_postal_code  | VARCHAR(20)  |                                                  |
| role                 | UserRole     | CUSTOMER \| TENANT_ADMIN \| SUPER_ADMIN          |
| kyc_status           | KycStatus    | UNVERIFIED \| PENDING \| VERIFIED \| REJECTED    |
| status               | UserStatus   | ACTIVE \| INACTIVE \| SUSPENDED                  |
| version              | BIGINT       | Optimistic lock                                  |
| created_at           | TIMESTAMPTZ  |                                                  |
| updated_at           | TIMESTAMPTZ  |                                                  |

### 2.3 KycProfile

One-to-one with User. Holds structured KYC data and the tenant-extensible `metadata` JSONB field.

| Field         | Type        | Notes                                                        |
|---------------|-------------|--------------------------------------------------------------|
| id            | UUID        | PK                                                           |
| user_id       | UUID        | FK → users, UNIQUE (1-to-1)                                  |
| tenant_id     | UUID        | FK → tenants                                                 |
| date_of_birth | DATE        | Nullable                                                     |
| bvn           | CHAR(11)    | Bank Verification Number — encrypt at rest before prod       |
| nin           | CHAR(11)    | National ID Number — encrypt at rest before prod             |
| kyc_level     | KycLevel    | NONE → BASIC → TIER_1 → TIER_2 → FULL                       |
| verified_at   | TIMESTAMPTZ | Set when kyc_level reaches FULL                              |
| metadata      | JSONB       | Tenant-specific fields e.g. `{ "cac_number": "RC-123456" }` |
| version       | BIGINT      | Optimistic lock                                              |
| created_at    | TIMESTAMPTZ |                                                              |
| updated_at    | TIMESTAMPTZ |                                                              |

**KYC tiers** (aligned with CBN tiered KYC requirements):

| Level  | Meaning                                      | Typical unlock              |
|--------|----------------------------------------------|-----------------------------|
| NONE   | Registration only                            | Very low transaction limits |
| BASIC  | Email + phone verified                       | Low limits                  |
| TIER_1 | BVN or NIN verified (soft KYC)               | Medium limits               |
| TIER_2 | Government-issued ID document approved       | Higher limits               |
| FULL   | All documents approved, address verified     | Full access                 |

### 2.4 KycDocument

Uploaded KYC documents. Stores a file reference (S3 key), not the file content.

| Field            | Type              | Notes                                                       |
|------------------|-------------------|-------------------------------------------------------------|
| id               | UUID              | PK                                                          |
| user_id          | UUID              | FK → users                                                  |
| tenant_id        | UUID              | FK → tenants                                                |
| type             | KycDocumentType   | See enum below                                              |
| file_key         | VARCHAR(512)      | S3 object key — never a URL; generate presigned URLs on demand |
| file_name        | VARCHAR(255)      | Original filename from upload                               |
| mime_type        | VARCHAR(100)      | e.g. `image/jpeg`, `application/pdf`                        |
| status           | KycDocumentStatus | PENDING \| UNDER_REVIEW \| APPROVED \| REJECTED             |
| rejection_reason | TEXT              | Set when status = REJECTED                                  |
| reviewed_by      | UUID              | Admin user ID who reviewed                                  |
| reviewed_at      | TIMESTAMPTZ       |                                                             |
| metadata         | JSONB             | Extra per-document attributes or custom type label          |
| created_at       | TIMESTAMPTZ       |                                                             |

**KycDocumentType values:** `PASSPORT`, `NATIONAL_ID`, `DRIVERS_LICENSE`, `VOTERS_CARD`, `BVN_SLIP`, `NIN_SLIP`, `PROOF_OF_ADDRESS`, `UTILITY_BILL`, `BANK_STATEMENT`, `SELFIE`, `BUSINESS_REGISTRATION`, `TAX_IDENTIFICATION`, `OTHER`

> Use `type = OTHER` + `metadata.custom_type` for tenant-specific document types not yet in the enum.

### 2.5 RefreshToken

Opaque refresh tokens. Raw token is given to the client; only the SHA-256 hash is stored.

| Field      | Type        | Notes                              |
|------------|-------------|------------------------------------|
| id         | UUID        | PK                                 |
| user_id    | UUID        | FK → users ON DELETE CASCADE       |
| token_hash | CHAR(64)    | SHA-256 hex of the raw token       |
| expires_at | TIMESTAMPTZ |                                    |
| revoked    | BOOLEAN     | Set on logout or token rotation    |
| created_at | TIMESTAMPTZ |                                    |

---

## 3. Authentication Flow

### 3.1 Registration (POST /api/v1/auth/register)

Creates a new tenant and its first user (TENANT_ADMIN) in a single transaction.

```
1. Validate tenant slug is not already taken
2. Create Tenant
3. Create User (role = TENANT_ADMIN, BCrypt password hash)
4. Create KycProfile (level = NONE)
5. Issue access token + refresh token
6. Store SHA-256(refresh_token) in refresh_tokens table
7. Return TokenResponse
```

### 3.2 Login (POST /api/v1/auth/login)

```
1. Resolve tenant by slug
2. Find user by (email, tenant_id)
3. BCrypt verify password — always return "Invalid credentials" on failure (no user enumeration)
4. Check user.status = ACTIVE
5. Issue tokens, store refresh token hash
6. Return TokenResponse
```

### 3.3 Token Refresh (POST /api/v1/auth/refresh)

Implements **refresh token rotation** — the old token is revoked and a new one issued.

```
1. Hash the incoming refresh token (SHA-256)
2. Look up by hash — 401 if not found
3. Check not revoked and not expired
4. Load user
5. Revoke old refresh token
6. Issue new access token + new refresh token
7. Return TokenResponse
```

### 3.4 Logout (POST /api/v1/auth/logout)

```
1. Hash the refresh token
2. Revoke in DB (single device logout)
```

> Use `revokeAllByUserId` for "logout everywhere" — not exposed as an endpoint yet.

---

## 4. JWT Structure

Algorithm: **RS256** (RSA + SHA-256). The private key signs tokens; the public key is published via JWKS so the API Gateway can verify without calling iam-service.

```json
{
  "sub": "<userId>",
  "tenant_id": "<tenantId>",
  "email": "user@example.com",
  "role": "CUSTOMER",
  "iat": 1718000000,
  "exp": 1718000900
}
```

**Key management:**
- Dev: RSA-2048 key pair generated in memory on startup (`JWT_PRIVATE_KEY` env var is blank)
- Prod: Load from `JWT_PRIVATE_KEY` + `JWT_PUBLIC_KEY` PEM env vars (PKCS#8 format)

**TTLs:**
- Access token: `JWT_EXPIRY_SECONDS` (default 900s / 15 min)
- Refresh token: `JWT_REFRESH_EXPIRY_SECONDS` (default 604800s / 7 days)

---

## 5. API Endpoints

### Auth (public — no JWT required)

| Method | Path                            | Description                          |
|--------|---------------------------------|--------------------------------------|
| POST   | `/api/v1/auth/register`         | Create tenant + first admin user     |
| POST   | `/api/v1/auth/login`            | Authenticate, receive token pair     |
| POST   | `/api/v1/auth/refresh`          | Rotate refresh token, new access token |
| POST   | `/api/v1/auth/logout`           | Revoke refresh token                 |
| GET    | `/api/v1/.well-known/jwks.json` | JWKS public key (used by API Gateway)|

### Users (protected — requires valid JWT)

| Method | Path                  | Description                        |
|--------|-----------------------|------------------------------------|
| GET    | `/api/v1/users/me`    | Get current user's profile         |
| PATCH  | `/api/v1/users/me`    | Update profile (name, phone, address) |
| GET    | `/api/v1/users/{id}`  | Get user by ID (TENANT_ADMIN only) |
| GET    | `/api/v1/users`       | List users for tenant (TENANT_ADMIN)|

### KYC (protected)

| Method | Path                              | Description                        |
|--------|-----------------------------------|------------------------------------|
| GET    | `/api/v1/kyc/profile`             | Get current user's KYC profile     |
| PATCH  | `/api/v1/kyc/profile`             | Update BVN, NIN, DOB               |
| POST   | `/api/v1/kyc/documents`           | Upload a KYC document              |
| GET    | `/api/v1/kyc/documents`           | List user's KYC documents          |
| GET    | `/api/v1/kyc/documents/{id}/url`  | Get presigned download URL         |

---

## 6. gRPC Interface (internal only)

Defined in `api-contracts/src/main/proto/identity.proto`.

| RPC          | Request              | Response             | Caller          |
|--------------|----------------------|----------------------|-----------------|
| `GetUser`    | `GetUserRequest`     | `UserResponse`       | Any service     |
| `GetJwks`    | `GetJwksRequest`     | `GetJwksResponse`    | API Gateway     |
| `ListUsers`  | `ListUsersRequest`   | `ListUsersResponse`  | Admin tooling   |

---

## 7. Security Decisions

- **No user enumeration**: Login always returns "Invalid credentials" regardless of whether the email or password is wrong.
- **Refresh token rotation**: Every refresh issues a new refresh token and revokes the old one. A stolen token that has already been used will be detected.
- **Hash before store**: Raw refresh tokens are never persisted. Only `SHA-256(token)` is stored.
- **BVN/NIN encryption**: Currently stored as plain strings. A JPA `AttributeConverter` backed by AES-256 (via AWS KMS in prod) must be added before going to production.
- **JWKS over direct gRPC**: The API Gateway fetches the JWKS once (with caching) and validates tokens locally — no per-request call to iam-service.
- **Single role per user**: Users have one role. Multi-role support can be added by evolving the entity to a `Set<UserRole>` collection without breaking the token claim shape.

---

## 8. Infrastructure

### 8.1 Docker Compose Services

| Service       | Host Port(s)        | Notes                                      |
|---------------|---------------------|--------------------------------------------|
| postgres      | 5432                | Databases: `identity_db`, `account_db`, `finCore_platform` — seeded by `infrastructure/postgres/init.sql` |
| kafka         | 9092 (external)     | Internal broker: `kafka:29092`             |
| zookeeper     | 2181                |                                            |
| localstack    | 4566                | S3 bucket `fincore-kyc-documents` bootstrapped by `infrastructure/localstack/init-aws.sh` |
| zipkin        | 9411                | Trace UI                                   |
| kafka-ui      | 8888                |                                            |
| iam-service   | 8092 (HTTP), 9082 (gRPC) | gRPC port changed from 9092 → 9082 to avoid collision with Kafka external port |
| account-service | 8081 (HTTP), 9081 (gRPC) |                                      |

### 8.2 Dockerfile Strategy

Both service Dockerfiles use a two-stage build:
- **Builder**: `eclipse-temurin:21-jdk-noble` — full JDK for Gradle compilation; never deployed
- **Runtime**: `gcr.io/distroless/java21-debian12:nonroot` — no shell, no package manager, runs as uid 65532; minimal CVE surface

### 8.3 Key Files

| Path | Purpose |
|------|---------|
| `infrastructure/postgres/init.sql` | Creates `identity_db` and `account_db` on first postgres start |
| `infrastructure/localstack/init-aws.sh` | Creates `fincore-kyc-documents` S3 bucket and enables versioning |
| `.dockerignore` | Excludes `.git`, `build/`, `.env`, IDE files from Docker build context |

---

## 9. Flyway Migration Index

| Version | Description             |
|---------|-------------------------|
| V1      | Create tenants          |
| V2      | Create users            |
| V3      | Create refresh_tokens   |
| V4      | Create kyc_profiles     |
| V5      | Create kyc_documents    |
