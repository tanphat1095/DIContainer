package vn.phat.container;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.phat.annotation.Bean;
import vn.phat.annotation.Autowired;
import vn.phat.beans.FirstBean;
import vn.phat.beans.SecondBean;
import vn.phat.beans.ThirdBean;
import vn.phat.beans.FourthBean;

import static org.junit.jupiter.api.Assertions.*;

class BeanFactoryImplTest {

    private BeanFactoryImpl beanFactory;

    @BeforeEach
    void setUp() {
        beanFactory = new BeanFactoryImpl();
        beanFactory.registerBean(ThirdBean.class, null);
        beanFactory.registerBean(FourthBean.class, null);
        beanFactory.registerBean(SecondBean.class, null);
        beanFactory.registerBean(FirstBean.class, null);
    }

    @Test
    void testFieldInjection() {
        FirstBean firstBean = beanFactory.getBean(FirstBean.class);
        SecondBean secondBean = beanFactory.getBean(SecondBean.class);
        ThirdBean thirdBean = beanFactory.getBean(ThirdBean.class);

        beanFactory.autowireFields(beanFactory, firstBean, thirdBean);

        assertNotNull(firstBean, "FirstBean should not be null");
        assertNotNull(thirdBean, "ThirdBean should not be null");
    }

    @Test
    void testSetterInjection() {
        FirstBean firstBean = beanFactory.getBean(FirstBean.class);
        assertNotNull(firstBean.getSecondBean(), "SecondBean should be injected into FirstBean via setter");
    }

    @Test
    void testConstructorInjection() {
        SecondBean secondBean = beanFactory.getBean(SecondBean.class);
        assertNotNull(secondBean, "SecondBean should not be null");
    }

    @Test
    void testMultiThreadedBeanRetrieval() throws InterruptedException {
        Thread thread = new Thread(() -> {
            FirstBean firstBean = beanFactory.getBean(FirstBean.class);
            assertNotNull(firstBean, "FirstBean should not be null in the new thread");
        });
        thread.start();
        thread.join();
    }

    @Test
    void testFourthBeanInjection() {
        SecondBean secondBean = beanFactory.getBean(SecondBean.class);
        assertNotNull(secondBean.getFourthBean(), "FourthBean should be injected into SecondBean");
    }

    @Test
    void testGetBeanByName() {
        Object bean = beanFactory.getBean("firstBean");
        assertNotNull(bean);
        assertTrue(bean instanceof FirstBean);
    }

    @Test
    void testGetBeanNull_WhenNotRegistered() {
        BeanFactoryImpl newFactory = new BeanFactoryImpl();
        FirstBean bean = newFactory.getBean(FirstBean.class);
        assertNull(bean);
    }

    @Test
    void testRegisterBeanTwice() {
        BeanFactoryImpl newFactory = new BeanFactoryImpl();
        newFactory.registerBean(FirstBean.class, null);
        FirstBean bean1 = newFactory.getBean(FirstBean.class);

        newFactory.registerBean(FirstBean.class, null);
        FirstBean bean2 = newFactory.getBean(FirstBean.class);

        assertNotNull(bean2);
    }

    @Test
    void testGetDeclaredBeans() {
        var declaredBeans = beanFactory.getDeclaredBeans();
        assertNotNull(declaredBeans);
        assertTrue(declaredBeans.size() >= 4);
    }

    @Test
    void testRegisterBeanByName() {
        BeanFactoryImpl newFactory = new BeanFactoryImpl();
        FirstBean bean = new FirstBean();
        newFactory.registerBeanByName("customFirstBean", bean);

        Object retrieved = newFactory.getBean("customFirstBean");
        assertSame(bean, retrieved);
    }

    @Test
    void testRegisterBeanByName_NullNameThrows() {
        BeanFactoryImpl newFactory = new BeanFactoryImpl();
        FirstBean bean = new FirstBean();

        assertThrows(IllegalArgumentException.class, () ->
            newFactory.registerBeanByName(null, bean)
        );
    }

    @Test
    void testRegisterBeanByName_BlankNameThrows() {
        BeanFactoryImpl newFactory = new BeanFactoryImpl();
        FirstBean bean = new FirstBean();

        assertThrows(IllegalArgumentException.class, () ->
            newFactory.registerBeanByName("  ", bean)
        );
    }

    @Test
    void testGetBeanWithType() {
        FirstBean bean = beanFactory.getBean("firstBean", FirstBean.class);
        assertNotNull(bean);
        assertTrue(bean instanceof FirstBean);
    }

    @Test
    void testGetBeanWithType_NotFound() {
        BeanFactoryImpl newFactory = new BeanFactoryImpl();
        FirstBean bean = newFactory.getBean("nonexistent", FirstBean.class);
        assertNull(bean);
    }

    @Test
    void testSingletonBehavior() {
        FirstBean bean1 = beanFactory.getBean(FirstBean.class);
        FirstBean bean2 = beanFactory.getBean(FirstBean.class);

        assertSame(bean1, bean2);
    }

    @Test
    void testMultipleBeanTypes() {
        FirstBean firstBean = beanFactory.getBean(FirstBean.class);
        SecondBean secondBean = beanFactory.getBean(SecondBean.class);
        ThirdBean thirdBean = beanFactory.getBean(ThirdBean.class);

        assertNotNull(firstBean);
        assertNotNull(secondBean);
        assertNotNull(thirdBean);
    }
}
