# Build Plan

**Design for all requirements; build incrementally in small bits** (per the v1-scope
decision — no reduced-scope release, just a sequence of shippable increments). Each phase is
independently valuable, tested, and traces to requirement IDs. Phases have dependencies but
adjacent ones can overlap.

Status legend: ✅ done · 🟡 partial · ⬜ not started.

## Current snapshot (what exists today)

- ✅ **api-gateway** — federated JWT validation, identity-header inject/strip, routing.
- ✅ **platform-security** — shared `CallerContext` + auto-config (downstream identity).
- 🟡 **iam-service** — optional bundled IdP: register/login/JWT/JWKS work; KYC still here
  (to move to party-service).
- 🟡 **account-service** — skeleton + `/me`; no CRUD/persistence yet.
- ✅ Infra: Postgres, Kafka, Zipkin, docker-compose; per-service `.env`; Boot 4 / Spring Cloud 2025.1.x.

## Phases

### Phase 0 — Platform foundations ✅ (core seams done)
Goal: the seams every service depends on.
- ✅ **Tenant seam** — `platform-tenancy` module: `TenantContext` + `TenantContextFilter`
  (from `X-Program-Id`) + Hibernate `CurrentTenantIdentifierResolver` + auto-config
  (ADR-0009). Unit-tested. Services opt in by adding the dep and annotating their tenant
  column `@TenantId`; escalation to schema/DB-per-tenant is a Hibernate-strategy swap.
- ✅ **Outbox** — `platform-outbox` module: `OutboxAppender` (JDBC, joins the caller's
  transaction, cross-tenant-safe) + polling `OutboxRelay` + a dedicated String-serializing
  KafkaTemplate + auto-config; reference `outbox.sql` shipped for per-service migration.
  Unit-tested. (FR-25)
- ✅ gateway, ✅ platform-security, ✅ observability baseline (tracing).
- ⬜ Config/product/CoA scaffolding — folds into Phase 3 (accounts + CoA), where it's used.
- _Build infra:_ ✅ added the JUnit Platform launcher to the root test classpath (Gradle 9
  no longer adds it automatically) — unblocks `test` tasks across all modules.

### Phase 1 — ledger-core (the spine) 🟡  → FR-2,3,7,16,17,19,20
The highest-value, highest-risk piece; build it first to de-risk. Small bits:
1. ✅ Ledger accounts (create, from CoA type/currency).
2. ✅ Post journal entry + **double-entry invariant** (per-currency balance) — unit-tested.
3. ✅ Balance read (posted/available/held/pending) — `Balances.signed()` (normalize by normal
   side, unit-tested), `PostingRepository.fold()` (native, explicit tenant filter),
   `GET /accounts/{id}/balance`. **Live-verified:** debit/credit math correct (credit-normal
   wallet debited 50000 → posted −50000, counterparty +50000) and cross-tenant read → 404.
4. ✅ **Snapshots** — global `posting.seq` (DB sequence, unmapped) + `balance_snapshot`
   table (V3); `SnapshotStore` (lag-safe advancing UPSERT — gap-free high-water mark) +
   scheduled `SnapshotWriter`; `getBalance` reads `snapshot ⊕ fold(seq > as_of_seq)`.
   **Live-verified:** writer advanced snapshots, and balances stayed exact with no snapshot,
   with snapshot (== full fold), and with snapshot + new delta.
5. ✅ **Point-in-time** by value_date — `PostingRepository.foldAsOf` (value_date <= asOf, full
   value-dated fold, not the booking-seq snapshots) + `getBalanceAsOf` +
   `GET /accounts/{id}/balance?asOf=YYYY-MM-DD`. Live-verified across value dates (inclusive
   boundary; pre-entry dates read 0). (FR-20)
6. ✅ **Idempotency** keys (per tenant). ✅ **Reversals** — `POST /entries/{id}/reverse` appends a
   swapped-leg entry (`reversal_of`, original immutable), idempotent, emits `ledger.entry.reversed`.
   Live-verified: nets to zero, idempotent replay, PIT before reversal preserves the original.
7. ✅ Emit `ledger.entry.posted` via outbox.

