/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

CREATE TABLE kyc_documents (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id          UUID         NOT NULL,
    tenant_id        UUID         NOT NULL,
    type             VARCHAR(30)  NOT NULL,
    file_key         VARCHAR(512) NOT NULL,
    file_name        VARCHAR(255) NOT NULL,
    mime_type        VARCHAR(100),
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    reviewed_by      UUID,
    reviewed_at      TIMESTAMPTZ,
    metadata         JSONB,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_kyc_documents        PRIMARY KEY (id),
    CONSTRAINT fk_kyc_documents_user   FOREIGN KEY (user_id)   REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_kyc_documents_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT chk_kyc_documents_type  CHECK (type IN (
        'PASSPORT', 'NATIONAL_ID', 'DRIVERS_LICENSE', 'VOTERS_CARD',
        'BVN_SLIP', 'NIN_SLIP',
        'PROOF_OF_ADDRESS', 'UTILITY_BILL', 'BANK_STATEMENT',
        'SELFIE',
        'BUSINESS_REGISTRATION', 'TAX_IDENTIFICATION',
        'OTHER'
    )),
    CONSTRAINT chk_kyc_documents_status CHECK (status IN ('PENDING', 'UNDER_REVIEW', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_kyc_documents_user_id   ON kyc_documents (user_id);
CREATE INDEX idx_kyc_documents_tenant_id ON kyc_documents (tenant_id);
CREATE INDEX idx_kyc_documents_status    ON kyc_documents (status);
