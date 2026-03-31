package vn.phat.aop;

/**
 * Minimal transaction-manager abstraction used by the AOP proxy.
 * Real implementations would wrap JDBC Connection / JPA EntityManager /
 * etc.  This version just prints to stdout so the demo output is visible
 * without any database dependency.
 */
public interface TransactionManager {

    /** Begin a new transaction and return a handle. */
    TransactionStatus begin(String operationName);

    /** Commit the active transaction. */
    void commit(TransactionStatus status);

    /** Roll back the active transaction. */
    void rollback(TransactionStatus status);
}
