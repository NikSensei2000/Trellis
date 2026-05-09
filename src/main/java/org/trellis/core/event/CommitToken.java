package org.trellis.core.event;

/**
 * Acknowledgement contract for at-least-once messaging consumers.
 * <p>
 * Many brokers expose manual commit semantics: a subscriber processes a message, then confirms
 * or rejects delivery. {@code CommitToken} abstracts that capability so domain code can remain
 * broker-agnostic while still participating in correct offset/commit management.
 * </p>
 * <p>
 * Implementations should document whether {@link #commit()} and {@link #reject()} are idempotent
 * and whether they may throw if the underlying consumer is closed.
 * </p>
 */
public interface CommitToken {

    /**
     * Signals successful processing; the infrastructure should acknowledge the message.
     */
    void commit();

    /**
     * Signals unsuccessful processing; the infrastructure should negative-acknowledge or route
     * the message according to policy.
     */
    void reject();
}
