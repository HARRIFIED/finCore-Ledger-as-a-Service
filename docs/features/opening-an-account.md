# Opening an Account

A guided tour of what happens when a customer account is opened in Harrified Ledger (previously finCore) — the first flow
that spans multiple services. It shows how the **gateway**, **account-service**,
**party-service**, and **ledger-core** cooperate, and how a licensee's own backend plugs in.

> **Status:** working and verified (2026-07-21). Everything below reflects real requests
> against the running services.

## The one-paragraph version

A licensee's backend, acting for one of its logged-in users, asks Harrified Ledger (previously finCore) to open an account
for a **party** (a customer). The **gateway** authenticates the call and stamps the caller's
identity onto it. **account-service** checks with **party-service** that this user is allowed
to act for that party, asks **ledger-core** to create the double-entry ledger account that
will hold the money, saves the customer-facing account, and emits an `account.opened` event.

## Actors & concepts

- **Licensee backend** — the integration client (a fintech's own server). Harrified Ledger (previously finCore) is
  backend-to-backend; the end user never calls Harrified Ledger (previously finCore) directly.
- **Party** — the customer (the account owner), held in party-service (the CIF). A person or org.
- **Subject** — the login user id from the licensee's IdP (the JWT `sub`). Authorization maps
  a subject to the parties it may act for.
- **Tenant** (`X-Program-Id`) — the licensee's program/brand; everything is scoped to it.

## The request path

```
Licensee backend ─(Bearer JWT, mTLS)─▶ API Gateway :8080
    gateway: validate JWT vs JWKS · strip client headers · inject X-Subject / X-Program-Id / X-Scopes
        │  POST /api/v1/accounts { partyId, accountName, type, currency }
        ▼
   account-service :8081 (REST edge)              internal hops are gRPC (ADR-0010);
        │ 1. resolve tenant (X-Program-Id) + caller (X-Subject)   tenant rides in gRPC metadata
        ├─gRPC PartyService.CheckAccess { partyId, subject } ─────▶ party-service  :9093
        │      ◀── { allowed, roles }         (must include OWNER or ADMIN)
        ├─gRPC LedgerService.CreateLedgerAccount { reference, coaType=LIABILITY, currency } ─▶ ledger-core :9082
        │      ◀── { id: ledgerAccountId }
        │ 4. save Account { partyId, ledgerAccountId, tenant } ; status ACTIVE
        │ 5. append account.opened to the outbox (same transaction)  ⇢ Kafka
        ▼
   201 Created { id, accountNumber, partyId, ledgerAccountId, status … }
```

> **Transport:** the gateway edge is REST/JSON; every service-to-service hop is **gRPC**
> ([ADR-0010](../architecture/decisions/0010-grpc-for-internal-service-calls.md)). The tenant
> travels as `x-program-id` gRPC **metadata** (not a header) — a shared interceptor in
> `platform-tenancy` injects it on the client and reads it back into `TenantContext` on the
> server, so callers never wire it up by hand.

## Step by step — who does what

### 1. Authentication (gateway)
The licensee backend calls the gateway with a Bearer token from **its own IdP** (federation —
[ADR-0002](../architecture/decisions/0002-federated-authentication.md)). The gateway validates
the signature against the configured JWKS, **strips** any client-supplied `X-Subject` /
`X-Program-Id` / `X-Scopes` (anti-spoofing), and **injects** them from the verified claims.
Downstream services never see or validate the token — only the trusted headers. Full detail:
[IAM ↔ API Gateway flow](../security/iam-api-gateway-flow.md).

### 2. Tenant & caller context (account-service)
`platform-tenancy` reads `X-Program-Id` into a request-scoped `TenantContext` (so Hibernate
scopes every query/write by tenant — [ADR-0009](../architecture/decisions/0009-tenancy-isolation-row-level.md)).
`platform-security` resolves `X-Subject` into a `CallerContext` for the controller. A request
without `X-Subject` is rejected `401` (it didn't come through the gateway).

### 3. Authorization (account-service → party-service, gRPC)
account-service asks party-service **"may this subject act for this party?"** over gRPC
(`PartyService.CheckAccess { partyId, subject }` — tenant in metadata). party-service
returns `{ allowed, roles }` from the `subject → party` membership mapping
([ADR-0006](../architecture/decisions/0006-party-service-and-authorization.md)). account-service
**enforces** the policy: opening an account requires an **OWNER or ADMIN** role. No membership
→ `403`; only VIEWER → `403`.

### 4. The ledger account (account-service → ledger-core, gRPC)
account-service asks ledger-core to create the **double-entry ledger account** that actually
holds the balance over gRPC (`LedgerService.CreateLedgerAccount { reference, coaType=LIABILITY,
currency }` — a customer wallet is money the operator owes the customer). ledger-core creates it
tenant-scoped (the tenant came in over gRPC metadata) and returns its id.

### 5. Persist & emit (account-service)
account-service saves the customer-facing `Account` — keyed on **`partyId`** (the owner) and
linked to **`ledgerAccountId`** — with a generated account number and `ACTIVE` status, then
appends an **`account.opened`** event to its outbox **in the same transaction**
([FR-25](../architecture/requirements.md)). The polling relay publishes it to Kafka for any
subscriber (notifications, the licensee's own systems, …).

## A concrete run

Set up a party and grant a user OWNER (normally the licensee provisions memberships up front):

```
# (via party-service, tenant header X-Program-Id: acme)
POST /api/v1/parties                      { type:ORGANIZATION, displayName:"Acme Ltd", organization:{…} }  -> { id: <partyId> }
POST /api/v1/parties/<partyId>/memberships { subject:"login-ada", role:"OWNER" }
```

Open the account as that user (the gateway would inject these headers from the token; shown explicitly):

```
POST http://…/api/v1/accounts
  X-Program-Id: acme
  X-Subject:    login-ada
  { "partyId":"<partyId>", "accountName":"Ada NGN Wallet", "type":"SAVINGS", "currency":"NGN" }

201 Created
  { "id":"5c51…", "accountNumber":"AC-A91DD1AFFAAF44", "status":"ACTIVE",
    "partyId":"60ef…", "ledgerAccountId":"f962…", "currency":"NGN" }
```

The `ledgerAccountId` is a real ledger account — its balance reads `0` from ledger-core:

```
GET /api/v1/ledger/accounts/f962…/balance   (X-Program-Id: acme)  -> { currency:"NGN", posted:0, available:0 }
```

Authorization is enforced:

```
open as "login-bob"    (no membership on the party) -> 403
open as "login-viewer" (VIEWER role, not OWNER/ADMIN) -> 403
```

## Which part of each service did the work

| Service | Role in this flow | Working parts |
|---------|-------------------|---------------|
| **api-gateway** | Authenticate & stamp identity | JWKS validation, `IdentityHeaderGlobalFilter` (strip + inject), routing |
| **platform-tenancy** | Tenant scoping (REST + gRPC) | `TenantContextFilter` (edge) + gRPC tenant interceptors (metadata) + Hibernate `@TenantId` resolver |
| **platform-security** | Caller identity | `CallerContext` argument resolver |
| **account-service** | Orchestrate & own the account | `AccountService` (authorize → create ledger acct → persist → event); `PartyClient`, `LedgerClient` (net.devh `@GrpcClient` stubs) |
| **party-service** | Authorization facts | `MembershipService.access` (subject → party → roles) |
| **ledger-core** | The money account | `LedgerService.createAccount` (CoA-typed, tenant-scoped) |
| **platform-outbox** | Reliable events | `OutboxAppender` (same-txn) + polling relay → Kafka |

## Integrating from an external system

A licensee's backend plugs in like this:

1. **Authenticate your user** with your own IdP; point Harrified Ledger (previously finCore)'s gateway at that IdP's JWKS
   (`AUTH_ISSUER_JWKS_URL`) — or use the bundled `iam-service`.
2. **Call the gateway**, backend-to-backend (mTLS + OAuth2 client-credentials), passing the
   user's Bearer token. The gateway does the rest; your services never handle Harrified Ledger (previously finCore) internals.
3. **Provision authorization** ahead of time: create the party (or reuse yours via `externalRef`)
   and grant the acting user a membership (`OWNER`/`ADMIN`) on it — that's how Harrified Ledger (previously finCore) knows the
   user may open accounts for that party.
4. **React to events**: subscribe to `account.opened` (Kafka) or register a webhook, to drive
   your own onboarding/notifications.

You never reach into Harrified Ledger (previously finCore)'s databases or internal services — you speak to the gateway and
consume events. That's the whole integration surface (see the
[federation flow](../security/iam-api-gateway-flow.md) and
[service interaction map](../architecture/service-interaction-map.md)).

## Failure modes

| Situation | Result |
|-----------|--------|
| No `X-Subject` (didn't pass the gateway) | `401` |
| Caller lacks OWNER/ADMIN on the party | `403` |
| Unknown/inactive party for the caller | `403` (no active membership) |
| Unsupported currency | `422` |
| ledger-core unavailable | the open fails atomically — no account row, no event |

## Related

- [Party/customer domain design](../architecture/party-domain-design.md) · [ADR-0006](../architecture/decisions/0006-party-service-and-authorization.md)
- [ledger-core design](../architecture/ledger-core-design.md) · [ADR-0005](../architecture/decisions/0005-event-sourced-ledger-with-snapshots.md)
- [Service interaction map](../architecture/service-interaction-map.md)


<!-- === 1. Party created (party-service REST) ===
{
    "id":  "b4ff3522-190f-4423-bb6b-38498462dba6",
    "type":  "INDIVIDUAL",
    "status":  "PROSPECT",
    "externalRef":  null,
    "displayName":  "Ada Lovelace",
    "individual":  {
                       "firstName":  "Ada",
                       "lastName":  "Lovelace",
                       "dateOfBirth":  null,
                       "nationality":  "GBR"
                   },
    "organization":  null
}

=== 2. OWNER membership granted ===
{
    "id":  "870611a3-5ca6-4ce8-9a8f-3fc48db83a53",
    "partyId":  "b4ff3522-190f-4423-bb6b-38498462dba6",
    "subject":  "user-owner-001",
    "role":  "OWNER",
    "status":  "ACTIVE"
}

=== 3. Account opened (authz + ledger account over gRPC) ===
{
    "id":  "f8b0157b-5a89-440b-8308-96bdcc5cb17c",
    "accountNumber":  "AC-BDDEB4623C7F42",
    "accountName":  "Ada Savings",
    "description":  null,
    "type":  "SAVINGS",
    "currency":  "NGN",
    "status":  "ACTIVE",
    "partyId":  "b4ff3522-190f-4423-bb6b-38498462dba6",
    "ledgerAccountId":  "e6bb7c14-2563-4ef7-9852-ad5c6ad909af",
    "parentAccountId":  null,
    "metadata":  null,
    "tags":  [

             ],
    "version":  0,
    "createdAt":  null,
    "updatedAt":  null
}

=== 4. Non-member rejected as expected ===
HTTP 403 Forbidden

--- SUMMARY ---
partyId        = b4ff3522-190f-4423-bb6b-38498462dba6
accountId      = f8b0157b-5a89-440b-8308-96bdcc5cb17c
ledgerAccountId= e6bb7c14-2563-4ef7-9852-ad5c6ad909af
account.partyId= b4ff3522-190f-4423-bb6b-38498462dba6 -->