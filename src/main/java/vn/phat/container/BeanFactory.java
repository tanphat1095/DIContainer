package vn.phat.container;

import java.util.List;

public interface BeanFactory {
    public void registerBean(Class<?> clazz, Object bean);
    public <T> T getBean(Class<T> clazz);
    public <T> T getBean(String beanName, Class<T> beanType);

    public Object getBean(String beanName);

    public List<String> getDeclaredBeans();
}
