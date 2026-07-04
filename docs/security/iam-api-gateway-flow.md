# IAM ↔ API Gateway Federation Flow

How authentication flows through finCore, end to end: from obtaining a token, through
the gateway's validation and identity-header injection, to a downstream service reading
the caller's identity.

> **Verified** locally on 2026-07-04 (api-gateway + iam-service + Postgres, plus a
> header-echo downstream and account-service): a real token was minted, validated, its
> claims propagated as headers, spoofed headers were stripped, and account-service read
> the injected identity (401 when absent).

## Principles

- **The gateway is the only trust boundary for tokens.** It validates JWT signatures;
  downstream services never parse tokens.
- **Federation-first.** The gateway validates against a *configurable* issuer's JWKS —
  the bundled iam-service by default, or a licensee's own IdP. Switching is configuration,
  not code.
- **Backend-to-backend.** The integration client is the licensee's backend (mTLS +
  OAuth2), not the end-user's device — mirroring how a bank's channels reach a core.
- **Identity headers are gateway-authoritative.** The gateway strips any inbound copies
  and re-sets them from the verified token, so downstream services can trust them.

## 1. Token acquisition

```
Client ──POST /api/v1/auth/{register,login}──▶ API Gateway ──▶ iam-service
                                                                │ validates credentials,
                                                                │ signs an RS256 JWT
Client ◀──────────── { accessToken, refreshToken } ────────────┘
```

`/api/v1/auth/**` and `/api/v1/.well-known/**` are `permitAll` at the gateway, so a client
can obtain a token (and the gateway can fetch signing keys) without already holding one.

The bundled iam-service stamps these claims (see `JwtService`):

| Claim        | Source                    |
|--------------|---------------------------|
| `sub`        | user id                   |
| `program_id` | user's tenant/program id  |
| `role`       | user role                 |
| `iss`        | `JWT_ISSUER`              |
| `email`      | user email                |
| `iat`/`exp`  | issued-at / expiry        |

## 2. Authenticated request

```
End user ─▶ Licensee backend ─Bearer JWT─▶ API Gateway ─────────────────▶ downstream service
                                            │ 1. validate JWT signature via JWKS
                                            │      (AUTH_ISSUER_JWKS_URL)
                                            │ 2. STRIP inbound X-Subject / X-Program-Id / X-Scopes
                                            │ 3. INJECT them from the verified claims
                                            └─────────────────────────────▶ trusts headers
```

1. The gateway validates the JWT signature against the configured issuer's JWKS.
2. It **strips** any client-supplied `X-Subject` / `X-Program-Id` / `X-Scopes`
   (anti-spoofing) — unconditionally, even on public routes.
3. It **injects** those headers from the verified claims and forwards downstream.
4. Downstream services trust the headers; `ownerId` derives from `X-Subject`, never the body.

Implemented by `IdentityHeaderGlobalFilter` in `api-gateway`.

### Claim → header mapping

| Injected header | From JWT claim | Notes |
|-----------------|----------------|-------|
| `X-Subject`     | `sub`          | opaque end-user id |
| `X-Program-Id`  | `program_id` (name via `GATEWAY_PROGRAM_CLAIM`) | omitted if the claim is absent |
| `X-Scopes`      | `scope` (via `GATEWAY_SCOPE_CLAIM`), falling back to `scp` / `roles` / `role` | normalised to a space-delimited string |

## 3. Downstream consumption

Downstream services read the injected identity — they do not validate tokens. This lives
in the shared **`platform-security`** module, so any servlet service consumes it by
adding the dependency (`implementation project(':platform-security')`) — the resolver is
registered via Spring Boot auto-configuration, with **no per-service wiring**:

- `GatewayHeaders` — the header-name constants.
- `CallerContext(subject, programId, scopes)` — the caller, as **opaque** strings.
- `CallerContextArgumentResolver` — resolves a `CallerContext` controller argument from
  the headers, and returns **401** if `X-Subject` is absent (the request did not pass
  through the gateway).
- `GatewayIdentityAutoConfiguration` — registers the resolver for servlet web apps.

```java
@GetMapping("/me")
public CallerContext me(CallerContext caller) {   // resolved from gateway headers
    return caller;                                 // ownership will key off caller.subject()
}
```

Verified behaviour of `GET /api/v1/accounts/me`:

| Request | Result |
|---------|--------|
| with `X-Subject` (+ optional program/scopes) | `200` → `{subject, programId, scopes[]}` |
| no `X-Subject` | `401` |
| `X-Subject` only | `200` → `programId: null, scopes: []` |

> **Note:** `subject`/`programId` are opaque strings. With the bundled iam-service they
> are UUIDs, but a federated IdP may use any format. Reconciling an opaque subject with
> entities that key on a UUID owner is part of the customer/party-boundary work.

## Configuration knobs

| Component | Key | Purpose |
|-----------|-----|---------|
| gateway | `AUTH_ISSUER_JWKS_URL` | trusted issuer's JWKS — **the federation switch** |
| gateway | `GATEWAY_PROGRAM_CLAIM` / `GATEWAY_SCOPE_CLAIM` | map an external IdP's claim names |
| iam-service | `JWT_ISSUER` | `iss` stamped on bundled-IdP tokens |
| iam-service | `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` | RS256 keypair (ephemeral in dev if blank) |

## Federated vs bundled

- **Bundled (default):** the licensee uses iam-service as its IdP; the gateway trusts
  iam's JWKS at `/api/v1/.well-known/jwks.json`.
- **Federated:** point `AUTH_ISSUER_JWKS_URL` at the licensee's own IdP and iam-service
  becomes optional — **configuration only, no code change.**
