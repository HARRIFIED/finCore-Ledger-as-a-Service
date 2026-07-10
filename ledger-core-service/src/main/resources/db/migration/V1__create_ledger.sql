-- Copyright (c) 2026-present Harrified tech and contributors
-- SPDX-License-Identifier: AGPL-3.0-only
-- See the LICENSE file for details.

-- Ledger accounts: the unit balances are tracked for (customer wallets + internal/GL).
CREATE TABLE ledger_account (
    id         UUID         NOT NULL,
    tenant_id  VARCHAR(100) NOT NULL,
    reference  VARCHAR(100) NOT NULL,
    coa_type   VARCHAR(20)  NOT NULL,
    currency   VARCHAR(3)   NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_ledger_account          PRIMARY KEY (id),
    CONSTRAINT uq_ledger_account_ref       UNIQUE (tenant_id, reference),
    CONSTRAINT chk_ledger_account_coa      CHECK (coa_type IN ('ASSET', 'LIABILITY', 'EQUITY', 'INCOME', 'EXPENSE')),
    CONSTRAINT chk_ledger_account_status   CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED'))
);
CREATE INDEX idx_ledger_account_tenant ON ledger_account (tenant_id);

-- Journal entries: one balanced, immutable transaction per row.
CREATE TABLE journal_entry (
    id              UUID         NOT NULL,
    tenant_id       VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    booking_time    TIMESTAMPTZ  NOT NULL,
    value_date      DATE         NOT NULL,
    correlation_id  VARCHAR(100),
    source          VARCHAR(100),
    reversal_of     UUID,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_journal_entry        PRIMARY KEY (id),
    CONSTRAINT uq_journal_entry_idem   UNIQUE (tenant_id, idempotency_key),
    CONSTRAINT chk_journal_entry_status CHECK (status IN ('POSTED', 'PENDING', 'REVERSED'))
);
CREATE INDEX idx_journal_entry_tenant ON journal_entry (tenant_id);

-- Postings: the debit/credit legs of an entry (append-only).
CREATE TABLE posting (
    id                UUID         NOT NULL,
    tenant_id         VARCHAR(100) NOT NULL,
    entry_id          UUID         NOT NULL,
    ledger_account_id UUID         NOT NULL,
    direction         VARCHAR(6)   NOT NULL,
    amount_minor      BIGINT       NOT NULL,
    currency          VARCHAR(3)   NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_posting            PRIMARY KEY (id),
    CONSTRAINT fk_posting_entry      FOREIGN KEY (entry_id) REFERENCES journal_entry (id),
    CONSTRAINT fk_posting_account    FOREIGN KEY (ledger_account_id) REFERENCES ledger_account (id),
    CONSTRAINT chk_posting_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_posting_amount    CHECK (amount_minor > 0)
);
-- Tenant-leading, account-scoped: supports balance folding per account (ADR-0005/0009).
CREATE INDEX idx_posting_account ON posting (tenant_id, ledger_account_id);
CREATE INDEX idx_posting_entry   ON posting (entry_id);
