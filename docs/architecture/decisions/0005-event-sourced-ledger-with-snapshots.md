# 0005. Event-sourced ledger with periodic snapshots

- **Status:** Accepted
- **Date:** 2026-07-05
- **Deciders:** Harrified Tech

## Context

`ledger-core` must provide correct double-entry balances, an append-only immutable record,
and point-in-time queries (`FR-2`, `FR-3`, `FR-17`, `FR-20`, `NFR-1`, `NFR-4`). Two balance
models were considered: maintained running balances vs. event-sourced (derive balances by
folding immutable entries).

## Decision

The ledger is **event-sourced**: **postings are the immutable source of truth**, and
balances are **derived by folding** an account's postings. **Periodic per-account snapshots**
(balance as-of a sequence point) make reads fast: `balance = latest snapshot + fold of
postings after it`. Nothing mutates a posting; corrections are reversing entries.

## Consequences

- Positive: append-only and point-in-time (`FR-17`/`FR-20`/`FR-3`) fall out naturally;
  audit is intrinsic (`FR-12`); balances are always reconstructable/verifiable.
- Trade-offs: reads require snapshots + fold (added complexity); value-dated point-in-time
  needs care when entries are backdated across a snapshot boundary (see design doc);
  snapshot cadence is a tuning parameter.
- Follow-ups: choose snapshot cadence and sequence scope; define holds' effect on balance
  (see ledger-core design, open questions).

## Alternatives considered

- **Maintained running balances** — rejected: fast reads but concurrency-sensitive, and it
  makes append-only/point-in-time/audit bolt-ons rather than intrinsic properties.
