# finCore Requirements

Status: **draft, under review.** IDs are stable so design and code can trace to them.
Items marked _(proposed)_ are additions from core-banking practice — flagged for your
review, not yet locked.

## 1. Product context

finCore is a **licensed, self-hosted core ledger engine** (see [ADR-0001](decisions/0001-licensed-self-hosted-engine.md)).
A licensee (a fintech) runs its own instance and database and builds its product around
the engine, integrating the way a bank integrates with a core like Finacle. Authentication
is **federated** to the licensee's IdP ([ADR-0002](decisions/0002-federated-authentication.md)).

## 2. Tenancy & party model

### 2.1 Multi-tenancy (TEN)

finCore is **multi-tenant within a single licensee deployment.** A **tenant** is a
program / brand / business line / legal entity *of the licensee* (e.g. a fintech's
"consumer app" vs "SME product") — **not** a different licensee. One deployment = one
licensee; many tenants inside it. This reconciles "designed with multi-tenancy in mind"
with the licensed model (cross-licensee isolation is achieved by separate deployments).

- **TEN-1** Every party, account, and ledger entry belongs to exactly one tenant.
- **TEN-2** All queries and postings are tenant-scoped; no operation may cross tenants
  implicitly. The tenant is derived from the gateway-injected `X-Program-Id`, never the body.
- **TEN-3** _(proposed)_ Tenant is a first-class configured entity (products, currencies,
  chart of accounts, and limits are defined per tenant).
- **TEN-4** Isolation: **row-level `tenant_id`** with an escalation seam to schema-/DB-per-tenant
  ([ADR-0009](decisions/0009-tenancy-isolation-row-level.md)).

### 2.2 Parties — retail and corporate/SME (PTY)

finCore owns a **Customer/Party master** (a CIF), distinct from login identity
([ADR-0004](decisions/0004-separate-customer-party-master.md)). Modeled after Finacle so
user management and auth reflect both retail and corporate/SME customers.

- **PTY-1** A **Party** is the legal entity that owns accounts. Party types: `INDIVIDUAL`
  (retail) and `ORGANIZATION` (corporate/SME), extensible.
- **PTY-2** Accounts are owned by a **party** (`partyId`), not by a login user.
- **PTY-3** KYC (individuals) / KYB (organizations) profiles attach to a party, with
  verification tiers/levels that can gate account operations.
- **PTY-4** A party may have **many authorized users**, and a user may act for **many
  parties** (e.g. a signatory on several companies). Corporate parties support roles /
  mandates (who can initiate vs approve vs view).
- **PTY-5** A party may exist with **no login** (serviced by staff); staff users may act
  **on behalf of** a party. Login users come from the federated IdP (`X-Subject`); finCore
  stores the `user → party` mapping and the party's own data.
- **PTY-6** _(proposed)_ Authorization is scope/role-based (`X-Scopes`), evaluated against
  the party relationship (e.g. "can initiate transfers on party X").

## 3. Functional requirements

### 3.1 As specified

| ID | Requirement |
|----|-------------|
| **FR-1** | **Account management** — create and manage accounts (open, update, freeze, close), with lifecycle states. |
| **FR-2** | **Double-entry posting** — record debits and credits; every transaction is balanced. |
| **FR-3** | **Balance computation** — maintain **available**, **held**, and **pending** (awaiting settlement) balances; support **point-in-time** balance queries. |
| **FR-4** | **History & statements** — transaction history and statement/report generation. |
| **FR-5** | **Transfers** — internal (account→account) and external (to outside rails). |
| **FR-6** | **Holds / reserves** — reserve funds for card-network payments, escrow, etc. |
| **FR-7** | **Transaction reversals** — reverse a prior transaction. |
| **FR-8** | **FX conversion** — convert between currencies. |
| **FR-9** | **Multicurrency** — accounts in different currencies; cross-currency transfers. |
| **FR-10** | **Scheduled ledger operations** — interest payments, monthly fees, savings rewards, etc. |
| **FR-11** | **Batch posting** — bulk transactions (e.g. company payroll). |
| **FR-12** | **Immutable audit trail** — tamper-evident record of all changes. |
| **FR-13** | **Real-time event streaming** — emit events for notifications, fraud systems, etc. |
| **FR-14** | **Webhooks** — deliver events to merchants / clients / external APIs. |
| **FR-15** | **General ledger reporting** — GL reports. |

### 3.2 Additional _(proposed — from core-banking practice)_

| ID | Requirement | Why it matters |
|----|-------------|----------------|
| **FR-16** | **Idempotent operations** — every posting/transfer/hold accepts a client **idempotency key**; retries never double-post. | The single most important safety property of a ledger under network retries. |
| **FR-17** | **Append-only ledger** — journal entries are immutable; corrections are made by **reversing entries**, never update/delete. | Makes FR-12 real at the ledger level, not just an audit log. |
| **FR-18** | **Chart of Accounts** — a configurable CoA (asset/liability/equity/income/expense) per tenant; every posting hits GL accounts. | Required for FR-15 and for the engine to be configured (not forked) per licensee. |
| **FR-19** | **Money representation** — integer **minor units** with per-currency scale; explicit rounding rules; no floating point. | Correctness; avoids rounding drift. |
| **FR-20** | **Value dating** — entries carry an **effective (value) date** distinct from booking date; supports backdating and accurate point-in-time (FR-3) and interest (FR-10). | Standard core-banking semantics. |
| **FR-21** | **Holds lifecycle** — place → (partial) capture → release/expire, with auto-expiry; `available = posted − active holds`. | Makes FR-6 precise. |
| **FR-22** | **Settlement/clearing states for external transfers** — pending → settled/failed, suspense/clearing accounts, reconciliation hooks. | External money movement (FR-5) is asynchronous and must reconcile. |
| **FR-23** | **Limits & controls** — overdraft rules, minimum balance, per-account/tenant transaction and velocity limits, freeze/block states. | Risk controls expected of a core. |
| **FR-24** | **Product definitions** — account **products** (savings, current, wallet, …) as configuration carrying currency, interest, fee, and limit rules. | Config-not-fork; drives FR-1/FR-10/FR-23. |
| **FR-25** | **Reliable event publishing** — events (FR-13/FR-14) published via a **transactional outbox** so they are never lost or duplicated relative to committed ledger state. | Consistency between ledger and downstream. |
| **FR-26** | **Interest & fee engine** — accrual vs. posting, day-count conventions, tiered rates/fee schedules driving FR-10. | Needed to implement scheduled ops correctly. |

