package com.github.niksensei2000.trellis.execution;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Bridges Spring’s declarative transactions with Trellis {@link Result} semantics.
 * <p>
 * In Railway Oriented Programming, domain failures are often modeled as {@link Result.Failure}
 * rather than exceptions. Spring’s transaction interceptor, by default, rolls back on unchecked
 * exceptions and {@code Error}, but it will <em>not</em> automatically roll back when a method
 * returns normally with a failure outcome.
 * </p>
 * <p>
 * This aspect executes <em>around</em> methods annotated with Spring’s {@link org.springframework.transaction.annotation.Transactional}.
 * After the join point returns, it inspects the return value using Java&nbsp;21 pattern matching.
 * If the value is a {@link Result.Failure} and a transaction is active, it marks the transaction
 * for rollback only, preserving the original {@link Result} as the method outcome.
 * </p>
 * <p>
 * Ordering is set to {@link Ordered#LOWEST_PRECEDENCE} {@code - 1} as required by Trellis so
 * this advice participates in the same ordering region as other infrastructure advisors. Teams
 * should validate ordering against their specific advisor graph if they introduce additional
 * transactional or security aspects.
 * </p>
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class TransactionalResultAspect {

    /**
     * Creates the aspect bean. Spring calls this constructor when registering components.
     */
    public TransactionalResultAspect() {
    }

    /**
     * Wraps {@code @Transactional} methods to align commit/rollback behavior with {@link Result}.
     * <p>
     * The pointcut targets the Spring {@code @Transactional} annotation directly so it applies
     * consistently to interface-based proxies and class-based proxies, provided the annotation
     * is visible to Spring’s transaction infrastructure.
     * </p>
     *
     * @param joinPoint active join point for the transactional method
     * @return the method’s return value, unmodified
     * @throws Throwable propagates any exception thrown by the join point; transaction rules for
     *                   exceptions remain governed by Spring’s rollback rules
     */
    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object handleResultRollback(ProceedingJoinPoint joinPoint) throws Throwable {
        Object outcome = joinPoint.proceed();
        if (outcome instanceof Result<?, ?> result) {
            switch (result) {
                case Result.Failure<?, ?> failure -> maybeRollbackForFailure(joinPoint, failure);
                case Result.Success<?, ?> ignored -> {
                    // Success does not influence transaction outcome beyond normal Spring rules.
                }
            }
        }
        return outcome;
    }

    private void maybeRollbackForFailure(ProceedingJoinPoint joinPoint, Result.Failure<?, ?> failure) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        log.warn(
                "Marked transaction rollback-only due to Result.Failure from {}.{} error={}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                failure.error()
        );
    }
}
