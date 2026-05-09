package org.trellis.core.event;

import org.trellis.core.execution.DomainError;
import org.trellis.core.execution.Result;

/**
 * Consumes {@link Event} instances with explicit acknowledgement via {@link CommitToken}.
 * <p>
 * Subscribers should return {@link Result#success} with {@code null} for {@code Void}
 * on success, or {@link Result#failure} when the message cannot be processed. The
 * surrounding adapter decides how failures map to {@link CommitToken#reject()}.
 * </p>
 *
 * @param <T> consumed payload type
 */
@FunctionalInterface
public interface EventSubscriber<T> {

    /**
     * Handles {@code event}.
     *
     * @param event inbound event
     * @param token acknowledgement handle for the underlying consumer
     * @return {@link Result} describing processing outcome
     */
    Result<Void, DomainError> onEvent(Event<T> event, CommitToken token);
}
