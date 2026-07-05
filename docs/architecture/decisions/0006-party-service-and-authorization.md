# 0006. Dedicated party-service and finCore-owned authorization

- **Status:** Accepted
- **Date:** 2026-07-05
- **Deciders:** Harrified Tech
- Concretizes [ADR-0004](0004-separate-customer-party-master.md).

## Context

ADR-0004 locked the principle: a Customer/Party master (CIF) separate from login identity.
Two concrete questions remained (requirements §7 Q3/Q4): where the party master lives, and
who owns the `user → party` mapping. Authentication is federated ([ADR-0002](0002-federated-authentication.md)),
so the login user belongs to the licensee's IdP, but the KYC'd account-owning party is
finCore's system-of-record data.

## Decision

1. **A dedicated `party-service`** owns the Party (CIF), KYC/KYB verification, party↔party
   relationships, and the `user → party` mapping. It is **always present** — not part of the
   **optional** bundled iam-service. KYC moves out of iam-service onto the Party.
2. **Authorization is finCore-owned**, resolved against `PartyMembership(subject, party,
   role)` held in party-service — **not** carried as party ids in the IdP token. The IdP
   only authenticates (`X-Subject`); "may this subject act for this party?" is a finCore check.
3. **Accounts are owned by `partyId`** (not the login `subject`). The `account-service`
   `ownerId = X-Subject` placeholder is replaced by `partyId`.

## Consequences

- Positive: CIF isn't coupled to the optional IdP; authorization generalizes to any
  federated IdP (the IdP needn't know finCore party ids); models retail + corporate/SME.
- Trade-offs: a new service and DB (`party_db`); authorization needs a membership lookup
  (cacheable) on the request path; KYC migration out of iam-service.
- Follow-ups: enforcement mechanism (sync check + cache — recommended), joint-account and
  maker-checker scope for v1 (see party-domain-design §10).

## Alternatives considered

- **Party master inside iam-service** — rejected: couples always-needed CIF to the optional
  login service.
- **`party_id` claim from the IdP** — rejected: forces the licensee's IdP to know finCore's
  domain ids; doesn't fit external/federated IdPs.
