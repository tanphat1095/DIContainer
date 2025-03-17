package vn.phat.container;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.phat.beans.FirstBean;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import vn.phat.loader.Application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BeanFactoryImplTest {

    @Mock
    private BeanFactory beanFactory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetBean() {
        // Mock the BeanFactory to return a specific bean definition
        FirstBean firstBean = new FirstBean();
        when(beanFactory.getBean(eq("firstBean"), eq(FirstBean.class))).thenReturn(firstBean);

        // Call the getBean method
        Object bean = beanFactory.getBean("firstBean", FirstBean.class);

        // Assert that the bean is retrieved correctly
        assertNotNull(bean);
        assertEquals(FirstBean.class, bean.getClass());
    }

    @Test
    void testRegisterBean() throws Exception {
        // Mock the BeanFactory
        FirstBean firstBean = new FirstBean();

        // Call the registerBean method
        beanFactory.registerBean(FirstBean.class, firstBean);

        // Assert that the bean is created and stored correctly
        verify(beanFactory, times(1)).registerBean(eq(FirstBean.class), eq(firstBean));
    }
}
