# 0010. AGPL-3.0 community edition with a commercial dual license

- **Status:** Accepted
- **Date:** 2026-07-06
- **Deciders:** Harrified Tech

## Context

finCore is a licensed, self-hosted core ledger engine ([ADR-0001](0001-licensed-self-hosted-engine.md)):
fintechs deploy and run it in their own environment, often building a product — including hosted
services — around it. The project was initially released under Apache-2.0, but a permissive license
lets anyone fork finCore, modify it, and redistribute (or operate) it under closed/proprietary terms
with no obligation to share improvements. That undercuts both the "derivative works stay free" goal
and the commercial-licensing revenue model the product depends on.

The source file headers had already been changed to `SPDX-License-Identifier: AGPL-3.0-only`, but the
root `LICENSE` file still contained Apache-2.0 text — an inconsistency that had to be resolved.

We need a license that (a) keeps all community derivatives free, (b) prevents relicensing under
different/proprietary terms, (c) closes the "SaaS loophole" given finCore is designed to be *run as a
service*, and (d) still supports a paid commercial offering.

## Decision

We will **dual-license** finCore:

- **Community Edition — AGPL-3.0-only.** The canonical, verbatim GNU AGPL-3.0 text is the root
  `LICENSE`. AGPL (over plain GPLv3) is chosen deliberately: §13 extends copyleft to network use, so
  anyone operating a modified finCore as a service must publish their corresponding source. Every
  source file carries the AGPL SPDX header.
- **Commercial Edition.** A separate, paid commercial license lifts the AGPL's copyleft obligations
  for customers who embed finCore in closed-source products or run modified hosted services without
  publishing source. Terms are summarized in `COMMERCIAL-LICENSE.md`.
- **Contributor grant.** Contributions are accepted under an inbound-license grant
  (`CONTRIBUTING.md`) that lets Harrified Tech offer them under **both** licenses, with DCO sign-off.
  This is what makes dual-licensing legally sound without a copyright assignment.

## Consequences

- Positive: community derivatives — including hosted/SaaS deployments — must stay open under AGPL;
  finCore cannot be relicensed proprietary by third parties; the commercial edition is preserved as a
  revenue path; source-header/LICENSE inconsistency is resolved.
- Negative / trade-offs: AGPL deters some corporate adopters (policy bans on AGPL), which is partly
  the point — it channels them to the commercial edition; dual-licensing requires disciplined
  contributor-grant intake (every contributor must accept `CONTRIBUTING.md` terms) and, ideally,
  signed corporate CLAs for company contributions.
<!-- - Follow-ups this creates: confirm the `licensing@harrified.tech` contact (placeholder); have the
  contributor grant and commercial summary reviewed by counsel; decide whether a signed corporate CLA
  is required; align the planned licensing/entitlement subsystem ([build plan](../build-plan.md),
  Phase 10) with the two editions. -->

## Alternatives considered

- **Stay on Apache-2.0** — rejected: permissive terms allow proprietary forks and closed hosted
  services, defeating both the "keep derivatives free" and commercial-licensing goals.
- **Plain GPL-3.0-only** — rejected: copyleft triggers only on *distribution*, so a competitor could
  run a modified finCore as a hosted service without sharing changes — the exact loophole AGPL §13
  closes, and finCore is built to be run as a service.
- **GPL-2.0-only** — rejected: no explicit patent grant and incompatible with some GPLv3 code; wrong
  fit for a new project.
- **Pure AGPL, no commercial option** — rejected: leaves no compliant path for closed-source/SaaS
  adopters and no commercial revenue, conflicting with the licensed-engine model ([ADR-0001](0001-licensed-self-hosted-engine.md)).
