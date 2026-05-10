package com.github.niksensei2000.trellis.execution;

/**
 * Coarse-grained classification for {@link DomainError} instances.
 * <p>
 * Categories exist so infrastructure layers (HTTP mapping, observability, retry policies) can
 * make decisions without parsing free-form messages. They are intentionally small: they describe
 * <em>how the failure should be handled</em>, not every possible business rule.
 * </p>
 */
public enum ErrorCategory {

    /**
     * Input violated invariants, schema constraints, or business validation rules.
     */
    VALIDATION,

    /**
     * A referenced aggregate or resource does not exist.
     */
    NOT_FOUND,

    /**
     * The caller is not allowed to perform the operation.
     */
    UNAUTHORIZED,

    /**
     * The operation cannot proceed due to a state mismatch (for example, duplicate keys or
     * illegal transitions).
     */
    CONFLICT,

    /**
     * Unexpected failure: downstream outages, serialization faults, or other non-domain errors.
     */
    SYSTEM
}
