-- Copyright (c) 2026-present Harrified tech and contributors
-- SPDX-License-Identifier: AGPL-3.0-only
-- See the LICENSE file for details.
--
-- Event-sourced balance snapshots (ADR-0005): a periodically-advanced fold per account so
-- balance reads = snapshot ⊕ fold(postings after the snapshot), instead of folding from zero.

-- Global monotonic sequence stamped on every posting — a cheap, ordered high-water mark for
-- snapshotting. DB-managed (DEFAULT nextval); intentionally NOT mapped on the Posting entity,
-- so inserts omit it and Hibernate schema-validation ignores the extra column.
CREATE SEQUENCE posting_seq_seq;
ALTER TABLE posting ADD COLUMN seq BIGINT NOT NULL DEFAULT nextval('posting_seq_seq');
ALTER SEQUENCE posting_seq_seq OWNED BY posting.seq;
CREATE INDEX idx_posting_account_seq ON posting (tenant_id, ledger_account_id, seq);

-- One current snapshot per ledger account (upserted as it advances).
CREATE TABLE balance_snapshot (
    ledger_account_id UUID         NOT NULL,
    tenant_id         VARCHAR(100) NOT NULL,
    as_of_seq         BIGINT       NOT NULL,
    debits_total      BIGINT       NOT NULL,
    credits_total     BIGINT       NOT NULL,
    as_of_time        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_balance_snapshot PRIMARY KEY (ledger_account_id)
);
