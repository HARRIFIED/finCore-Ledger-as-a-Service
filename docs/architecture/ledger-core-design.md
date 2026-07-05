# ledger-core Design

The double-entry, event-sourced heart of finCore. Owns the journal, ledger accounts, and
balances. Satisfies **FR-2, FR-3, FR-7, FR-16, FR-17, FR-18, FR-19, FR-20** and underpins
transfers/holds/FX/scheduling/reporting.

Status: **draft.** Records the model and flags the sub-decisions still open (§10).
Balance model per [ADR-0005](decisions/0005-event-sourced-ledger-with-snapshots.md).

## 1. Vocabulary (avoid the "account" overload)

| Term | Meaning | Owned by |
|------|---------|----------|
| **Ledger account** | The unit balances are tracked for. Single tenant, single currency, a chart-of-accounts type. Customer wallets AND internal/GL accounts are both ledger accounts. | ledger-core |
| **Customer account** | The product-level thing a party opens (savings, wallet…) — metadata, lifecycle, ownership. References one ledger account. | account-service |
| **Journal entry** | One balanced transaction; an immutable event containing ≥2 postings. | ledger-core |
| **Posting** | One leg of an entry: a debit or credit of an amount to a ledger account. Append-only. | ledger-core |
| **Chart of Accounts (CoA)** | The per-tenant set/type-taxonomy of ledger accounts (asset/liability/equity/income/expense). | ledger-core (configured) |

## 2. Double-entry model

A **journal entry** contains ≥2 **postings**; each posting is a `DEBIT` or `CREDIT` of a
non-negative amount to a ledger account. Core invariant, enforced atomically at write:

```
for each currency in the entry:  Σ(debits) == Σ(credits)
```

A ledger account has a **normal side** from its CoA type. Its balance is normalized:

```
debit-normal  (ASSET, EXPENSE):      balance = Σdebits − Σcredits
credit-normal (LIABILITY, EQUITY, INCOME): balance = Σcredits − Σdebits
```

A customer wallet is typically a **LIABILITY** ledger account (the operator owes the
customer). Example — internal transfer of 500 from Alice to Bob (both liability wallets):

```
Journal entry (balanced):
  DEBIT  Alice-wallet   500   (↓ what we owe Alice)
  CREDIT Bob-wallet     500   (↑ what we owe Bob)
```

## 3. Event sourcing & balances

Postings are the source of truth. A ledger account's balance is a **fold** over its
postings; **per-account snapshots** make reads fast:

```
balance(account, T) = snapshot(account, ≤T) ⊕ fold(postings after snapshot, ≤T)
```

Snapshots are written every N postings (and/or on a timer) as
`{account, as_of_sequence, debits_total, credits_total, as_of_time}`.

### Balance types (FR-3)

| Balance | Definition |
|---------|------------|
| **Posted** (booked) | fold of `POSTED` postings effective as of the query time |
| **Held** | sum of active holds on the account (§6) |
| **Pending** | value of entries in `PENDING` (awaiting external settlement, §5/§FR-22) |
| **Available** | `posted − held` (pending inbound is excluded until settled) |

### Point-in-time & value dating (FR-20)

Every entry carries **`booking_time`** (when recorded, system clock) and **`value_date`**
(when it takes economic effect). Point-in-time balance folds postings with
`value_date ≤ T`. Snapshots are indexed by booking sequence, so a **backdated** entry
written after a snapshot must invalidate/rebuild snapshots whose window it falls into — a
tractable but real concern (see §10 Q2).

## 4. Money & currency (FR-19, FR-9)

- Amounts are **integer minor units** (`long`/`BigInteger`), never floating point. Each
  currency has a **scale** (JPY 0, USD 2, BHD 3, …); the currency's scale is authoritative.
- A ledger account is **single-currency**. There is no mixed-currency account.
- **Multicurrency entries** balance **per currency** (the §2 invariant is per-currency), so
  a single entry can move USD legs and NGN legs as long as each currency nets to zero.

### FX (FR-8) via position accounts

A cross-currency transfer is **not** one currency crossing accounts. It posts through
internal **FX position** accounts at the applied rate, so each currency still balances:

```
Convert Alice's 100 USD → NGN at 1600:
  DEBIT  Alice-USD-wallet     100_00        CREDIT FX-position-USD   100_00
  DEBIT  FX-position-NGN   160_000_00        CREDIT Bob-NGN-wallet 160_000_00
  (USD legs net to 0; NGN legs net to 0; the FX position accounts carry the conversion)
```

Rate source/spread and whether the two sub-entries are one journal entry or two linked
entries → §10 Q3.

## 5. Entry lifecycle, idempotency, reversals