## 4. Non-functional requirements

| ID | Requirement |
|----|-------------|
| **NFR-1 Correctness** | A posting is **atomic** across all its legs and leaves the ledger balanced (Σdebits = Σcredits per currency). No partial postings. |
| **NFR-2 Consistency model** | Strong consistency **within** the ledger core (a transaction); well-defined eventual consistency **across** services (via events/outbox). |
| **NFR-3 Idempotency** | All state-changing APIs are idempotent under retry (see FR-16). |
| **NFR-4 Auditability** | Every state change is attributable (who/what/when, correlation id) and immutable (FR-12/FR-17). |
| **NFR-5 Availability** | Target HA for the posting path; graceful degradation of non-critical paths (reporting, notifications). |
| **NFR-6 Performance** | Posting path target: **≈5,000 TPS sustained, p99 < 100 ms**. Implies sharding hot internal/GL accounts, tuned/async snapshots, and posting-table partitioning (see build plan). |
| **NFR-7 Scalability** | Horizontal scale of stateless services; a balance/posting model that scales with account/txn volume. |
| **NFR-8 Security** | Federated auth; downstream services trust gateway headers only; PII encrypted at rest; least-privilege DB access; secrets externalized. |
| **NFR-9 Observability** | Distributed tracing, metrics, structured logs across all services (already: Zipkin/Micrometer). |
| **NFR-10 Data residency** | All data stays in the licensee's infrastructure; finCore (vendor) never hosts or accesses it. |
| **NFR-11 Compliance & retention** | Configurable retention; audit records retained per regulatory requirement. |
| **NFR-12 Deployability** | Signed images + Helm; runs air-gapped; licensee-run DB migrations on upgrade. |
| **NFR-13 Extensibility** | Behaviour (products, fees, CoA, limits) is configuration, not code changes (config-not-fork). |
| **NFR-14 API stability** | Published gRPC/OpenAPI contracts are backward-compatible within a major version. |

## 5. Non-goals (for now)

- finCore is **not** a payments switch / scheme processor, a card issuer-processor, or a
  KYC/AML provider — it **integrates** with these (external transfers, KYC data in, events out).
- Not an end-user UI; it's a backend engine consumed by the licensee's systems.
- Not a cross-licensee SaaS (isolation is by separate deployment — see §2.1).

## 6. Traceability (services ↔ requirements)

Indicative mapping to the current service catalog (to be firmed up in the service design):
`ledger-core` → FR-2,3,17,18,19,20; `account` → FR-1,24; `transaction` → FR-2,7,11,16;
`transfer` → FR-5,22; `hold` → FR-6,21; `fx` → FR-8,9; `scheduler` → FR-10,26;
`audit` → FR-12; `notification`/`webhook` → FR-13,14,25; `reporting` → FR-4,15; `iam` +
Party master → §2.

## 7. Open questions (drive the next design steps)

1. ~~**Balance model**~~ — **Resolved:** event-sourced with periodic snapshots ([ADR-0005](decisions/0005-event-sourced-ledger-with-snapshots.md)).
2. ~~**Tenancy isolation** (TEN-4)~~ — **Resolved:** row-level `tenant_id` with an escalation seam ([ADR-0009](decisions/0009-tenancy-isolation-row-level.md)).
3. ~~**Party master placement**~~ — **Resolved:** dedicated `party-service` ([ADR-0006](decisions/0006-party-service-and-authorization.md)).
4. ~~**`user → party` mapping ownership**~~ — **Resolved:** maintained in finCore (`PartyMembership` in party-service), not an IdP claim ([ADR-0006](decisions/0006-party-service-and-authorization.md)).
5. ~~**External transfer rails**~~ — **Resolved:** pluggable outbound-rail port with async
   settlement callbacks; a **simulated rail** at v1, real rails (ISO 20022, provider APIs) as adapters later.
6. ~~**FX mechanics**~~ — **Resolved:** internal rate table + short-lived quotes, configurable
   spread, one multi-currency entry via FX position accounts ([ADR-0007](decisions/0007-fx-rates-and-conversion.md)).
7. ~~**Performance targets** (NFR-6)~~ — **Resolved:** ≈**5,000 TPS** sustained on the posting
   path, **p99 < 100 ms** (see NFR-6 and the build plan's hardening notes).
8. ~~**Scope at v1**~~ — **Resolved:** **design for all** requirements; **build incrementally**
   in small phases (see [build-plan.md](build-plan.md)), not a reduced-scope v1.
