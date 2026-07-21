-- Copyright (c) 2026-present Harrified tech and contributors
-- SPDX-License-Identifier: AGPL-3.0-only
-- See the LICENSE file for details.

-- Customer accounts. Owned by a party (party_id, CIF) and backed by a ledger account in
-- ledger-core (ledger_account_id). Tenant-scoped via tenant_id (@TenantId, ADR-0009).
CREATE TABLE accounts (
    id                UUID         NOT NULL,
    tenant_id         VARCHAR(100) NOT NULL,
    account_number    VARCHAR(50)  NOT NULL,
    account_name      VARCHAR(255) NOT NULL,
    description       TEXT,
    type              VARCHAR(50)  NOT NULL,
    currency          VARCHAR(3)   NOT NULL,
    status            VARCHAR(50)  NOT NULL,
    party_id          UUID         NOT NULL,
    ledger_account_id UUID         NOT NULL,
    parent_account_id UUID,
    metadata          JSONB,
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_accounts        PRIMARY KEY (id),
    CONSTRAINT uq_accounts_number UNIQUE (tenant_id, account_number)
);
CREATE INDEX idx_accounts_tenant ON accounts (tenant_id);
CREATE INDEX idx_accounts_party  ON accounts (tenant_id, party_id);

-- @ElementCollection tags.
CREATE TABLE account_tags (
    account_id UUID         NOT NULL,
    tag        VARCHAR(255),

    CONSTRAINT fk_account_tags_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE CASCADE
);
CREATE INDEX idx_account_tags_account ON account_tags (account_id);
