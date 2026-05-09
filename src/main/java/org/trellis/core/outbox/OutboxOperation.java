package org.trellis.core.outbox;

import org.trellis.core.execution.DomainError;
import org.trellis.core.execution.Result;

/**
 * Produces an {@link OutboxEvent} from an input without persisting it.
 * <p>
 * Splitting “build event” from {@link OutboxStore#save(OutboxEvent)} supports composition: a
 * workflow can validate, map, then persist, while tests can assert on the generated event shape
 * without touching storage. This follows the dependency inversion principle: orchestration code
 * depends on abstractions ({@link OutboxOperation}, {@link OutboxStore}), not concrete adapters.
 * </p>
 *
 * @param <I> input type used to derive the outbox payload
 */
@FunctionalInterface
public interface OutboxOperation<I> {

    /**
     * Builds an {@link OutboxEvent} for {@code input}.
     *
     * @param input operation input
     * @return success with a ready-to-persist event, or a {@link DomainError}
     */
    Result<OutboxEvent, DomainError> execute(I input);
}
