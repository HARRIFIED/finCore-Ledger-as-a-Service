/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

CREATE TABLE users (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id     UUID         NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    phone_number  VARCHAR(30),
    role          VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    kyc_status    VARCHAR(20)  NOT NULL DEFAULT 'UNVERIFIED',
    address_street       VARCHAR(255),
    address_city         VARCHAR(100),
    address_state        VARCHAR(100),
    address_country      VARCHAR(100),
    address_postal_code  VARCHAR(20),

    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_users                PRIMARY KEY (id),
    CONSTRAINT fk_users_tenant         FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT uq_users_email_tenant   UNIQUE (email, tenant_id),
    CONSTRAINT chk_users_role          CHECK (role       IN ('CUSTOMER', 'TENANT_ADMIN', 'SUPER_ADMIN')),
    CONSTRAINT chk_users_kyc_status    CHECK (kyc_status IN ('UNVERIFIED', 'PENDING', 'VERIFIED', 'REJECTED')),
    CONSTRAINT chk_users_status        CHECK (status     IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

CREATE INDEX idx_users_tenant_id ON users (tenant_id);
CREATE INDEX idx_users_email     ON users (email);
