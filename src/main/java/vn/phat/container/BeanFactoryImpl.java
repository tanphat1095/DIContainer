package vn.phat.container;

import vn.phat.annotation.Autowired;
import vn.phat.annotation.Bean;
import vn.phat.util.NameConverter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeanFactoryImpl implements BeanFactory{

    private final Map<String, Object> beans = new HashMap<>();
    @Override
    public void registerBean(Class<?> clazz, Object bean) {
        assertNotNull(clazz);

        try {
            Constructor<?> autowiredConstructor = null;
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                if (constructor.isAnnotationPresent(Autowired.class)) {
                    if (autowiredConstructor != null) {
                        throw new IllegalStateException("Only one constructor can be annotated with @Autowired");
                    }
                    autowiredConstructor = constructor;
                }
            }

            Object beanInstance;
            if (autowiredConstructor != null) {
                Object[] dependencies = resolveConstructorDependencies(autowiredConstructor);
                beanInstance = autowiredConstructor.newInstance(dependencies);
            } else {
                beanInstance = bean != null ? bean : clazz.getDeclaredConstructor().newInstance();
            }

            resolveSetterDependencies(beanInstance);

            beans.put(getBeanName(clazz), beanInstance);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create bean: " + clazz.getName(), e);
        }
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

    private Object[] resolveConstructorDependencies(Constructor<?> constructor) {
        Parameter[] parameters = constructor.getParameters();
        Object[] dependencies = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> parameterType = parameter.getType();
            dependencies[i] = getBean(parameterType);
        }
        return dependencies;
    }

    private void resolveSetterDependencies(Object bean) {
        Class<?> clazz = bean.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Autowired.class) && method.getName().startsWith("set") && method.getParameterCount() == 1 && java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                try {
                    Class<?> parameterType = method.getParameterTypes()[0];
                    Object dependency = getBean(parameterType);
                    method.invoke(bean, dependency);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to inject dependency via setter: " + method.getName(), e);
                }
            }
        }
    }
}
