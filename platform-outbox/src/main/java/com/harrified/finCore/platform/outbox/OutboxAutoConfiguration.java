/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.platform.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashMap;
import java.util.Map;

/**
 * Wires the outbox for any service that adds {@code platform-outbox} and has both a
 * DataSource (JdbcTemplate) and Kafka on the classpath. Provides the {@link OutboxAppender},
 * a <b>dedicated</b> String-serializing {@link KafkaTemplate} (the payload is already JSON,
 * independent of the app's default template), and the {@link OutboxRelay} (scheduling enabled).
 */
@AutoConfiguration
@ConditionalOnClass({JdbcTemplate.class, KafkaTemplate.class})
@EnableScheduling
public class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OutboxAppender outboxAppender(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        return new OutboxAppender(jdbcTemplate, objectMapper);
    }

    /** Own producer: keys and values are Strings (payload is already serialized JSON). */
    @Bean
    public KafkaTemplate<String, String> outboxKafkaTemplate(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        ProducerFactory<String, String> factory = new DefaultKafkaProducerFactory<>(config);
        return new KafkaTemplate<>(factory);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxRelay outboxRelay(JdbcTemplate jdbcTemplate,
                                   KafkaTemplate<String, String> outboxKafkaTemplate,
                                   @Value("${fincore.outbox.batch-size:100}") int batchSize,
                                   @Value("${fincore.outbox.send-timeout-ms:5000}") long sendTimeoutMs) {
        return new OutboxRelay(jdbcTemplate, outboxKafkaTemplate, batchSize, sendTimeoutMs);
    }
}
