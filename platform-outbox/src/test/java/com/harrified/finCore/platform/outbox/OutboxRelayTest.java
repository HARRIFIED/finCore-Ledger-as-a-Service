/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.platform.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
class OutboxRelayTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final OutboxRelay relay = new OutboxRelay(jdbc, kafka, 100, 5000);

    @Test
    void publishesPendingRowsThenMarksThemPublished() {
        OutboxRelay.Pending p1 = new OutboxRelay.Pending(UUID.randomUUID(), "topicA", "k1", "{\"x\":1}");
        OutboxRelay.Pending p2 = new OutboxRelay.Pending(UUID.randomUUID(), "topicA", "k2", "{\"x\":2}");
        doReturn(List.of(p1, p2)).when(jdbc).query(anyString(), any(RowMapper.class), anyInt());
        doReturn(CompletableFuture.completedFuture(null)).when(kafka).send(anyString(), anyString(), anyString());

        relay.pollAndPublish();

        verify(kafka).send("topicA", "k1", "{\"x\":1}");
        verify(kafka).send("topicA", "k2", "{\"x\":2}");
        verify(jdbc).update(contains("published_at"), eq(p1.id()));
        verify(jdbc).update(contains("published_at"), eq(p2.id()));
    }

    @Test
    void onSendFailureBumpsAttemptsAndStopsWithoutMarkingPublished() {
        OutboxRelay.Pending p1 = new OutboxRelay.Pending(UUID.randomUUID(), "topicA", "k1", "{}");
        doReturn(List.of(p1)).when(jdbc).query(anyString(), any(RowMapper.class), anyInt());
        CompletableFuture<Object> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("kafka unavailable"));
        doReturn(failed).when(kafka).send(anyString(), anyString(), anyString());

        relay.pollAndPublish();

        verify(jdbc).update(contains("attempts"), eq(p1.id()));
        verify(jdbc, never()).update(contains("published_at"), any(UUID.class));
    }
}
