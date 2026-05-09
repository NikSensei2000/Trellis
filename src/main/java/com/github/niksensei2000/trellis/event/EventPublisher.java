package com.github.niksensei2000.trellis.event;

import com.github.niksensei2000.trellis.execution.DomainError;
import com.github.niksensei2000.trellis.execution.Result;

/**
 * Publishes {@link Event} instances to a logical {@code channel}.
 * <p>
 * The channel name is a deployment concern (topic, exchange, queue prefix). Returning
 * {@link Result} allows publishers to surface mapping and serialization failures without
 * throwing for expected error paths.
 * </p>
 */
public interface EventPublisher {

    /**
     * Publishes {@code event} to {@code channel}.
     *
     * @param channel target channel identifier
     * @param event   event envelope
     * @param <T>     payload type
     * @return success with a broker-specific identifier when available, otherwise a stable id, or
     * failure with {@link DomainError}
     */
    <T> Result<String, DomainError> publish(String channel, Event<T> event);
}
