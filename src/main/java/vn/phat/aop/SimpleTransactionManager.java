package vn.phat.aop;

import vn.phat.annotation.Bean;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fake in-memory {@link TransactionManager} that prints coloured console
 * output so the AOP demo is easy to follow without a real database.
 *
 * The nesting counter simulates "current transaction" for REQUIRED propagation.
 */
@Bean("transactionManager")
public class SimpleTransactionManager implements TransactionManager {

    // ANSI colour codes for pretty console output
    private static final String RESET  = "\u001B[0m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED    = "\u001B[31m";
    private static final String CYAN   = "\u001B[36m";
    private static final String BOLD   = "\u001B[1m";

    /** Simulates whether a transaction is currently active on this thread. */
    private final ThreadLocal<TransactionStatus> currentTx = new ThreadLocal<>();
    private final AtomicInteger txCounter = new AtomicInteger(0);

    @Override
    public TransactionStatus begin(String operationName) {
        TransactionStatus existing = currentTx.get();

        // Re-use existing transaction if REQUIRED propagation already active
        if (existing != null && !existing.isCompleted()) {
            String txId = existing.getId();
            System.out.printf("%s%s[TX %s] Joining existing transaction for: %s%s%n",
                    BOLD, CYAN, txId, operationName, RESET);
            return existing;  // return same status – do not open new tx
        }

        String txId = "TX-" + txCounter.incrementAndGet();
        TransactionStatus status = new TransactionStatus(txId, operationName);
        currentTx.set(status);

        System.out.printf("%n%s%s[%s] ─── BEGIN TRANSACTION ──────────────────────%s%n",
                BOLD, GREEN, txId, RESET);
        System.out.printf("%s[%s] Operation: %s%s%n", GREEN, txId, operationName, RESET);

        return status;
    }

    @Override
    public void commit(TransactionStatus status) {
        if (status.isCompleted()) {
            // Was a "join" – nothing to commit yet
            return;
        }
        status.markCompleted();
        currentTx.remove();

        System.out.printf("%s%s[%s] ─── COMMIT ─────────────────────────────────%s%n%n",
                BOLD, GREEN, status.getId(), RESET);
    }

    @Override
    public void rollback(TransactionStatus status) {
        if (status.isRolledBack()) return;
        status.markRolledBack();
        currentTx.remove();

        System.out.printf("%s%s[%s] ─── ROLLBACK ────────────────────────────────%s%n%n",
                BOLD, RED, status.getId(), RESET);
    }

    /** Returns the active transaction on the current thread, or null. */
    public TransactionStatus getCurrent() {
        return currentTx.get();
    }
}
