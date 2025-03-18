package vn.phat.container;

import vn.phat.annotation.Autowired;
import vn.phat.annotation.Bean;
import vn.phat.util.NameConverter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class BeanFactoryImpl implements BeanFactory{

    private final Map<String, Object> beans = new ConcurrentHashMap<>();
    private final Set<Class<?>> resolving = new HashSet<>();

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
                Object[] dependencies = resolveConstructorDependencies(autowiredConstructor, new HashSet<>());
                beanInstance = autowiredConstructor.newInstance(dependencies);
            } else {
                beanInstance = bean != null ? bean : clazz.getDeclaredConstructor().newInstance();
            }

            resolveSetterDependencies(beanInstance, new HashSet<>());

            String beanName = getBeanName(clazz);
            beans.put(beanName, beanInstance);
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

    private Object[] resolveConstructorDependencies(Constructor<?> constructor, Set<Class<?>> resolving) {
        Parameter[] parameters = constructor.getParameters();
        Object[] dependencies = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> parameterType = parameter.getType();
            dependencies[i] = getBean(parameterType, resolving);
        }
        return dependencies;
    }

    private void resolveSetterDependencies(Object bean, Set<Class<?>> resolving) {
        Class<?> clazz = bean.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Autowired.class) && method.getName().startsWith("set") && method.getParameterCount() == 1 && java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                try {
                    Class<?> parameterType = method.getParameterTypes()[0];
                    Object dependency = getBean(parameterType, resolving);
                    method.invoke(bean, dependency);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to inject dependency via setter: " + method.getName(), e);
                }
            }
        }
    }

    private Object getBean(Class<?> clazz, Set<Class<?>> resolving) {
        if (resolving.contains(clazz)) {
            throw new RuntimeException("Circular dependency detected: " + clazz.getName());
        }
        resolving.add(clazz);
        try {
            String beanName = getBeanName(clazz);
            Object bean = getBean(beanName);
             if (bean == null) {
                throw new RuntimeException("No bean found for class: " + clazz.getName());
            }
            return bean;
        } finally {
            resolving.remove(clazz);
        }
    }

    public void autowireFields(Object beanFactory, Object... beans) {
        Arrays.stream(beans).forEach(bean -> {
            Class<?> clazz = bean.getClass();
            Arrays.stream(clazz.getDeclaredFields()).forEach(field -> {
                if (field.isAnnotationPresent(Autowired.class)) {
                    field.setAccessible(true);
                    try {
                        Object dependency = getBean(field.getType());
                        field.set(bean, dependency);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Failed to inject dependency via field: " + field.getName(), e);
                    }
                }
            });
        });
    }

    public BeanFactoryImpl autowireProperties(BeanFactoryImpl beanFactory) {
        return this;
    }
}
