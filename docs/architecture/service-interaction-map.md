# Service Interaction Map

How finCore's services call each other and communicate via events. Ties the domain designs
together (ledger-core, party, FX/CoA) and shows where each requirement's flow runs.

Status: **draft.**

## Legend

- **`──▶` sync** — request/response, gRPC (internal) fronted by REST at the gateway.
- **`⇢ async`** — event via Kafka, produced through the **transactional outbox** (`FR-25`).
- Everything downstream trusts gateway-injected identity headers; nothing re-validates tokens.

## 1. Request path (write)

```
End user → Licensee backend ─(mTLS + OAuth2)→ API Gateway
   gateway: validate JWT · strip+inject X-Subject / X-Program-Id / X-Scopes
        │
        ▼
   Domain service (account | transfer | hold | fx | scheduler)
        ├──▶ party-service   : authorize (membership on owning party?) + party/tier checks
        ├──▶ fx-service       : quote/rate (only for cross-currency)
        └──▶ ledger-core      : PostJournalEntry(idempotencyKey, postings…)   ← the one write of record
                 │ (same DB txn) append postings + outbox row
                 ⇢ Kafka: ledger.entry.posted
```

## 2. Who calls whom (sync)

| Caller | Callee | Purpose |
|--------|--------|---------|
| account-service | party-service | validate party exists/ACTIVE, KYC tier ok (open account) |
| account-service | ledger-core | create the customer's ledger account |
| transfer / hold / scheduler | party-service | authorize caller's membership + role on the owning party |
| transfer / scheduler | ledger-core | post journal entries (transfers, interest, fees, batch) |
| hold-service | ledger-core | record hold events (place/capture/release) into `held` |
| fx-service | ledger-core | post the multi-currency conversion entry |
| transfer-service | fx-service | get quote/rate for a cross-currency transfer |
| transfer-service | rail adapter | submit an external transfer (async settlement, §4) |

**ledger-core calls no one** — it is the leaf authority for balances, so a posting is one
local ACID transaction (no distributed transaction). This is the core invariant of the map.

## 3. Event / outbox topology (async)

Producers write an **outbox row in the same transaction** as their state change; a relay
publishes to Kafka. Indicative topics and consumers:

```
ledger-core   ⇢ ledger.entry.posted, ledger.entry.reversed
account       ⇢ account.opened, account.frozen, account.closed
transfer      ⇢ transfer.initiated, transfer.settled, transfer.failed
hold          ⇢ hold.placed, hold.captured, hold.released, hold.expired
party         ⇢ party.created, party.kyc.updated

consumers:
  audit-service        ← all topics        → immutable audit trail (FR-12)
  notification-service ← selected topics    → email/SMS/push (FR-13)
  webhook-service      ← selected topics    → merchant/client delivery + retry (FR-14)
  reporting-service    ← ledger.* , account.* → materialized read models (FR-4, FR-15)
  (licensee fraud/analytics) ← event stream  → external consumers (FR-13)
```

Events are **facts about committed state** (never commands), so consumers are decoupled and
independently replayable.

## 4. External transfer & settlement (FR-5 external, FR-22)

```
transfer-service ──▶ ledger-core : post PENDING entry (funds → suspense/clearing)
transfer-service ──▶ rail adapter : submit (simulated rail at v1; real rails later)
        … asynchronously …
rail adapter ⇢ settlement callback ──▶ transfer-service
   settled → ledger-core: move clearing → external (PENDING → POSTED)   ⇢ transfer.settled
   failed  → ledger-core: reverse the pending entry                      ⇢ transfer.failed
```

## 5. Cross-cutting

- **Tenant context** — `X-Program-Id` → a tenant-scoped repository layer in every service
  (ADR-0009). No cross-tenant access.
- **Idempotency** — client keys flow through domain services into `ledger-core`
  (`FR-16`); retries are safe end to end.
- **Authorization** — always resolved against `party-service` membership (ADR-0006), cached.
- **Observability** — one correlation id propagates across sync calls and onto emitted
  events (Zipkin/Micrometer).
