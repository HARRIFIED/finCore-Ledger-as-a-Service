# 0002. Federated authentication at the gateway

- **Status:** Accepted
- **Date:** 2026-06-29
- **Deciders:** Harrified Tech

## Context

A licensee already has its own users and identity provider. Forcing them to migrate users
into finCore would be high-friction and wrong for a licensed engine (`NFR-8`). Downstream
services must not each re-validate tokens.

## Decision

The **API Gateway** is the single trust boundary. It validates JWT signatures against a
**configurable issuer's JWKS** (`AUTH_ISSUER_JWKS_URL`) — the licensee's own IdP by
default, or the optional bundled `iam-service`. It **strips** any inbound identity headers
and **injects** `X-Subject` / `X-Program-Id` / `X-Scopes` from the verified claims.
Downstream services **trust these headers and never parse tokens** (consumed via the
shared `platform-security` module). The primary caller is the licensee's backend
(service-to-service), not the end-user device.

## Consequences

- Positive: licensees plug in their existing IdP with config only; uniform identity model
  downstream; `iam-service` is optional.
- Trade-offs: the gateway is security-critical (must strip spoofed headers — implemented
  and verified); `subject`/`programId` are opaque strings, which motivates [ADR-0004].
- Follow-up: claim-name mapping for external IdPs (`GATEWAY_PROGRAM_CLAIM` / `..._SCOPE_CLAIM`).

## Alternatives considered

- **Each service validates JWTs** — rejected: duplication, and couples every service to IdP config.
- **finCore-owned mandatory IdP** — rejected: forces user migration; contradicts the licensed model.
