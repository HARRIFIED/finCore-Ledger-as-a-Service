-- Copyright (c) 2026-present Harrified tech and contributors
-- SPDX-License-Identifier: AGPL-3.0-only
--
-- Transactional-outbox table (see platform-outbox). Written in the same transaction as the
-- party change; the polling relay publishes rows to Kafka. Not a JPA entity (JdbcTemplate,
-- read cross-tenant by the relay), so it is not @TenantId-scoped.

CREATE TABLE outbox (
    id           UUID         NOT NULL,
    tenant_id    VARCHAR(100) NOT NULL,
    topic        VARCHAR(255) NOT NULL,
    message_key  VARCHAR(255),
    event_type   VARCHAR(255) NOT NULL,
    payload      JSONB        NOT NULL,
    attempts     INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,

    CONSTRAINT pk_outbox PRIMARY KEY (id)
);
CREATE INDEX idx_outbox_unpublished ON outbox (created_at) WHERE published_at IS NULL;
