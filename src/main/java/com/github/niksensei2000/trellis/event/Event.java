package com.github.niksensei2000.trellis.event;

import java.time.Instant;
import java.util.Map;

/**
 * Message envelope for publish/subscribe integrations.
 * <p>
 * {@code Event} wraps a strongly typed {@code payload} with routing and observability metadata.
 * The {@code eventId} enables idempotent consumers; {@code aggregateId} ties the message to a
 * domain aggregate for ordering and consistency discussions; {@code timestamp} supports lag
 * metrics; {@code metadata} carries cross-cutting identifiers.
 * </p>
 *
 * @param eventId      unique identifier for this message instance
 * @param aggregateId  domain aggregate identifier
 * @param payload      typed payload
 * @param timestamp    when the event was published or materially occurred
 * @param metadata     auxiliary attributes (tracing, tenancy, feature flags)
 * @param <T>          payload type
 */
public record Event<T>(
        String eventId,
        String aggregateId,
        T payload,
        Instant timestamp,
        Map<String, String> metadata
) {
}
