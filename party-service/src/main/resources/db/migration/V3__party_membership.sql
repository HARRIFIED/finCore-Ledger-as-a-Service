-- Copyright (c) 2026-present Harrified tech and contributors
-- SPDX-License-Identifier: AGPL-3.0-only
-- See the LICENSE file for details.
--
-- Links a login subject (the gateway's X-Subject, from the federated IdP) to a party with a
-- role — finCore's authorization mapping (ADR-0006). One row per (party, subject, role).

CREATE TABLE party_membership (
    id         UUID         NOT NULL,
    tenant_id  VARCHAR(100) NOT NULL,
    party_id   UUID         NOT NULL,
    subject    VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_party_membership       PRIMARY KEY (id),
    CONSTRAINT fk_party_membership_party FOREIGN KEY (party_id) REFERENCES party (id),
    CONSTRAINT uq_party_membership       UNIQUE (tenant_id, party_id, subject, role),
    CONSTRAINT chk_membership_role       CHECK (role IN ('OWNER', 'ADMIN', 'INITIATOR', 'APPROVER', 'VIEWER', 'SIGNATORY')),
    CONSTRAINT chk_membership_status     CHECK (status IN ('ACTIVE', 'REVOKED'))
);
-- "what parties can this login act for?" and "who can act for this party?"
CREATE INDEX idx_membership_subject ON party_membership (tenant_id, subject);
CREATE INDEX idx_membership_party   ON party_membership (party_id);
