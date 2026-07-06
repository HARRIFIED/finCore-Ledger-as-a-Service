/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.platform.outbox;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Polling relay: on a fixed interval it reads a batch of unpublished {@code outbox} rows
 * (oldest first, across all tenants), publishes each to Kafka, and marks it published.
 *
 * <p>Ordering & safety: rows publish oldest-first; on the first send failure the cycle
 * stops (the row's attempt count is bumped) and retries next tick — so a stuck row never
 * lets later rows overtake it, and a row is marked published only after Kafka confirms.
 */
public class OutboxRelay {

    private static final String SELECT_PENDING = """
            SELECT id, topic, message_key, payload
            FROM outbox
            WHERE published_at IS NULL
            ORDER BY created_at
            LIMIT ?
            """;
    private static final String MARK_PUBLISHED = "UPDATE outbox SET published_at = now() WHERE id = ?";
    private static final String BUMP_ATTEMPTS = "UPDATE outbox SET attempts = attempts + 1 WHERE id = ?";

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final int batchSize;
    private final long sendTimeoutMs;

    public OutboxRelay(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka, int batchSize, long sendTimeoutMs) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.batchSize = batchSize;
        this.sendTimeoutMs = sendTimeoutMs;
    }

    @Scheduled(fixedDelayString = "${fincore.outbox.poll-interval-ms:1000}")
    public void pollAndPublish() {
        List<Pending> batch = jdbc.query(
                SELECT_PENDING,
                (rs, rowNum) -> new Pending(
                        rs.getObject("id", UUID.class),
                        rs.getString("topic"),
                        rs.getString("message_key"),
                        rs.getString("payload")),
                batchSize);

        for (Pending p : batch) {
            try {
                kafka.send(p.topic(), p.key(), p.payload()).get(sendTimeoutMs, TimeUnit.MILLISECONDS);
                jdbc.update(MARK_PUBLISHED, p.id());
            } catch (Exception e) {
                // Leave the row unpublished for the next tick; stop to preserve ordering.
                jdbc.update(BUMP_ATTEMPTS, p.id());
                break;
            }
        }
    }

    record Pending(UUID id, String topic, String key, String payload) {
    }
}
