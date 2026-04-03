package vn.phat.aop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.phat.annotation.Transactional;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class TransactionalPropagationTest {

    private SimpleTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        transactionManager = new SimpleTransactionManager();
    }

    // ── helper ────────────────────────────────────────────────────────────

    private TestService createProxy() {
        TestServiceImpl service = new TestServiceImpl();
        return (TestService) Proxy.newProxyInstance(
            TestService.class.getClassLoader(),
            new Class[]{TestService.class},
            new TransactionalInterceptor(service, transactionManager)
        );
    }

    // ── tests ─────────────────────────────────────────────────────────────

    @Test
    void testPropagation_REQUIRED_JoinsExistingTransaction() {
        TestService proxy = createProxy();

        TransactionStatus tx1 = transactionManager.begin("first");
        proxy.requiredOperation();
        proxy.requiredOperation();

        assertEquals(tx1, transactionManager.getCurrent());
        transactionManager.commit(tx1);
    }

    @Test
    void testPropagation_REQUIRED_CreatesNewTransaction() {
        TestService proxy = createProxy();

        proxy.requiredOperation();

        assertNull(transactionManager.getCurrent());
    }

    @Test
    void testPropagation_REQUIRES_NEW_CreatesNewTransaction() {
        TestService proxy = createProxy();

        TransactionStatus tx1 = transactionManager.begin("outer");
        String tx1Id = tx1.getId();

        proxy.requiresNewOperation();

        TransactionStatus currentTx = transactionManager.getCurrent();
        if (currentTx != null) {
            assertNotEquals(tx1Id, currentTx.getId());
        }

        transactionManager.commit(tx1);
    }

    @Test
    void testPropagation_NestedREQUIRED() {
        TestService proxy = createProxy();

        TransactionStatus tx1 = transactionManager.begin("outer");
        proxy.requiredOperation();

        assertEquals(tx1, transactionManager.getCurrent());
        transactionManager.commit(tx1);
    }

    @Test
    void testPropagation_CommitOnREQUIRED() {
        TestService proxy = createProxy();

        proxy.requiredOperation();

        assertNull(transactionManager.getCurrent());
    }

    @Test
    void testPropagation_RollbackOnREQUIRED_Exception() {
        TestService proxy = createProxy();

        assertThrows(RuntimeException.class, proxy::failingRequired);
        assertNull(transactionManager.getCurrent());
    }

    // ── test doubles ──────────────────────────────────────────────────────

    interface TestService {
        void requiredOperation();
        void requiresNewOperation();
        void failingRequired();
    }

    static class TestServiceImpl implements TestService {
        @Override
        @Transactional(propagation = Transactional.Propagation.REQUIRED)
        public void requiredOperation() {}

        @Override
        @Transactional(propagation = Transactional.Propagation.REQUIRES_NEW)
        public void requiresNewOperation() {}

        @Override
        @Transactional(propagation = Transactional.Propagation.REQUIRED)
        public void failingRequired() {
            throw new RuntimeException("Operation failed");
        }
    }
}
