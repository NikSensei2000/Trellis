package org.trellis.core.execution;

/**
 * Canonical Railway Oriented Programming (ROP) outcome for Trellis workflows.
 * <p>
 * {@code Result} models success and failure without exceptions for expected domain outcomes.
 * That separation matters in enterprise systems: it keeps control flow explicit, makes
 * compositional pipelines predictable, and allows cross-cutting infrastructure (such as the
 * transactional aspect) to react to domain failures in a uniform way.
 * </p>
 * <p>
 * The type is a Java {@code sealed} interface so all outcomes are known at compile time,
 * which enables exhaustive {@code switch} handling with pattern matching in Java 21.
 * </p>
 *
 * @param <T> successful value type
 * @param <E> error value type (in Trellis, commonly {@link DomainError})
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    /**
     * Creates a successful outcome wrapping {@code value}.
     * <p>
     * Factory methods are preferred over constructors for call-site readability in fluent
     * pipelines and for consistent generic inference.
     * </p>
     *
     * @param value successful payload; may be {@code null} if {@code T} allows it
     * @param <T>   success type
     * @param <E>   error type
     * @return a {@link Success}
     */
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    /**
     * Creates a failed outcome wrapping {@code error}.
     *
     * @param error domain or technical error description
     * @param <T>   success type
     * @param <E>   error type
     * @return a {@link Failure}
     */
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    /**
     * Successful branch of a {@link Result}.
     * <p>
     * Modeled as a {@link java.lang.Record record} for immutability, structural equality, and
     * transparent decomposition in pattern matching.
     * </p>
     *
     * @param value successful payload
     * @param <T>   success type
     * @param <E>   error type (present for uniform {@link Result} typing)
     */
    record Success<T, E>(T value) implements Result<T, E> {
    }

    /**
     * Failed branch of a {@link Result}.
     *
     * @param error error payload
     * @param <T>   success type (present for uniform {@link Result} typing)
     * @param <E>   error type
     */
    record Failure<T, E>(E error) implements Result<T, E> {
    }
}
