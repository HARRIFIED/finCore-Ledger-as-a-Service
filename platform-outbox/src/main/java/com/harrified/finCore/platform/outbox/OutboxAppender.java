/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.platform.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

/**
 * Appends an event to the {@code outbox} table. Call this from <b>within the same
 * {@code @Transactional}</b> as the state change you are recording: because it uses
 * {@link JdbcTemplate} on the shared DataSource, the insert joins the caller's transaction,
 * so the event is committed atomically with the state change (the transactional-outbox
 * guarantee, FR-25). A separate {@link OutboxRelay} publishes it to Kafka afterwards.
 *
 * <p>Uses JDBC (not a JPA entity) deliberately: the outbox is written with an explicit
 * {@code tenantId} and read cross-tenant by the relay, so it must not be subject to
 * Hibernate tenant filtering.
 */
public class OutboxAppender {

    private static final String INSERT = """
            INSERT INTO outbox (id, tenant_id, topic, message_key, event_type, payload)
            VALUES (?, ?, ?, ?, ?, ?::jsonb)
            """;

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public OutboxAppender(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    /**
     * @param tenantId     owning tenant (program) — stored explicitly, not from any filter
     * @param topic        Kafka topic to publish to
     * @param messageKey   Kafka message key (use the aggregate id for per-aggregate ordering)
     * @param eventType    logical event name (e.g. {@code ledger.entry.posted})
     * @param payload      event body; serialized to JSON and stored as jsonb
     * @return the generated outbox row id
     */
    public UUID append(String tenantId, String topic, String messageKey, String eventType, Object payload) {
        UUID id = UUID.randomUUID();
        String json = toJson(payload);
        jdbc.update(INSERT, id, tenantId, topic, messageKey, eventType, json);
        return id;
    }

    String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize outbox payload of type "
                    + (payload == null ? "null" : payload.getClass().getName()), e);
        }
    }
}
