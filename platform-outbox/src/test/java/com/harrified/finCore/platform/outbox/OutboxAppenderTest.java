/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.platform.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxAppenderTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final OutboxAppender appender = new OutboxAppender(jdbc, new ObjectMapper());

    @Test
    void serializesPayloadToJson() {
        assertEquals("{\"amount\":500}", appender.toJson(Map.of("amount", 500)));
    }

    @Test
    void rejectsUnserializablePayload() {
        // An empty anonymous bean cannot be serialized (FAIL_ON_EMPTY_BEANS).
        assertThrows(IllegalArgumentException.class, () -> appender.toJson(new Object() {
        }));
    }

    @Test
    void insertsOneRowAndReturnsId() {
        UUID id = appender.append("t1", "ledger.entry.posted", "agg-9", "ledger.entry.posted",
                Map.of("a", 1));

        assertNotNull(id);
        // sql + 6 params (id, tenant, topic, key, eventType, payload)
        verify(jdbc).update(contains("outbox"), any(), any(), any(), any(), any(), any());
    }
}