_Bit 1 delivered + verified (2026-07-10):_ `ledger-core-service` module + `LedgerAccount`/
`JournalEntry`/`Posting` entities (`@TenantId` seam), `DoubleEntryValidator`, `LedgerService`
(create account, post entry with invariant + idempotency + outbox), REST controller,
Flyway V1/V2 (+ `ledger_core_db`). Invariant unit-tested (6/6). **Live smoke test passed:**
booted clean (`ddl-auto: validate` accepted the schema), balanced post → `201`, unbalanced
→ `422`, idempotent replay → same entry, and DB showed `2 accounts / 1 entry / 2 postings /
1 outbox` all tagged `tenant_id` auto-set from `X-Program-Id`. (Fix along the way:
`platform-outbox` no longer hard-requires the consumer to expose an `ObjectMapper` bean.)

### Phase 2 — party-service (CIF) 🟡  → §2.2 (PTY-1..6)
Parallelizable with Phase 1. Party (INDIVIDUAL/ORG), unified verification (KYC/KYB) + tiers,
`PartyMembership` (+ authorization API), `PartyRelationship`. **Migrate KYC out of iam-service.**

_Bit 1 delivered + verified (2026-07-18):_ `party-service` module (`:8093`, `party_db`) +
`Party` aggregate (single-table, INDIVIDUAL + ORGANIZATION, `@TenantId`), create/get REST,
type↔profile validation, `party.created` outbox event, Flyway V1 (party) + V2 (outbox).
**Live smoke test passed:** created individual + organization parties, GET both, type/profile
mismatch → `422`, and DB showed `2` parties (`tenant_id` auto-set) + `2` `party.created` outbox
rows. Next bits: verification (KYC/KYB) + tiers, `PartyMembership` + authorization API,
`PartyRelationship`.

### Phase 3 — accounts + CoA/products ⬜  → FR-1,18,24  (needs 1 & 2)
account-service CRUD **keyed on `partyId`**; template-seeded CoA + standard internal accounts
(ADR-0008); product definitions with posting rules; open-account creates a ledger account.
Replaces the `ownerId = X-Subject` placeholder.

### Phase 4 — internal money movement ⬜  → FR-5(internal),6,7,21  (needs 1 & 3)
transfer-service (internal transfers), hold-service (place/capture/release/expire → `held`),
reversal flows.

### Phase 5 — events & integration ⬜  → FR-12,13,14,25  (needs outbox + emitters)
Harden outbox→Kafka; audit-service (immutable trail), notification-service, webhook-service
(delivery + retry). Reporting read models begin here.

### Phase 6 — multicurrency & FX ⬜  → FR-8,9  (needs 1,3)
fx-service (rate table, quotes, spread — ADR-0007); multicurrency accounts; cross-currency
transfer as one multi-currency entry via FX position accounts.

### Phase 7 — external transfers & settlement ⬜  → FR-5(external),22  (needs 4)
Outbound **rail-adapter port** + simulated rail; PENDING→SETTLED/FAILED via async callbacks;
suspense/clearing accounts; reconciliation.

### Phase 8 — scheduled ops & batch ⬜  → FR-10,11,23,26  (needs 1,3)
scheduler-service (interest accrual/posting, fees, rewards; day-count/tiers); batch posting
(payroll); limits/controls enforcement.

### Phase 9 — reporting & statements ⬜  → FR-4,15  (needs 5)
reporting-service: statements (opening/closing/running balance), GL reports, exports (PDF/CSV/JSON).

### Phase 10 — licensing & packaging ⬜  (deferred subsystem)
License issuance service + in-engine signed-license enforcement (capacity/modules/expiry,
air-gapped); signed images + Helm; licensee-run upgrade/migration tooling.

## Cross-cutting: performance hardening  → NFR-6 (≈5,000 TPS, p99 < 100 ms)
Applied progressively once the posting path exists (after Phases 1 & 4), driven by load tests:
- **Shard hot internal/GL accounts** (settlement, FX position, fee income) into buckets to
  spread write contention — every transaction touches these, so they are the hotspot.
- Tuned / **async snapshotting**; posting-table **partitioning** (by account/tenant).
- Event-sourced append already avoids balance-row locking — lean into it (no update-in-place).

## Sequencing rationale

ledger-core first (spine, de-risks the hardest invariant early); party in parallel; accounts
once both exist; then money movement; events/integration; then multicurrency, external rails,
scheduling, reporting; licensing/packaging last. Each phase ships in small, tested increments.
