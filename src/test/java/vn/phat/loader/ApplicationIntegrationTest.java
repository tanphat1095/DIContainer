package vn.phat.loader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.phat.Main;
import vn.phat.aop.TransactionManager;
import vn.phat.container.BeanFactory;
import vn.phat.demo.AccountService;
import vn.phat.demo.BankService;
import vn.phat.beans.FirstBean;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationIntegrationTest {

    @BeforeEach
    void resetApplication() throws Exception {
        Field field = Application.class.getDeclaredField("beanFactory");
        field.setAccessible(true);
        field.set(null, null);
    }

    @Test
    void testApplicationBootstrap() {
        BeanFactory beanFactory = Application.run(Main.class);

        assertNotNull(beanFactory);
        assertNotNull(beanFactory.getDeclaredBeans());
        assertTrue(beanFactory.getDeclaredBeans().size() > 0);
    }

    @Test
    void testApplicationBootstrap_FirstBeanWired() {
        BeanFactory beanFactory = Application.run(Main.class);

        FirstBean firstBean = beanFactory.getBean(FirstBean.class);
        assertNotNull(firstBean);
        assertNotNull(firstBean.getSecondBean());
    }

    @Test
    void testApplicationBootstrap_AccountServiceAvailable() {
        BeanFactory beanFactory = Application.run(Main.class);

        AccountService accountService = beanFactory.getBean(AccountService.class);
        assertNotNull(accountService);
    }

    @Test
    void testApplicationBootstrap_BankServiceAvailable() {
        BeanFactory beanFactory = Application.run(Main.class);

        BankService bankService = beanFactory.getBean(BankService.class);
        assertNotNull(bankService);
    }

    @Test
    void testApplicationBootstrap_TransactionManagerRegistered() {
        BeanFactory beanFactory = Application.run(Main.class);

        TransactionManager transactionManager = beanFactory.getBean(TransactionManager.class);
        assertNotNull(transactionManager);
    }

    @Test
    void testApplicationBootstrap_SingletonBehavior() {
        BeanFactory beanFactory = Application.run(Main.class);

        FirstBean bean1 = beanFactory.getBean(FirstBean.class);
        FirstBean bean2 = beanFactory.getBean(FirstBean.class);

        assertSame(bean1, bean2);
    }

    @Test
    void testApplicationBootstrap_InterfaceResolution() {
        BeanFactory beanFactory = Application.run(Main.class);

        AccountService accountService = beanFactory.getBean(AccountService.class);
        assertNotNull(accountService);
        assertDoesNotThrow(() -> accountService.getBalance("ACC-001"));
    }

    @Test
    void testApplicationBootstrap_MultipleBeansRegistered() {
        BeanFactory beanFactory = Application.run(Main.class);

        int declaredBeans = beanFactory.getDeclaredBeans().size();
        assertTrue(declaredBeans >= 3, "Should have at least 3 beans registered");
    }

    @Test
    void testApplicationBootstrap_BeansNotNull() {
        BeanFactory beanFactory = Application.run(Main.class);

        for (String beanName : beanFactory.getDeclaredBeans()) {
            Object bean = beanFactory.getBean(beanName);
            assertNotNull(bean, "Bean " + beanName + " should not be null");
        }
    }

    @Test
    void testApplicationBootstrap_ProxiesCreated() {
        BeanFactory beanFactory = Application.run(Main.class);

        AccountService accountService = beanFactory.getBean(AccountService.class);
        assertTrue(java.lang.reflect.Proxy.isProxyClass(accountService.getClass()),
                   "AccountService should be wrapped with a proxy for transactional support");
    }

    @Test
    void testApplicationBootstrap_DepositOperation() {
        BeanFactory beanFactory = Application.run(Main.class);

        AccountService accountService = beanFactory.getBean(AccountService.class);
        assertDoesNotThrow(() -> accountService.deposit("ACC-TEST", 100.0));
    }
}
