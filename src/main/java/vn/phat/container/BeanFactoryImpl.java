package vn.phat.container;

import vn.phat.annotation.Autowired;
import vn.phat.annotation.Bean;
import vn.phat.exception.AmbiguousBeanException;
import vn.phat.exception.BeanCreationException;
import vn.phat.exception.BeanResolutionException;
import vn.phat.exception.CircularDependencyException;
import vn.phat.util.NameConverter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
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
        if (clazz == null) {
            throw new IllegalArgumentException("Class type cannot be null");
        }

        try {
            Constructor<?> autowiredConstructor = null;
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                if (constructor.isAnnotationPresent(Autowired.class)) {
                    if (autowiredConstructor != null) {
                        throw new BeanCreationException("Only one constructor can be annotated with @Autowired");
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

            String beanName = getBeanName(clazz);
            beans.put(beanName, beanInstance);
        } catch (BeanCreationException e) {
            throw e;
        } catch (Exception e) {
            throw new BeanCreationException("Failed to create bean: " + clazz.getName(), e);
        }
    }

    @Override
    public <T> T getBean(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class type cannot be null");
        }

        Bean beanAnnotation = clazz.getAnnotation(Bean.class);
        if (beanAnnotation != null) {
            String beanName = getBeanName(clazz);
            try {
                return getBean(beanName, clazz);
            } catch (BeanResolutionException e) {
                throw new BeanResolutionException(
                        "Bean of type " + clazz.getSimpleName() + " with name '" + beanName +
                        "' not found. Available beans: " + beans.keySet(),
                        e
                );
            }
        }

        T bean = findBeanByType(clazz);
        if (bean == null) {
            List<String> similarBeans = findSimilarBeans(clazz);
            String message = "No bean found of type " + clazz.getSimpleName();
            if (!similarBeans.isEmpty()) {
                message += ". Available beans: " + beans.keySet();
            }
            throw new BeanResolutionException(message);
        }
        return bean;
    }

    @Override
    public <T> T getBean(String beanName, Class<T> beanType) {
        if (beanType == null) {
            throw new IllegalArgumentException("Bean type cannot be null");
        }
        Object bean = getBean(beanName);
        if (bean == null) {
            return null;
        }
        try {
            return castBeanObject(beanType, bean);
        } catch (ClassCastException e) {
            throw new BeanResolutionException(
                    "Bean '" + beanName + "' of type " + bean.getClass().getSimpleName() +
                    " cannot be cast to " + beanType.getSimpleName(),
                    e
            );
        }
    }

    @Override
    public Object getBean(String beanName) {
        if (beanName == null || beanName.isBlank()) {
            throw new IllegalArgumentException("Bean name cannot be null or blank");
        }

        Object bean = beans.get(beanName);
        if (bean == null) {
            throw new BeanResolutionException(
                    "No bean found with name: '" + beanName + "'. Available beans: " + beans.keySet()
            );
        }
        return bean;
    }

    @Override
    public List<String> getDeclaredBeans() {
        return beans.keySet().stream().toList();
    }

    private String getBeanName(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class type cannot be null");
        }
        Bean beanAnnotation = clazz.getAnnotation(Bean.class);
        if (beanAnnotation == null) {
            throw new IllegalArgumentException(
                    "Class " + clazz.getName() + " is not annotated with @Bean");
        }
        return beanAnnotation.value() == null || beanAnnotation.value().trim().isEmpty()
                ? NameConverter.convertCLassToBeanName(clazz)
                : beanAnnotation.value();
    }

    private <T> List<String> findSimilarBeans(Class<T> clazz) {
        List<String> similar = new ArrayList<>();
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Class<?> beanClass = entry.getValue().getClass();
            if (beanClass.getName().contains(clazz.getSimpleName())) {
                similar.add(entry.getKey() + " (" + beanClass.getSimpleName() + ")");
            }
        }
        return similar;
    }

    @SuppressWarnings("unchecked")
    private <T> T findBeanByType(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class type cannot be null");
        }

        List<String> matchingBeanNames = new ArrayList<>();
        List<Object> matchingBeans = new ArrayList<>();

        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            if (clazz.isAssignableFrom(entry.getValue().getClass())) {
                matchingBeanNames.add(entry.getKey());
                matchingBeans.add(entry.getValue());
            }
        }

        if (matchingBeans.isEmpty()) {
            return null;
        }

        if (matchingBeans.size() > 1) {
            String message = String.format(
                    "Ambiguous bean of type %s. Found %d matching beans: %s",
                    clazz.getSimpleName(),
                    matchingBeans.size(),
                    matchingBeanNames
            );
            throw new AmbiguousBeanException(message);
        }

        return (T) matchingBeans.get(0);
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
                    throw new BeanResolutionException("Failed to inject dependency via setter: " + method.getName(), e);
                }
            }
        }
    }

    private Object getBean(Class<?> clazz, Set<Class<?>> resolving) {
        if (resolving.contains(clazz)) {
            throw new CircularDependencyException("Circular dependency detected: " + clazz.getName());
        }
        resolving.add(clazz);
        try {
            // Use the public getBean(Class) which handles both @Bean classes and interfaces
            Object bean = getBean(clazz);
            if (bean == null) {
                throw new BeanResolutionException("No bean found for class: " + clazz.getName());
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
                        throw new BeanResolutionException("Failed to inject dependency via field: " + field.getName(), e);
                    }
                }
            });
        });
    }

    public BeanFactoryImpl autowireProperties(BeanFactoryImpl beanFactory) {
        return this;
    }

    /**
     * Directly store {@code bean} under the given {@code beanName}, replacing
     * any previously registered instance.  Used by the AOP post-processor to
     * swap a raw bean for its transactional proxy without re-running injection.
     *
     * @param beanName the canonical name already in the registry
     * @param bean     the replacement object (usually a JDK dynamic proxy)
     */
    public void registerBeanByName(String beanName, Object bean) {
        if (beanName == null || beanName.isBlank()) {
            throw new IllegalArgumentException("Bean name must not be null or blank");
        }
        beans.put(beanName, bean);
    }
}
