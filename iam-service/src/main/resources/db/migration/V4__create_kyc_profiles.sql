/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

CREATE TABLE kyc_profiles (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL,
    tenant_id     UUID        NOT NULL,
    date_of_birth DATE,
    bvn           CHAR(11),
    nin           CHAR(11),
    kyc_level     VARCHAR(20) NOT NULL DEFAULT 'NONE',
    verified_at   TIMESTAMPTZ,
    metadata      JSONB,
    version       BIGINT      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_kyc_profiles            PRIMARY KEY (id),
    CONSTRAINT fk_kyc_profiles_user       FOREIGN KEY (user_id)   REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_kyc_profiles_tenant     FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT uq_kyc_profiles_user_id    UNIQUE (user_id),
    CONSTRAINT chk_kyc_profiles_level     CHECK (kyc_level IN ('NONE', 'BASIC', 'TIER_1', 'TIER_2', 'FULL')),
    CONSTRAINT chk_kyc_profiles_bvn       CHECK (bvn IS NULL OR bvn ~ '^\d{11}$'),
    CONSTRAINT chk_kyc_profiles_nin       CHECK (nin IS NULL OR nin ~ '^\d{11}$')
);

CREATE INDEX idx_kyc_profiles_tenant_id ON kyc_profiles (tenant_id);
