package vn.phat.container;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
}
