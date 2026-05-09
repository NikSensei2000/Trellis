package com.github.niksensei2000.trellis.execution;

/**
 * Canonical domain-level error for Trellis workflows and operations.
 * <p>
 * {@code DomainError} is deliberately small and stable: a machine-oriented {@code code}, a
 * human-oriented {@code message}, and an {@link ErrorCategory} for policy decisions. This
 * keeps errors portable across layers (application, integration, messaging) while remaining easy
 * to log and to translate to transport-specific responses.
 * </p>
 *
 * @param code     stable identifier (for example {@code "incident.title.required"})
 * @param message  human-readable explanation safe for operator logs
 * @param category routing hint for cross-cutting handlers
 */
public record DomainError(String code, String message, ErrorCategory category) {
}
