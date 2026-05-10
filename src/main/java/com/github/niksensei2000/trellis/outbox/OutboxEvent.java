package com.github.niksensei2000.trellis.outbox;

import java.time.Instant;
import java.util.Map;

/**
 * Durable outbox record describing something that should be published reliably after commit.
 * <p>
 * The transactional outbox pattern avoids “dual write” problems: business data and the outbox row
 * persist in the same database transaction, and a separate dispatcher publishes to brokers or
 * HTTP endpoints. {@code OutboxEvent} is intentionally serialization-friendly: identifiers and
 * payloads are strings at the boundary, while {@code metadata} carries tracing and tenancy context.
 * </p>
 * <p>
 * {@code createdAt} supports ordering and observability; it should represent the instant the event
 * was logically created, typically assigned close to persistence time.
 * </p>
 *
 * @param aggregateId stable id of the aggregate that produced the event
 * @param eventType   versioned type name (for example {@code IncidentCreated.v1})
 * @param payload     serialized domain payload (commonly JSON)
 * @param metadata    auxiliary key/value data (correlation ids, tenant ids, causation ids)
 * @param createdAt   event creation timestamp
 */
public record OutboxEvent(
        String aggregateId,
        String eventType,
        String payload,
        Map<String, String> metadata,
        Instant createdAt
) {
}
