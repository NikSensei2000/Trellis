package org.trellis.core.execution;

/**
 * A composable unit of work that accepts an input and returns a {@link Result}.
 * <p>
 * {@code Workflow} is the orchestration-oriented counterpart to {@link Operation}: it typically
 * coordinates multiple steps (validation, persistence, outbox emission, integration calls)
 * while keeping the overall contract explicit and exception-safe for expected failures.
 * </p>
 * <p>
 * Implementations should prefer returning {@link Result#failure} for expected
 * domain failures rather than throwing, so transactional boundaries and aspects can behave
 * consistently.
 * </p>
 *
 * @param <I> input type
 * @param <R> result type on success
 */
@FunctionalInterface
public interface Workflow<I, R> {

    /**
     * Executes the workflow for {@code input}.
     *
     * @param input workflow input; must not be {@code null} unless the workflow explicitly allows it
     * @return success with {@code R}, or failure with {@link DomainError}
     */
    Result<R, DomainError> execute(I input);
}
