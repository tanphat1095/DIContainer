package vn.phat.container;

import vn.phat.annotation.Bean;
import vn.phat.util.NameConverter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeanFactoryImpl implements BeanFactory{

    private final Map<String, Object> beans = new HashMap<>();
    @Override
    public void registerBean(Class<?> clazz, Object bean) {
        assertNotNull(clazz);

        beans.put(getBeanName(clazz), bean);
    }

    @Override
    public <T> T getBean(Class<T> clazz) {
        assertNotNull(clazz);
        String beanName = getBeanName(clazz);
        return getBean(beanName, clazz);
    }

    @Override
    public <T> T getBean(String beanName, Class<T> beanType) {
        return castBeanObject(beanType, getBean(beanName));
    }

    @Override
    public Object getBean(String beanName) {
        return beans.get(beanName);
    }

    @Override
    public List<String> getDeclaredBeans() {
        return beans.keySet().stream().toList();
    }

    private void assertNotNull(Class<?> clazz) throws NullPointerException{
        if(clazz == null) throw new NullPointerException();
    }

    private String getBeanName(Class<?> clazz){
        assertNotNull(clazz);
        Bean beanAnnotation = clazz.getAnnotation(Bean.class);
        assert beanAnnotation != null;
        return beanAnnotation.value() == null || beanAnnotation.value().trim().isEmpty() ? NameConverter.convertCLassToBeanName(clazz) : beanAnnotation.value();
    }

    private <T> T castBeanObject(Class<T> clazz, Object bean){
        if(bean == null) return null;
        return clazz.cast(bean);
    }
}
