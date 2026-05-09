package org.trellis.core.outbox;

/**
 * Persistence port for {@link OutboxEvent} records.
 * <p>
 * Trellis keeps this interface small on purpose: different deployments will map it to JPA,
 * JDBC, jOOQ, or other storage technologies. The critical invariant is transactional atomicity
 * with the surrounding business mutation when used inside a {@link org.springframework.transaction.annotation.Transactional}
 * boundary.
 * </p>
 */
public interface OutboxStore {

    /**
     * Persists {@code event} in the same transaction as the caller’s unit of work.
     * <p>
     * Implementations should treat this as an append-mostly write: duplicates are typically
     * prevented by unique constraints or idempotent dispatchers upstream.
     * </p>
     *
     * @param event event to persist; must not be {@code null}
     */
    void save(OutboxEvent event);
}
