-- Copyright (c) 2026-present Harrified tech and contributors
-- SPDX-License-Identifier: AGPL-3.0-only
-- See the LICENSE file for details.

-- The Customer/Party master (CIF). One row per party; type-specific columns are populated
-- per `type` (bit 1 single-table model; may normalize into profile tables later).
CREATE TABLE party (
    id                  UUID         NOT NULL,
    tenant_id           VARCHAR(100) NOT NULL,
    type                VARCHAR(20)  NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    external_ref        VARCHAR(200),
    display_name        VARCHAR(255) NOT NULL,

    -- INDIVIDUAL (retail)
    first_name          VARCHAR(100),
    last_name           VARCHAR(100),
    date_of_birth       DATE,
    nationality         VARCHAR(3),

    -- ORGANIZATION (corporate/SME)
    legal_name          VARCHAR(255),
    registration_number VARCHAR(100),
    tax_id              VARCHAR(100),

    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_party        PRIMARY KEY (id),
    CONSTRAINT chk_party_type   CHECK (type IN ('INDIVIDUAL', 'ORGANIZATION')),
    CONSTRAINT chk_party_status CHECK (status IN ('PROSPECT', 'ACTIVE', 'SUSPENDED', 'CLOSED'))
);
CREATE INDEX idx_party_tenant       ON party (tenant_id);
CREATE INDEX idx_party_external_ref ON party (tenant_id, external_ref);
