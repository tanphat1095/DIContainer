package vn.phat.aop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleTransactionManagerTest {

    private SimpleTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        transactionManager = new SimpleTransactionManager();
    }

    @Test
    void testBeginTransaction() {
        TransactionStatus status = transactionManager.begin("test-operation");

        assertNotNull(status);
        assertFalse(status.isCompleted());
        assertFalse(status.isRolledBack());
    }

    @Test
    void testBeginTransaction_WithName() {
        TransactionStatus status = transactionManager.begin("deposit");

        assertNotNull(status);
        assertTrue(status.getId().startsWith("TX-"));
    }

    @Test
    void testCommitTransaction() {
        TransactionStatus status = transactionManager.begin("test");
        transactionManager.commit(status);

        assertTrue(status.isCompleted());
    }

    @Test
    void testRollbackTransaction() {
        TransactionStatus status = transactionManager.begin("test");
        transactionManager.rollback(status);

        assertTrue(status.isRolledBack());
    }

    @Test
    void testJoinExistingTransaction() {
        TransactionStatus first = transactionManager.begin("first");
        TransactionStatus second = transactionManager.begin("second");

        assertSame(first, second);
        assertFalse(second.isCompleted());
    }

    @Test
    void testGetCurrentTransaction() {
        assertNull(transactionManager.getCurrent());

        TransactionStatus status = transactionManager.begin("test");
        assertEquals(status, transactionManager.getCurrent());

        transactionManager.commit(status);
        assertNull(transactionManager.getCurrent());
    }

    @Test
    void testRollbackDoesNotAffectIfAlreadyCompleted() {
        TransactionStatus status = transactionManager.begin("test");
        transactionManager.commit(status);
        transactionManager.rollback(status);

        assertTrue(status.isCompleted());
    }

    @Test
    void testSequentialTransactions() {
        TransactionStatus first = transactionManager.begin("first");
        transactionManager.commit(first);

        TransactionStatus second = transactionManager.begin("second");
        transactionManager.commit(second);

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void testRollbackClearsCurrentTransaction() {
        TransactionStatus status = transactionManager.begin("test");
        transactionManager.rollback(status);

        assertNull(transactionManager.getCurrent());
    }

    @Test
    void testCommitClearsCurrentTransaction() {
        TransactionStatus status = transactionManager.begin("test");
        transactionManager.commit(status);

        assertNull(transactionManager.getCurrent());
    }
}
