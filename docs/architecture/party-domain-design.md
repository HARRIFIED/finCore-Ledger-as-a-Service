# Party / Customer Domain Design

Gives [ADR-0004](decisions/0004-separate-customer-party-master.md) its concrete shape: the
Customer Information File (CIF) as finCore owns it. Satisfies **PTY-1…PTY-6**, supports
retail and corporate/SME (requirements §2.2), and resolves requirements §7 Q3 (placement)
and Q4 (mapping ownership) via [ADR-0006](decisions/0006-party-service-and-authorization.md).

Status: **draft.**

## 1. Placement — a dedicated `party-service`

The Party master is **not** part of iam-service. Reasoning:

- iam-service is the **optional bundled IdP** — it exists only for licensees without their
  own IdP, and is federated *out* otherwise ([ADR-0002](decisions/0002-federated-authentication.md)).
- The Party/CIF is **always-present, system-of-record banking data** (KYC'd account owners),
  needed regardless of who authenticates.

Coupling always-needed CIF data to an optional login service would be wrong. So:

| Service | Owns | Presence |
|---------|------|----------|
| **iam-service** | Login users, credentials, JWT issuance | Optional (bundled IdP) |
| **party-service** (new) | Party (CIF), KYC/KYB, party relationships, user→party mapping | Always |
| **account-service** | Customer accounts (product/metadata); `partyId` → Party | Always |

KYC currently lives in iam-service (`KycProfile` on `User`) — it **moves to party-service**
and attaches to the **Party**, not the login user (see §8).

## 2. The Party aggregate (PTY-1)

```
Party
├─ id, tenantId, type (INDIVIDUAL | ORGANIZATION), status (PROSPECT|ACTIVE|SUSPENDED|CLOSED)
├─ externalRef        (the licensee's own id for this party — for their reconciliation)
├─ displayName
├─ contacts[]         (email / phone / address value objects)
├─ INDIVIDUAL → individualProfile (names, DOB, nationality, gender?, …)
└─ ORGANIZATION → organizationProfile (legalName, registrationNumber, taxId,
                                       incorporationDate, orgType, industry, …)
```

- Tenant-scoped (TEN-1): every party belongs to one tenant.
- `type` drives which profile and which verification (KYC vs KYB) applies.

## 3. Verification — KYC (individuals) / KYB (organizations) (PTY-3)

- A **verification profile** attaches to the party: level/tier + status + documents.
  - INDIVIDUAL → KYC (identity docs, BVN/NIN, liveness, …).
  - ORGANIZATION → KYB (registration docs, directors, UBOs, …).
- **Tiers gate operations** (ties `FR-23`): e.g. tier-0 = no transacting; higher tiers lift
  limits. account/transfer services check the party's tier before permitting operations.

## 4. Relationships

### 4.1 Party ↔ Account (ownership) — PTY-2

Accounts are owned by a **party**, not a login user. `account-service.Account.partyId → Party`.
One party → many accounts.

- **Retail:** the INDIVIDUAL party owns its accounts.
- **Corporate:** the ORGANIZATION party owns the company's accounts; individuals are
  *authorized users*, not owners.
- **Joint accounts** (multiple parties own one account) → an account↔party ownership
  relationship rather than a single `partyId`. **v1 scope question** (§10 Q1).

### 4.2 Party ↔ User (login) — `PartyMembership` (PTY-4, PTY-5)

The **user→party mapping is finCore data**, held in party-service (not the IdP — see
[ADR-0006](decisions/0006-party-service-and-authorization.md)):

```
PartyMembership: (tenantId, partyId, subject, role, status)
  subject = the opaque login id (JWT sub / X-Subject) from the federated IdP
  role    = OWNER | ADMIN | INITIATOR | APPROVER | VIEWER | SIGNATORY …
```

- **Retail:** the individual's login has one membership (OWNER of their own party).
- **Corporate:** many memberships — several logins mapped to the org party with different
  roles (maker/checker: INITIATOR proposes, APPROVER authorizes).
- **Staff on behalf of a party:** a staff login has a membership (or a broader staff scope)
  authorizing action for a party that itself may have no login (PTY-5).

### 4.3 Party ↔ Party (`PartyRelationship`)

Links parties to each other — essential for corporate/SME KYB:

```
PartyRelationship: (fromPartyId, toPartyId, type)
  type = DIRECTOR | UBO (beneficial owner) | SIGNATORY | GUARDIAN | PARENT_ORG …
```

E.g. an ORGANIZATION has DIRECTOR/UBO relationships to INDIVIDUAL parties.

## 5. Retail vs corporate/SME — walkthroughs

**Retail (individual):**
```
Party(INDIVIDUAL, KYC tier-2) ── owns ── Account(s)
   └─ PartyMembership(subject=login-A, role=OWNER)
```

**Corporate/SME (organization):**
```
Party(ORGANIZATION "Acme Ltd", KYB verified) ── owns ── Account(s)
   ├─ PartyMembership(subject=login-CFO,   role=APPROVER)
   ├─ PartyMembership(subject=login-Clerk, role=INITIATOR)
   ├─ PartyRelationship → Party(INDIVIDUAL "Jane", DIRECTOR)
   └─ PartyRelationship → Party(INDIVIDUAL "Sam",  UBO)
```

## 6. Authorization model (PTY-6)

Authentication is federated (the IdP proves *who* the subject is). **Authorization is
finCore's** — resolved against `PartyMembership`:

```
May subject S perform action A on account owned by party P?
  → S has a PartyMembership on P with a role permitting A (+ party KYC tier allows A)
```

- The gateway is unchanged: it injects `X-Subject` (login), `X-Program-Id` (tenant),
  `X-Scopes`. It does **not** know parties.
- A service handling an operation resolves the acting party and checks membership via
  party-service (memberships are cacheable; low churn).
- **Enforcement mechanism** (sync membership check + cache vs. membership claims injected
  when iam-service is the IdP) → **§10 Q3**.

## 7. Service interactions

```
Open account:
  client → gateway → account-service
     account-service → party-service: validate partyId exists, ACTIVE, tier OK
     account-service → ledger-core:   create ledger account
     account-service: persist Account{partyId, ledgerAccountId}

Authorize a transfer:
  client → gateway → transfer-service
     transfer-service → party-service: membership(subject, owningParty) allows INITIATE?
     transfer-service → ledger-core:   post entry
```

## 8. Impact on existing code

- **account-service:** `Account.ownerId (UUID = X-Subject)` becomes **`Account.partyId → Party`**.
  The current `ownerId = X-Subject` placeholder (from the CallerContext work) is replaced;
  ownership keys off the party, and the caller's right to act is a membership check. This
  closes the original "opaque subject vs UUID owner" flag.
- **iam-service:** KYC (`KycProfile`/`KycDocument`) **moves out** to party-service, re-homed
  on the Party. iam-service keeps only login-user concerns.
- **`CallerContext`** (platform-security) is unchanged — `subject` stays the opaque login id;
  party resolution/authorization happens in the domain services against party-service.

## 9. Data model (party_db) — sketch

```
party(id, tenant_id, type, status, external_ref, display_name, created_at, updated_at, version)
individual_profile(party_id PK/FK, first_name, last_name, dob, nationality, …)
organization_profile(party_id PK/FK, legal_name, registration_number, tax_id,
                     incorporation_date, org_type, industry, …)
party_contact(id, party_id, kind, value, is_primary)
verification_profile(id, party_id, kind KYC|KYB, level, status, verified_at)
verification_document(id, verification_profile_id, doc_type, status, …)
party_membership(id, tenant_id, party_id, subject, role, status, created_at)
  └─ INDEX(tenant_id, subject)   -- "what parties can this login act for?"
  └─ INDEX(party_id)             -- "who can act for this party?"
party_relationship(id, tenant_id, from_party_id, to_party_id, type, created_at)
```

## 10. Open questions

1. **Joint accounts at v1?** Single `Account.partyId` (simplest) vs. account↔party ownership
   set (supports joint/retail-couples and corporate signatories-as-owners).
2. **Maker-checker / mandates depth** — do we model approval mandates (e.g. transfers > X
   need 2 APPROVERs) at v1, or just roles now and mandates later?
3. **Authorization enforcement** — synchronous membership check (+ cache) in each service,
   vs. injecting membership/role claims into the token when iam-service is the IdP (doesn't
   generalize to external IdPs). Recommend **sync + cache** as the general mechanism.
4. **Verification model** — one unified `verification_profile` (kind = KYC|KYB) as sketched,
   vs. separate KYC/KYB aggregates. (Leaning unified.)
5. **Service naming** — `party-service` (chosen, extensible) vs. `customer-service` (business
   familiar / Finacle "CIF"). Trivial to rename; confirm preference.
