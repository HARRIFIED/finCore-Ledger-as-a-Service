# 0004. Separate Customer/Party master from login identity

- **Status:** Accepted (principle; concrete shape to be designed)
- **Date:** 2026-07-05
- **Deciders:** Harrified Tech

## Context

"The user who logs in" is not "the customer who owns accounts." Real cores must handle
corporate customers with many users, customers with no login, staff acting on behalf of a
customer, and one person tied to several customer records. Because authentication is
federated ([ADR-0002]), the **login user belongs to the licensee's IdP** — but the
**KYC'd, account-owning party is finCore's system-of-record data** (`FR-1`, §2.2). This is
Finacle's CIF (Customer Information File) concept.

## Decision

finCore will own a **Customer/Party master** that is **distinct from login identity**.
Accounts are owned by a **party** (`partyId`), not by the login `subject`. Party types
cover **retail (`INDIVIDUAL`)** and **corporate/SME (`ORGANIZATION`)**. A `user → party`
mapping links federated login users to the parties they may act for, with roles/mandates.

The **principle is locked now**; the concrete shape is deferred to the design phase
(open questions: party-master service placement, mapping ownership, relationship
cardinality — see requirements §7).

## Consequences

- Positive: models retail + corporate correctly; fits federation; ownership survives
  changes in login. Lets `Account.ownerId = X-Subject` remain a serviceable placeholder
  until the party model lands (no rework needed now).
- Trade-offs: introduces a new domain and a `user↔party` mapping to design and maintain.

## Alternatives considered

- **Merge user == customer** — rejected: only fits single-login retail; breaks corporate,
  staff-serviced, and federated cases.
- **Defer entirely** — rejected: the principle affects account ownership and should be
  fixed before CRUD is built on the wrong key.
