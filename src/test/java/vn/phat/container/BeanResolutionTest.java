package vn.phat.container;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.phat.annotation.Bean;
import vn.phat.annotation.Autowired;

import vn.phat.exception.AmbiguousBeanException;
import vn.phat.exception.BeanResolutionException;

import static org.junit.jupiter.api.Assertions.*;

class BeanResolutionTest {

    private BeanFactoryImpl beanFactory;

    @BeforeEach
    void setUp() {
        beanFactory = new BeanFactoryImpl();
    }

    @Test
    void testGetBeanByClass() {
        beanFactory.registerBean(SimpleBeanA.class, null);

        SimpleBeanA bean = beanFactory.getBean(SimpleBeanA.class);
        assertNotNull(bean);
    }

    @Test
    void testGetBeanByName() {
        beanFactory.registerBean(SimpleBeanA.class, null);

        Object bean = beanFactory.getBean("simpleBeanA");
        assertNotNull(bean);
        assertTrue(bean instanceof SimpleBeanA);
    }

    @Test
    void testGetBeanByInterface() {
        beanFactory.registerBean(InterfaceImplA.class, null);

        TestInterface bean = beanFactory.getBean(TestInterface.class);
        assertNotNull(bean);
        assertTrue(bean instanceof InterfaceImplA);
    }

    @Test
    void testGetBeanNotFound() {
        assertThrows(BeanResolutionException.class, () -> beanFactory.getBean(SimpleBeanA.class));
    }

    @Test
    void testGetDeclaredBeans() {
        beanFactory.registerBean(SimpleBeanA.class, null);
        beanFactory.registerBean(SimpleBeanB.class, null);

        var declaredBeans = beanFactory.getDeclaredBeans();
        assertTrue(declaredBeans.size() >= 2);
    }

    @Test
    void testGetBeanWithCustomName() {
        beanFactory.registerBean(CustomNameBean.class, null);

        Object bean = beanFactory.getBean("myCustomBean");
        assertNotNull(bean);
    }

    @Test
    void testGetBeanWithDefaultName() {
        beanFactory.registerBean(SimpleBeanA.class, null);

        Object bean = beanFactory.getBean("simpleBeanA");
        assertNotNull(bean);
    }

    @Test
    void testGetBeanAmbiguous_ThrowsException() {
        beanFactory.registerBean(InterfaceImplA.class, null);
        beanFactory.registerBean(InterfaceImplB.class, null);

        assertThrows(AmbiguousBeanException.class, () -> beanFactory.getBean(TestInterface.class));
    }

    @Test
    void testGetBeanMultipleImplementations() {
        beanFactory.registerBean(InterfaceImplA.class, null);
        beanFactory.registerBean(InterfaceImplB.class, null);

        var beans = beanFactory.getDeclaredBeans();
        assertTrue(beans.size() >= 2);
    }

    @Test
    void testGetBeanWithDependency() {
        beanFactory.registerBean(SimpleBeanB.class, null);
        beanFactory.registerBean(DependentBean.class, null);

        DependentBean bean = beanFactory.getBean(DependentBean.class);
        assertNotNull(bean);
    }

    @Test
    void testRegisterBeanByName() {
        SimpleBeanA bean = new SimpleBeanA();
        beanFactory.registerBeanByName("customName", bean);

        Object retrieved = beanFactory.getBean("customName");
        assertSame(bean, retrieved);
    }

    @Test
    void testRegisterBeanByName_NullNameThrows() {
        SimpleBeanA bean = new SimpleBeanA();

        assertThrows(IllegalArgumentException.class, () ->
            beanFactory.registerBeanByName(null, bean)
        );
    }

    @Test
    void testRegisterBeanByName_BlankNameThrows() {
        SimpleBeanA bean = new SimpleBeanA();

        assertThrows(IllegalArgumentException.class, () ->
            beanFactory.registerBeanByName("   ", bean)
        );
    }

    interface TestInterface {
        void doSomething();
    }

    @Bean
    static class SimpleBeanA {
    }

    @Bean
    static class SimpleBeanB {
    }

    @Bean("myCustomBean")
    static class CustomNameBean {
    }

    @Bean
    static class InterfaceImplA implements TestInterface {
        @Override
        public void doSomething() {
        }
    }

    @Bean
    static class InterfaceImplB implements TestInterface {
        @Override
        public void doSomething() {
        }
    }

    @Bean
    static class DependentBean {
        @Autowired
        public DependentBean(SimpleBeanB dependency) {
        }
    }
}
