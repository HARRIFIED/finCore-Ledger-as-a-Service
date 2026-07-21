# 0010. gRPC for all internal service-to-service calls

- **Status:** Accepted — **implemented & live-verified 2026-07-21**
- **Date:** 2026-07-21
- **Deciders:** Harrified Tech

## Context

The [service interaction map](../service-interaction-map.md) always specified gRPC for internal
sync calls with REST only at the gateway edge, but the first inter-service wiring
(account-service → party-service / ledger-core) was built with REST as an expedient shortcut —
a divergence from the design. The posting path targets **more than 5,000 TPS** (`NFR-6`), and
we want strongly-typed contracts across services.

## Decision

**All internal service-to-service communication uses gRPC.** REST/JSON is only the **external
edge** protocol, exposed by the API gateway (which routes external REST calls to each service's
REST controllers). Concretely:

- An operation invoked by **another finCore service** is a **gRPC** RPC (proto in `api-contracts`,
  package `fincore.<domain>.v1`). E.g. `account → party` access checks, `account → ledger`
  account creation, `transfer/hold → ledger` posting.
- An operation invoked by an **external client via the gateway** stays **REST** on the service
  (e.g. open-account, balance queries a licensee reads).
- The existing REST inter-service clients (`PartyClient`, `LedgerClient`) **were converted to
  gRPC** (net.devh `@GrpcClient` blocking stubs) — no REST-based service-to-service calls remain.
  Be disciplined: new inter-service calls are gRPC from the start.
- **Tenant/identity propagate over gRPC metadata** (`x-program-id`), mirrored from the gateway
  headers, via a shared gRPC interceptor pair in `platform-tenancy`
  (`GrpcTenantServerInterceptor` / `GrpcTenantClientInterceptor`, registered globally via
  net.devh so every channel/server gets them automatically).

## Consequences

- Positive: typed proto contracts; HTTP/2 performance for the hot posting path; uniform internal
  transport; streaming available for batch posting later.
- Trade-offs: each service runs a gRPC server (`grpc-spring-boot-starter`, already used by
  iam-service) alongside its REST controllers; more build/codegen; need a gRPC tenancy interceptor
  and client-side metadata plumbing. Retrofitting ledger-core + party-service + account-service is
  real work (see build plan).

## Alternatives considered

- **Hybrid (REST control-plane + gRPC hot path)** — rejected: two conventions to reason about;
  the team prefers one disciplined internal transport.
- **REST everywhere internal** — rejected: leaves the >5,000 TPS posting path on JSON/HTTP overhead
  and untyped contracts.
