# 0007. FX rates and conversion

- **Status:** Accepted
- **Date:** 2026-07-05
- **Deciders:** Harrified Tech

## Context

finCore must convert between currencies (`FR-8`) for multicurrency operations (`FR-9`),
while remaining self-hosted and able to run air-gapped (`NFR-10`, `NFR-12`). It also needs
to support the licensee's FX revenue and produce auditable postings (`NFR-1`, `NFR-4`).

## Decision

1. **Internal rate table** — `fx-service` stores exchange rates the **licensee feeds** (via
   API or scheduled import from their own provider). No external runtime feed, so
   conversions work air-gapped. Every conversion **captures the rate-as-of**.
2. **Short-lived quotes** — a client may request a **quote** that locks a rate for a short
   TTL, then execute against the `quoteId`. (Unquoted conversions use the current table rate.)
3. **Configurable spread** — an optional per-tenant / per-currency-pair **markup on the mid
   rate**; the applied rate is `mid ± spread`, and the margin posts to an **FX-income** GL
   account (licensee revenue). Spread 0 = pass-through.
4. **Posting shape** — a cross-currency movement is **one multi-currency journal entry**,
   balanced **per currency** through internal **FX position accounts**; the spread margin is
   a separate credit leg to FX-income (see design doc for a worked example).

## Consequences

- Positive: air-gapped-friendly; licensee controls rates and FX revenue; single atomic,
  auditable entry per conversion; rate-as-of/quote captured for dispute/audit.
- Trade-offs: rates are only as fresh as the licensee's feed; quote TTL and stale-rate
  policy must be defined; FX position accounts must be monitored/revalued by the licensee.

## Alternatives considered

- **External live market feed** — rejected: external runtime dependency conflicts with
  air-gapped/on-prem deployment.
- **Pass-through mid rate only** — rejected: no built-in FX-revenue mechanism (spread 0
  already covers that case).
