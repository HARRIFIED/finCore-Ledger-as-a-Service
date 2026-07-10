# Contributing to finCore

Thanks for your interest in finCore. Please read this document before opening a pull request —
the licensing terms below are a condition of contributing.

## Licensing of contributions (important)

finCore is **dual-licensed**: a free [AGPL-3.0](LICENSE) community edition and a paid
[commercial edition](COMMERCIAL-LICENSE.md), both built from the same source. For that model to
work, Harrified Tech must be able to distribute *every* line of the codebase — including your
contributions — under **both** licenses.

By submitting a contribution (a pull request, patch, or any code, documentation, or other material)
to this project, you agree to the following:

1. **Grant of rights.** You grant Harrison Ikpefua / Harrified Tech ("the Maintainer") a perpetual,
   worldwide, non-exclusive, royalty-free, irrevocable license to reproduce, prepare derivative
   works of, publicly display, publicly perform, sublicense, and distribute your contribution and
   such derivative works **under the AGPL-3.0 and under any other license terms, including
   Harrified Tech's commercial license**. In short: you allow your contribution to be offered under
   both the community and commercial editions.

2. **You retain your copyright.** This is a license grant, **not** a copyright assignment. You keep
   ownership of your contribution and may use it however you wish elsewhere.

3. **Patent grant.** You grant the Maintainer and all recipients of the software a license to any
   patent claims you own that are necessarily infringed by your contribution, consistent with the
   AGPL-3.0.

4. **You have the right to contribute.** You certify that each contribution is your original work
   (or that you have the necessary rights to submit it), and that submitting it does not violate any
   agreement with an employer or third party. If your employer has rights to work you create, you
   confirm you have permission to contribute under these terms, or your employer has waived those
   rights.

5. **No warranty.** You provide your contribution "as is," without warranties of any kind.

If you cannot agree to these terms, please do not submit a contribution.

<!-- ## Developer Certificate of Origin (sign-off)

Every commit must be signed off under the [Developer Certificate of Origin](https://developercertificate.org/).
Add a `Signed-off-by` line to each commit — `git commit -s` does this for you:

```
Signed-off-by: Your Name <your.email@example.com>
```

The sign-off certifies the origin statements in items 1–4 above. -->

## Making a change

We follow common open-source conventions so the history stays readable and reviews stay easy.

### 1. Open an issue first

For anything non-trivial, open an issue before you start coding so we can agree on the
approach. Use a clear, concise title with a type prefix:

| Type | Title format | Use for |
|------|--------------|---------|
| Bug | `Bug: <short description>` | Something is broken or behaves incorrectly |
| Feature | `Feature: <short description>` | A new capability |
| Improvement | `Improvement: <short description>` | Enhancing existing behaviour |
| Docs | `Docs: <short description>` | Documentation only |
| Chore | `Chore: <short description>` | Build, dependencies, tooling, cleanup |

Include enough for someone else to act on it:

- **Bugs:** what you did, what you expected, what actually happened, and the version/commit.
- **Features / Improvements:** lead with the problem and motivation, then the proposed change.

Significant, hard-to-reverse design decisions are captured as an
[ADR](docs/architecture/decisions/README.md) — link or open one from the issue.

### 2. Branch from `main`

Keep each branch focused on a single issue. Name it `<type>/<short-kebab-summary>`,
optionally prefixed with the issue number:

```
feat/ledger-balances
fix/outbox-relay-retry
docs/contributing-guide
chore/bump-spring-cloud
feat/142-point-in-time-balance
```

Branch/commit types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`, `ci`.

### 3. Commit using Conventional Commits

Write commit messages in the [Conventional Commits](https://www.conventionalcommits.org/) style:

```
<type>(<optional scope>): <imperative summary>

<optional body — the what and why, not the how>
```

Scope is the module or area touched. Examples:

```
feat(ledger-core): enforce per-currency double-entry invariant
fix(platform-outbox): confirm Kafka send before marking a row published
docs(architecture): add ADR-0009 row-level tenant isolation
chore(build): add junit-platform-launcher to the test classpath
```

Keep commits small and logically separate; tidy up noise (`git rebase`) before review.

### 4. Every new source file carries the standard header

Use the comment syntax of the file's language (see existing files for examples):

```
Copyright (c) 2026-present Harrified tech and contributors
SPDX-License-Identifier: AGPL-3.0-only
See the LICENSE file for details.
```

### 5. Open a pull request

- **Title:** the same Conventional Commit format as above — it becomes the squash-merge subject.
- **Link the issue:** add `Closes #<n>` (or `Refs #<n>`) to the description so it closes on merge.
- **Explain what & why**, and **how you verified it** (tests added, commands run). Call out any
  breaking changes or follow-ups.
- **Keep it focused and reviewable** — one concern per PR; prefer several small PRs over one large one.
- Open a **Draft PR** if you want early feedback before it's ready.

Before requesting review, run through the checklist:

- [ ] Branch is up to date with `main` and the change is focused on one concern.
- [ ] Every new source file has the standard header (above).
- [ ] Conventions in [`docs/`](docs/) are followed; an [ADR](docs/architecture/decisions/README.md)
      is added/updated for significant, hard-to-reverse decisions.
- [ ] Tests added/updated for the change.
- [ ] Build and tests pass locally: `./gradlew build`.
- [ ] Docs updated (README / `docs/`) wherever behaviour or setup changed.

