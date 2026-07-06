# platform-outbox

Transactional-outbox scaffolding (FR-25): publish domain events to Kafka **reliably**, with
no chance of an event being lost or duplicated relative to committed database state.

## How it works

1. Inside your `@Transactional` state change, call `OutboxAppender.append(...)`. It inserts
   an `outbox` row on the **same DataSource**, so it commits atomically with your change.
2. `OutboxRelay` polls unpublished rows (oldest-first, all tenants) and publishes each to
   Kafka via a dedicated String-serializing `KafkaTemplate`, marking rows published only
   after Kafka confirms.

It uses `JdbcTemplate` (not a JPA entity) on purpose: the relay reads **across tenants**, so
the outbox must not be subject to Hibernate's per-tenant filtering ([ADR-0009]).

## Using it in a service

1. `implementation project(':platform-outbox')` — the appender, relay, and dedicated
   `KafkaTemplate` are auto-configured (needs a DataSource + Kafka on the classpath).
2. Add the `outbox` table to the service's Flyway migrations — copy
   [`src/main/resources/db/outbox.sql`](src/main/resources/db/outbox.sql) into a
   `Vn__create_outbox.sql`.
3. Append from your domain code:

```java
outboxAppender.append(tenantId, "ledger.entry.posted", entryId.toString(),
                      "ledger.entry.posted", eventPayload);
```

## Config

| Property | Default | Meaning |
|----------|---------|---------|
| `fincore.outbox.poll-interval-ms` | `1000` | Relay polling interval |
| `fincore.outbox.batch-size` | `100` | Rows published per tick |
| `fincore.outbox.send-timeout-ms` | `5000` | Per-message Kafka send timeout |

_Relay mechanism is polling (self-hosted / air-gapped friendly); CDC (Debezium) can replace
the relay later without changing producers._
