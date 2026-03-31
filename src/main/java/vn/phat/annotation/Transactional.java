package vn.phat.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method (or all methods of a class) to run inside a transaction.
 * When the IOC container detects this annotation it wraps the bean with a
 * JDK dynamic proxy. Before each annotated method the proxy opens a
 * transaction; after successful execution it commits; on any exception it
 * rolls back and re-throws.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Transactional {

    /**
     * Propagation behaviour (simplified subset – for demo purposes).
     */
    Propagation propagation() default Propagation.REQUIRED;

    /**
     * Whether to roll back on checked exceptions as well (default: only
     * unchecked / RuntimeException).
     */
    boolean rollbackOnCheckedException() default false;

    enum Propagation {
        /** Use current transaction or open a new one. */
        REQUIRED,
        /** Always open a new transaction, suspend the current one. */
        REQUIRES_NEW
    }
}
