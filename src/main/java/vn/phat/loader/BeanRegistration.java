package vn.phat.loader;

import vn.phat.annotation.Autowired;
import vn.phat.annotation.Bean;
import vn.phat.container.BeanFactory;
import vn.phat.exception.BeanResolutionException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

class BeanRegistration {

    private final BeanFactory beanFactory;

    BeanRegistration(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    <T> T registerBeanIfMarked(Class<T> clazz)
            throws NoSuchMethodException, InvocationTargetException,
                   InstantiationException, IllegalAccessException {
        Bean beanMarked = clazz.getAnnotation(Bean.class);
        if (beanMarked == null) return null;

        T existing = findExistingBean(clazz);
        if (existing != null) return existing;

        java.lang.reflect.Constructor<?> autowiredConstructor = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(c -> c.isAnnotationPresent(Autowired.class))
                .findFirst()
                .orElse(null);

        T object;
        if (autowiredConstructor != null) {
            for (Class<?> paramType : autowiredConstructor.getParameterTypes()) {
                if (findBean(paramType) == null && paramType.isAnnotationPresent(Bean.class)) {
                    registerBeanIfMarked(paramType);
                }
            }

            beanFactory.registerBean(clazz, null);
            object = findExistingBean(clazz);
        } else {
            object = clazz.getConstructor().newInstance();
            registerDependencies(object);
            beanFactory.registerBean(clazz, object);
        }
        return object;
    }

    @SuppressWarnings("unchecked")
    private <T> T findExistingBean(Class<?> clazz) {
        Bean beanMarked = clazz.getAnnotation(Bean.class);
        if (beanMarked == null) return null;
        String name = (beanMarked.value() == null || beanMarked.value().isBlank())
                ? vn.phat.util.NameConverter.convertCLassToBeanName(clazz)
                : beanMarked.value();
        return (T) findBeanByName(name);
    }

    private Object findBeanByName(String name) {
        try {
            return beanFactory.getBean(name);
        } catch (BeanResolutionException e) {
            return null;
        }
    }

    private <T> T findBean(Class<T> clazz) {
        try {
            return beanFactory.getBean(clazz);
        } catch (BeanResolutionException | IllegalArgumentException e) {
            return null;
        }
    }

    private void registerDependencies(Object object)
            throws InvocationTargetException, NoSuchMethodException,
                   InstantiationException, IllegalAccessException {
        Field[] fields = object.getClass().getDeclaredFields();
        Predicate<Field> isAutowiredField = f -> f.getAnnotation(Autowired.class) != null;
        List<Field> fieldAutowired = Arrays.stream(fields).filter(isAutowiredField).toList();
        for (Field f : fieldAutowired) {
            f.setAccessible(true);
            setBeanToField(f.getType(), f, object);
        }

        for (java.lang.reflect.Method method : object.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Autowired.class) && method.getName().startsWith("set") && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                Object dependency = findBean(paramType);
                if (dependency == null && paramType.isAnnotationPresent(Bean.class)) {
                    dependency = registerBeanIfMarked(paramType);
                }
                method.setAccessible(true);
                method.invoke(object, dependency);
            }
        }
    }

    <T> void setBeanToField(Class<T> clazz, Field field, Object object)
            throws InvocationTargetException, NoSuchMethodException,
                   InstantiationException, IllegalAccessException {
        T bean = findBean(clazz);
        if (bean == null && clazz.isAnnotationPresent(Bean.class)) {
            bean = registerBeanIfMarked(clazz);
        }
        field.set(object, bean);
    }
}
