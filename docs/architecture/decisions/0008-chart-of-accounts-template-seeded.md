# 0008. Template-seeded Chart of Accounts per tenant

- **Status:** Accepted
- **Date:** 2026-07-05
- **Deciders:** Harrified Tech

## Context

Every posting hits GL accounts, and tenants need a Chart of Accounts for GL reporting
(`FR-15`, `FR-18`) and to drive product/fee/interest posting (`FR-24`, `FR-26`). The engine
must be configured, not forked (`NFR-13`), and support many tenants per deployment (§2.1).

## Decision

Each **tenant** gets a Chart of Accounts **seeded from a default template** at setup, then
**customizable**. finCore ships a **standard internal-accounts set** in that template
(settlement/cash, FX position per currency, fee income, interest income, interest expense,
suspense/clearing, rounding). Customer accounts are created from **product templates** that
specify their CoA type (typically LIABILITY), currency, and the GL accounts fees/interest
post to.

## Consequences

- Positive: fast tenant onboarding with sane defaults, yet customizable; consistent
  internal accounts across tenants; posting rules are configuration, not code (`NFR-13`).
- Trade-offs: need template versioning/migration as the standard set evolves; customization
  must be validated (a broken CoA breaks posting).

## Alternatives considered

- **Fully custom per tenant** — rejected: slow onboarding, error-prone.
- **Fixed engine-wide CoA** — rejected: too inflexible for licensees with differing
  accounting needs.
