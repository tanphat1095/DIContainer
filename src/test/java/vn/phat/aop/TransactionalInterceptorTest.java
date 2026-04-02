package vn.phat.aop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.phat.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionalInterceptorTest {

    private TransactionManager transactionManager;
    private TransactionStatus transactionStatus;

    @BeforeEach
    void setUp() {
        transactionManager = mock(TransactionManager.class);
        transactionStatus = new TransactionStatus("TX-TEST", "test-operation");
        when(transactionManager.begin(anyString())).thenReturn(transactionStatus);
    }

    @Test
    void testInterceptor_CommitsOnSuccess() throws Throwable {
        TestService service = new TestService();
        TransactionalInterceptor interceptor = new TransactionalInterceptor(service, transactionManager);

        java.lang.reflect.Method method = TestService.class.getMethod("successfulOperation");
        Object result = interceptor.invoke(null, method, new Object[]{});

        verify(transactionManager).begin(anyString());
        verify(transactionManager).commit(transactionStatus);
        verify(transactionManager, never()).rollback(any());
    }

    @Test
    void testInterceptor_RollsBackOnRuntimeException() throws Throwable {
        TestService service = new TestService();
        TransactionalInterceptor interceptor = new TransactionalInterceptor(service, transactionManager);

        java.lang.reflect.Method method = TestService.class.getMethod("failingOperation");

        assertThrows(RuntimeException.class, () ->
            interceptor.invoke(null, method, new Object[]{})
        );

        verify(transactionManager).begin(anyString());
        verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void testInterceptor_REQUIRED_Propagation() throws Throwable {
        TestService service = new TestService();
        TransactionStatus existingStatus = new TransactionStatus("TX-EXISTING", "existing");
        when(transactionManager.begin(anyString())).thenReturn(existingStatus);

        TransactionalInterceptor interceptor = new TransactionalInterceptor(service, transactionManager);
        java.lang.reflect.Method method = TestService.class.getMethod("successfulOperation");

        interceptor.invoke(null, method, new Object[]{});

        verify(transactionManager).begin(anyString());
    }

    @Test
    void testInterceptor_PassThroughWhenNoTransactional() throws Throwable {
        TestService service = new TestService();
        TransactionalInterceptor interceptor = new TransactionalInterceptor(service, transactionManager);

        java.lang.reflect.Method method = TestService.class.getMethod("nonTransactionalOperation");
        Object result = interceptor.invoke(null, method, new Object[]{});

        verify(transactionManager, never()).begin(anyString());
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void testInterceptor_ClassLevelTransactional() throws Throwable {
        ClassLevelTransactional service = new ClassLevelTransactional();
        TransactionalInterceptor interceptor = new TransactionalInterceptor(service, transactionManager);

        java.lang.reflect.Method method = ClassLevelTransactional.class.getMethod("anyMethod");
        interceptor.invoke(null, method, new Object[]{});

        verify(transactionManager).begin(anyString());
        verify(transactionManager).commit(any());
    }

    @Test
    void testInterceptor_RollbackOnCheckedException() throws Throwable {
        TestService service = new TestService();
        TransactionalInterceptor interceptor = new TransactionalInterceptor(service, transactionManager);

        java.lang.reflect.Method method = TestService.class.getMethod("checkedExceptionOperation");

        assertThrows(Exception.class, () ->
            interceptor.invoke(null, method, new Object[]{})
        );

        verify(transactionManager).rollback(any());
    }

    static class TestService {
        @Transactional
        public void successfulOperation() {
        }

        @Transactional
        public void failingOperation() {
            throw new RuntimeException("Operation failed");
        }

        @Transactional(rollbackOnCheckedException = true)
        public void checkedExceptionOperation() throws Exception {
            throw new Exception("Checked exception");
        }

        public void nonTransactionalOperation() {
        }
    }

    @Transactional
    static class ClassLevelTransactional {
        public void anyMethod() {
        }
    }
}
