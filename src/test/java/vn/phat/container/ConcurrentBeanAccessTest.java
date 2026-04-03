package vn.phat.container;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.phat.annotation.Bean;
import vn.phat.annotation.Autowired;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentBeanAccessTest {

    private BeanFactoryImpl beanFactory;

    @BeforeEach
    void setUp() {
        beanFactory = new BeanFactoryImpl();
        beanFactory.registerBean(ThreadSafeBean.class, null);
        beanFactory.registerBean(SimpleBean.class, null);
    }

    @Test
    void testConcurrentBeanAccess_SingletonBehavior() throws InterruptedException {
        int threadCount = 10;
        List<ThreadSafeBean> beans = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                ThreadSafeBean bean = beanFactory.getBean(ThreadSafeBean.class);
                beans.add(bean);
                latch.countDown();
            }).start();
        }

        latch.await();

        ThreadSafeBean firstBean = beans.get(0);
        for (ThreadSafeBean bean : beans) {
            assertSame(firstBean, bean);
        }
    }

    @Test
    void testConcurrentBeanAccess_AllThreadsGetBean() throws InterruptedException {
        int threadCount = 20;
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                ThreadSafeBean bean = beanFactory.getBean(ThreadSafeBean.class);
                if (bean != null) {
                    successCount.incrementAndGet();
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(threadCount, successCount.get());
    }

    @Test
    void testConcurrentBeanAccess_MultipleBeansSequential() throws InterruptedException {
        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                ThreadSafeBean bean1 = beanFactory.getBean(ThreadSafeBean.class);
                SimpleBean bean2 = beanFactory.getBean(SimpleBean.class);
                assertNotNull(bean1);
                assertNotNull(bean2);
                latch.countDown();
            }).start();
        }

        latch.await();
    }

    @Test
    void testConcurrentBeanAccess_NoDataRaces() throws InterruptedException {
        int threadCount = 15;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                ThreadSafeBean bean = beanFactory.getBean(ThreadSafeBean.class);
                assertNotNull(bean);
                endLatch.countDown();
            }).start();
        }

        startLatch.countDown();
        endLatch.await();
    }

    @Test
    void testConcurrentBeanAccess_DeclaredBeans() throws InterruptedException {
        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                var beans = beanFactory.getDeclaredBeans();
                assertNotNull(beans);
                assertTrue(beans.size() > 0);
                latch.countDown();
            }).start();
        }

        latch.await();
    }

    @Test
    void testConcurrentBeanAccess_InterleavedAccess() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                if (threadId % 2 == 0) {
                    ThreadSafeBean bean = beanFactory.getBean(ThreadSafeBean.class);
                    assertNotNull(bean);
                } else {
                    SimpleBean bean = beanFactory.getBean(SimpleBean.class);
                    assertNotNull(bean);
                }
                latch.countDown();
            }).start();
        }

        latch.await();
    }

    @Test
    void testConcurrentBeanAccess_HighContention() throws InterruptedException {
        int threadCount = 50;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                ThreadSafeBean bean = beanFactory.getBean(ThreadSafeBean.class);
                assertNotNull(bean);
                latch.countDown();
            }).start();
        }

        latch.await();
    }

    @Bean
    static class ThreadSafeBean {
        private static final AtomicInteger instanceCount = new AtomicInteger(0);

        public ThreadSafeBean() {
            instanceCount.incrementAndGet();
        }

        public static int getInstanceCount() {
            return instanceCount.get();
        }

        public static void resetCount() {
            instanceCount.set(0);
        }
    }

    @Bean
    static class SimpleBean {
    }
}