- **Statuses:** `POSTED` (final, affects posted balance), `PENDING` (recorded, awaiting
  external settlement — affects *pending*, not *posted*; §FR-22), later `SETTLED`→`POSTED`
  or `FAILED`→reversed.
- **Idempotency (FR-16):** every `PostJournalEntry` carries a client **idempotency key**,
  unique per tenant. A duplicate key returns the original entry — no new postings. Enforced
  by a unique constraint + read-back.
- **Reversal (FR-7):** creates a **new** entry with debit/credit swapped, `reversal_of =
  original.id`. The original is never mutated; "reversed" is derived from the existence of a
  reversal. Net effect is zero.

## 6. Holds (FR-6, FR-21)

Recommended: holds are **first-class in ledger-core** as append-only hold events
(`PLACED` → `CAPTURED`(full/partial) → `RELEASED`/`EXPIRED`) folded into the account's
**held** component, so `available = posted − held` is computed in one place (ledger-core
stays the balance authority). **hold-service** owns lifecycle/policy (expiry timers,
capture rules, card-network semantics) and calls ledger-core to record hold events. On
capture, the held amount converts into a real posting; on release/expire, held is freed
with no posting. (Alternative: a separate per-account "held" ledger account — §10 Q4.)

## 7. Atomicity & concurrency (NFR-1, NFR-2)

- A journal entry + all its postings are written in **one `ledger_core_db` transaction**.
  Because every leg lives in ledger-core's own database, cross-account atomicity is native —
  no distributed transaction. **This is why the journal is a single service.**
- The balance invariant (§2) is checked before commit; a non-balancing entry is rejected.
- Snapshots use optimistic concurrency (`as_of_sequence` + version); a stale snapshot only
  costs a longer fold, never incorrectness (postings remain the truth).
- Events to the rest of the platform are published via a **transactional outbox** (FR-25):
  the outbox row is written in the same transaction as the entry, then relayed to Kafka.

## 8. Data model (ledger_core_db) — sketch

```
ledger_account(id, tenant_id, coa_type, normal_side, currency, reference, status,
               created_at, version)
journal_entry(id, tenant_id, idempotency_key, status, booking_time, value_date,
              reversal_of, correlation_id, source_type, metadata, created_at)
  └─ UNIQUE(tenant_id, idempotency_key)
posting(id, entry_id, tenant_id, ledger_account_id, direction, amount_minor, currency,
        account_sequence)                      -- append-only; indexed by (account, sequence)
hold(id, tenant_id, ledger_account_id, amount_minor, currency, status, expires_at,
     captured_minor, created_at)               -- lifecycle events folded into 'held'
balance_snapshot(ledger_account_id, as_of_sequence, debits_total, credits_total,
                 as_of_time, version)
outbox(id, tenant_id, aggregate_id, event_type, payload, created_at, published_at)
```

## 9. Public API (gRPC — aligns with `ledger.proto`)

| RPC | Purpose |
|-----|---------|
| `PostJournalEntry(idempotencyKey, valueDate, postings[], metadata)` | Append a balanced entry (or return the existing one for the key). |
| `GetBalance(ledgerAccountId)` | Current posted / available / held / pending. |
| `GetBalanceAtPointInTime(ledgerAccountId, asOf)` | Value-dated point-in-time balance. |
| `GetLedgerEntries(ledgerAccountId, range, page)` | History for statements/reports (FR-4/FR-15). |

Account opening: account-service calls ledger-core to create the customer's ledger account
and stores the returned `ledgerAccountId`.

## 10. Open design questions

1. **Snapshot cadence** — every N postings, time-based, or hybrid? Tune for read latency
   vs. write overhead (NFR-6/7).
2. **Value-dated PIT vs. snapshots** — how backdated entries invalidate/rebuild affected
   snapshots (rebuild-forward vs. snapshot only on booking order + separate value-date index).
3. ~~**Cross-currency entry shape** / FX~~ — **Resolved:** one multi-currency journal entry via
   FX position accounts; internal rate table + quotes; configurable spread. See
   [fx-and-chart-of-accounts.md](fx-and-chart-of-accounts.md) and [ADR-0007](decisions/0007-fx-rates-and-conversion.md).
4. **Holds placement** — held-component folded in ledger-core (recommended §6) vs. a
   dedicated per-account "held" ledger account.
5. **Sequence scope** — per-account sequence (for folding) plus a global/per-tenant journal
   sequence (for audit ordering)? Confirm both.
6. ~~**CoA configuration**~~ — **Resolved:** template-seeded per-tenant CoA + standard internal
   accounts; products carry posting rules. See [fx-and-chart-of-accounts.md](fx-and-chart-of-accounts.md)
   and [ADR-0008](decisions/0008-chart-of-accounts-template-seeded.md).
