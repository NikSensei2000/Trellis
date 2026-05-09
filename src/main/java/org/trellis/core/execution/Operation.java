package org.trellis.core.execution;

/**
 * A single step within a broader {@link Workflow}.
 * <p>
 * {@code Operation} models “do one thing well”: persist an entity, enqueue an outbox message,
 * call a downstream port, and so on. Keeping steps as named operations improves testability and
 * reuse, and it aligns with SOLID’s single-responsibility principle at the orchestration layer.
 * </p>
 *
 * @param <I> input type
 * @param <O> output type on success
 */
@FunctionalInterface
public interface Operation<I, O> {

    /**
     * Executes the operation for {@code input}.
     *
     * @param input operation input
     * @return success with {@code O}, or failure with {@link DomainError}
     */
    Result<O, DomainError> execute(I input);
}
