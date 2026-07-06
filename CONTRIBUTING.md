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

## Developer Certificate of Origin (sign-off)

Every commit must be signed off under the [Developer Certificate of Origin](https://developercertificate.org/).
Add a `Signed-off-by` line to each commit — `git commit -s` does this for you:

```
Signed-off-by: Your Name <your.email@example.com>
```

The sign-off certifies the origin statements in items 1–4 above.

## Making a change

1. Open an issue first for anything non-trivial, so we can agree on the approach.
2. Branch from `main`; keep the change focused.
3. Every new source file must carry the standard header:
   ```
   Copyright (c) 2026-present Harrified tech and contributors
   SPDX-License-Identifier: AGPL-3.0-only
   See the LICENSE file for details.
   ```
   (Use the comment syntax of the file's language; see existing files for examples.)
4. Follow the conventions in [`docs/`](docs/) — especially the
   [ADR process](docs/architecture/decisions/README.md) for significant, hard-to-reverse decisions.
5. Run the build and tests before opening the PR (`./gradlew build`).

---

> **Note for maintainers:** the contributor grant above is a lightweight inbound-license CLA
> intended to keep dual-licensing sound. Before relying on it commercially, have it reviewed by
> counsel — and consider requiring a signed corporate CLA for contributions made on behalf of a
> company. Past releases distributed under Apache-2.0 remain available under Apache-2.0; this
> grant governs contributions submitted from the AGPL era onward.
