-- Copyright (c) 2026-present Harrified tech and contributors
-- SPDX-License-Identifier: AGPL-3.0-only
--
-- Reference DDL for the transactional-outbox table. Each service OWNS its database, so
-- copy this into that service's own Flyway migration (e.g. Vn__create_outbox.sql). It is
-- intentionally NOT placed under db/migration here, to avoid Flyway auto-merging a shared
-- migration (and version clashes) across every service.

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

-- The relay only ever scans unpublished rows, oldest-first.
CREATE INDEX idx_outbox_unpublished ON outbox (created_at) WHERE published_at IS NULL;
