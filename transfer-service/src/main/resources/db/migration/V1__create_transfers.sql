-- Copyright (c) 2026-present Harrified tech and contributors
-- SPDX-License-Identifier: AGPL-3.0-only
--
-- Internal transfers (FR-5 internal). A transfer records the intent + outcome of moving funds
-- between two customer accounts; the actual double-entry lives in ledger-core (ledger_entry_id
-- links to it). tenant_id is Hibernate's @TenantId discriminator (row-level, ADR-0009).

CREATE TABLE transfers (
    id                     UUID         NOT NULL,
    tenant_id              VARCHAR(100) NOT NULL,
    from_account_id        UUID         NOT NULL,
    to_account_id          UUID         NOT NULL,
    from_ledger_account_id UUID         NOT NULL,
    to_ledger_account_id   UUID         NOT NULL,
    amount_minor           BIGINT       NOT NULL,
    currency               VARCHAR(3)   NOT NULL,
    status                 VARCHAR(20)  NOT NULL,
    idempotency_key        VARCHAR(200) NOT NULL,
    ledger_entry_id        UUID,
    reference              VARCHAR(255),
    version                BIGINT       NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_transfers PRIMARY KEY (id),
    -- Idempotency (FR-16): a client key never double-posts, scoped per tenant.
    CONSTRAINT uq_transfers_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_transfers_from_account ON transfers (tenant_id, from_account_id);
CREATE INDEX idx_transfers_to_account   ON transfers (tenant_id, to_account_id);
