# FX & Chart of Accounts Design

How multicurrency conversion and the per-tenant Chart of Accounts work. Satisfies
**FR-8, FR-9, FR-15, FR-18, FR-24** and builds on the ledger model
([ledger-core-design.md](ledger-core-design.md)). Decisions:
[ADR-0007](decisions/0007-fx-rates-and-conversion.md) (FX),
[ADR-0008](decisions/0008-chart-of-accounts-template-seeded.md) (CoA).

Status: **draft.**

## 1. FX (fx-service)

### 1.1 Rate table

`fx-service` holds rates the **licensee feeds** (API or scheduled import). No external
runtime feed → works air-gapped.

```
fx_rate(id, tenant_id, base_ccy, quote_ccy, mid_rate, scale, source, effective_from, created_at)
  └─ current rate = latest effective_from ≤ now for (tenant, base, quote)
```

### 1.2 Quotes (short-lived locks)

```
fx_quote(id, tenant_id, base_ccy, quote_ccy, applied_rate, amount, expires_at, status)
  status: OPEN → CONSUMED | EXPIRED
```

Client flow: `GetQuote` → (locked rate, `quoteId`, TTL) → `ConvertCurrency(quoteId, …)`.
Unquoted conversions use the current table rate. Every conversion **captures the applied
rate + quoteId** on the resulting journal entry (audit / dispute).

### 1.3 Spread

Per-tenant / per-pair config: `applied_rate = mid ± spread`. The margin (mid vs applied) is
posted to the tenant's **FX-income** GL account. Spread 0 = pass-through.

```
fx_spread_config(tenant_id, base_ccy, quote_ccy, spread_bps)   -- bps = basis points
```

### 1.4 Conversion posting (one balanced-per-currency entry)

A cross-currency movement is a **single journal entry** through internal **FX position**
accounts, balanced within each currency; the spread is a separate leg to FX-income.

Worked example — Alice converts **100.00 USD → NGN**, mid `1600`, spread `50 bps` (0.5%),
so applied `1592`; customer receives `159,200.00 NGN`, margin `800.00 NGN`:

```
USD legs (net 0):
  DEBIT  Alice-USD-wallet        100.00        CREDIT FX-position-USD    100.00
NGN legs (net 0):
  DEBIT  FX-position-NGN      160,000.00        CREDIT Bob-NGN-wallet 159,200.00
                                                CREDIT FX-income-NGN      800.00
```

The FX position accounts carry the operator's FX exposure (revalued/managed by the licensee);
the margin lands in FX-income. All amounts are integer minor units (`FR-19`).

## 2. Chart of Accounts (per tenant)

### 2.1 Model

Each tenant's CoA is **seeded from a default template** at tenant setup, then customizable
([ADR-0008](decisions/0008-chart-of-accounts-template-seeded.md)). Every ledger account
belongs to a CoA entry that fixes its **type** (→ normal side) and rules.

```
coa_account(id, tenant_id, code, name, type, normal_side, is_internal, currency?, parent_code?)
  type ∈ {ASSET, LIABILITY, EQUITY, INCOME, EXPENSE}
```

### 2.2 Standard internal accounts (shipped in the template)

| Account | Type | Purpose |
|---------|------|---------|
| Settlement / Cash | ASSET | Real funds at the bank/rail; external transfers settle here |
| FX position (per currency) | ASSET/LIABILITY | Holds the operator's FX book per currency (§1.4) |
| Fee income | INCOME | Destination for fees (`FR-23`/`FR-26`) |
| Interest income / expense | INCOME / EXPENSE | Interest postings (`FR-10`/`FR-26`) |
| Suspense / clearing | LIABILITY | Holds in-flight external transfers pending settlement (`FR-22`) |
| Rounding | INCOME/EXPENSE | Absorbs rounding remainders (`FR-19`) |

Customer wallets are **LIABILITY** accounts created from product templates.

### 2.3 Product → posting rules (FR-24)

An account **product** carries the posting configuration so the engine (not code) knows
where money goes:

```
product(id, tenant_id, name, coa_type=LIABILITY, allowed_currencies[],
        fee_income_account, interest_income_account, interest_expense_account,
        interest_rules, fee_schedule, limits)   -- config, not code (NFR-13)
```

Opening a customer account instantiates a ledger account from the product; fee/interest
scheduled operations (`FR-10`) post between the customer account and these GL accounts.

## 3. Open / next

- Rate-feed import format and stale-rate policy (reject vs. warn when rates are old).
- Default quote TTL.
- Whether FX position is modeled ASSET or LIABILITY per currency (revaluation approach).
- CoA template versioning/migration as the standard set evolves.
