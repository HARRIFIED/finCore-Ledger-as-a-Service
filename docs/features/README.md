# Feature Flows

Narrative, end-to-end walkthroughs of finCore capabilities — how a whole flow works, which
services take part, what each contributes, and **how an external system plugs in**.

These read like guided tours (close to a blog post), and are the counterpart to
[`../architecture/`](../architecture/), which holds the reference design and decisions.
When a flow is built and verified, we add its walkthrough here.

## Conventions

- One file per user-visible flow: `docs/features/<flow-name>.md`.
- Show the **request path** (a fenced ASCII sequence), a **step-by-step narrative** naming
  each service's role, a **concrete request/response** example (from a verified run), and an
  **"integrating from outside"** section for licensee backends.
- Link back to the architecture docs / ADRs for the "why".
- Only document flows that actually work — note what was verified and when.

## Index

| Flow | Status | What it covers |
|------|--------|----------------|
| [Opening an account](opening-an-account.md) | ✅ working | Federated auth → party authorization → ledger account → persisted account, across api-gateway, account-service, party-service, ledger-core |
