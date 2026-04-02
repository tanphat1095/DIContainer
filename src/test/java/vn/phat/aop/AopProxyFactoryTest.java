package vn.phat.aop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import vn.phat.annotation.Transactional;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AopProxyFactoryTest {

    private TransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        transactionManager = mock(TransactionManager.class);
        when(transactionManager.begin(anyString())).thenReturn(new TransactionStatus("TX-TEST", "test"));
    }

    @Test
    void testWrapIfTransactional_ClassLevelAnnotation() {
        TransactionalService service = new TransactionalService();
        Object proxy = AopProxyFactory.wrapIfTransactional(service, transactionManager);

        assertNotNull(proxy);
        assertTrue(Proxy.isProxyClass(proxy.getClass()));
        assertTrue(proxy instanceof TransactionalServiceInterface);
    }

    @Test
    void testWrapIfTransactional_MethodLevelAnnotation() {
        MethodLevelTransactionalService service = new MethodLevelTransactionalService();
        Object proxy = AopProxyFactory.wrapIfTransactional(service, transactionManager);

        assertNotNull(proxy);
        assertTrue(Proxy.isProxyClass(proxy.getClass()));
    }

    @Test
    void testWrapIfTransactional_NoAnnotation() {
        NonTransactionalService service = new NonTransactionalService();
        Object proxy = AopProxyFactory.wrapIfTransactional(service, transactionManager);

        assertSame(service, proxy);
        assertFalse(Proxy.isProxyClass(proxy.getClass()));
    }

    @Test
    void testWrapIfTransactional_NoInterface() {
        NoInterfaceService service = new NoInterfaceService();
        Object proxy = AopProxyFactory.wrapIfTransactional(service, transactionManager);

        assertSame(service, proxy);
    }

    @Test
    void testProxyInvokesInterceptor() {
        TransactionalService service = new TransactionalService();
        TransactionalServiceInterface proxy = (TransactionalServiceInterface) AopProxyFactory.wrapIfTransactional(service, transactionManager);

        proxy.doSomething("test");

        verify(transactionManager, atLeast(1)).begin(anyString());
    }

    @Test
    void testProxyInvokesMethodLevelAnnotation() {
        MethodLevelTransactionalService service = new MethodLevelTransactionalService();
        MethodLevelInterface proxy = (MethodLevelInterface) AopProxyFactory.wrapIfTransactional(service, transactionManager);

        proxy.transactionalMethod();

        verify(transactionManager, atLeast(1)).begin(anyString());
    }

    interface TransactionalServiceInterface {
        void doSomething(String arg);
    }

    @Transactional
    static class TransactionalService implements TransactionalServiceInterface {
        @Override
        public void doSomething(String arg) {
        }
    }

    interface MethodLevelInterface {
        void transactionalMethod();
        void nonTransactionalMethod();
    }

    static class MethodLevelTransactionalService implements MethodLevelInterface {
        @Override
        @Transactional
        public void transactionalMethod() {
        }

        @Override
        public void nonTransactionalMethod() {
        }
    }

    static class NonTransactionalService implements TransactionalServiceInterface {
        @Override
        public void doSomething(String arg) {
        }
    }

    @Transactional
    static class NoInterfaceService {
        void doSomething(String arg) {
        }
    }
}
