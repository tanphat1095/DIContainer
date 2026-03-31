package vn.phat.loader;

import vn.phat.annotation.Autowired;
import vn.phat.annotation.Bean;
import vn.phat.annotation.PackageScan;
import vn.phat.annotation.Transactional;
import vn.phat.aop.AopProxyFactory;
import vn.phat.aop.TransactionManager;
import vn.phat.container.BeanFactory;
import vn.phat.container.BeanFactoryImpl;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class Application {

    private Application(){}

    private static final String CLASSES = "/classes";
    private static BeanFactory beanFactory;

    private static BeanFactory getInstance(){
        if(beanFactory == null)
            beanFactory = new BeanFactoryImpl();
        return beanFactory;
    }

    public static BeanFactory run(Class<?> mainClass){
        if(mainClass == null) return null;
        try {
            File file = getFile(mainClass);
            List<String> classes = new ArrayList<>();
            findAllBeanClass(file, classes);

            // ── Phase 1: Register all @Bean instances ────────────────────
            for (String className : classes) {
                Class<?> clazz = Class.forName(className);
                registerBeanIfMarked(clazz);
            }

            // ── Phase 2: Wrap @Transactional beans with AOP proxies ───────
            //   After proxying, re-inject dependencies so raw targets hold
            //   proxy references (prevents stale direct-object references).
            applyTransactionalProxies();

            return getInstance();
        }
        catch (Exception e){
            e.printStackTrace();
            return getInstance();
        }
    }

    // ── AOP post-processing ───────────────────────────────────────────────

    private static void applyTransactionalProxies() {
        TransactionManager txManager = getInstance().getBean(TransactionManager.class);
        if (txManager == null) {
            System.out.println("[Application] No TransactionManager found – skipping AOP proxy creation.");
            return;
        }

        System.out.println("\n[Application] ── Applying @Transactional AOP proxies ──────────────");

        // Snapshot so we don't modify the list while iterating
        List<String> beanNames = new ArrayList<>(getInstance().getDeclaredBeans());

        // Pass 1: create proxies and track raw targets for re-injection
        List<Object> rawTargets = new ArrayList<>();
        for (String beanName : beanNames) {
            Object bean = getInstance().getBean(beanName);
            if (bean == null || bean instanceof TransactionManager) continue;

            Class<?> clazz = bean.getClass();
            boolean needsProxy = clazz.isAnnotationPresent(Transactional.class)
                    || Arrays.stream(clazz.getMethods())
                             .anyMatch(m -> m.isAnnotationPresent(Transactional.class));

            if (needsProxy) {
                rawTargets.add(bean);   // keep raw ref before replacing
                Object proxy = AopProxyFactory.wrapIfTransactional(bean, txManager);
                ((BeanFactoryImpl) getInstance()).registerBeanByName(beanName, proxy);
            }
        }

        // Pass 2: re-inject @Autowired fields of raw targets so they hold
        //         proxy references instead of direct bean references.
        for (Object rawTarget : rawTargets) {
            reInjectAutowiredFields(rawTarget);
        }

        System.out.println("[Application] ── AOP proxies applied ─────────────────────────────\n");
    }

    /**
     * Re-scans the {@code @Autowired} fields of {@code target} and sets each
     * one to the current bean in the container (which may now be a proxy).
     */
    private static void reInjectAutowiredFields(Object target) {
        for (Field field : target.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(Autowired.class)) continue;
            field.setAccessible(true);
            Object dep = getInstance().getBean(field.getType());
            if (dep != null) {
                try {
                    field.set(target, dep);
                } catch (IllegalAccessException e) {
                    System.err.printf("[Application] Warning: could not re-inject field '%s': %s%n",
                            field.getName(), e.getMessage());
                }
            }
        }
    }

    // ── Bean registration helpers ─────────────────────────────────────────

    private static <T> T registerBeanIfMarked(Class<T> clazz)
            throws NoSuchMethodException, InvocationTargetException,
                   InstantiationException, IllegalAccessException {
        Bean beanMarked = clazz.getAnnotation(Bean.class);
        if (beanMarked == null) return null;

        // Re-use if already in the container
        @SuppressWarnings("unchecked")
        T existing = (T) findExistingBean(clazz);
        if (existing != null) return existing;

        // Check if the class has an @Autowired constructor
        java.lang.reflect.Constructor<?> autowiredConstructor = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(c -> c.isAnnotationPresent(Autowired.class))
                .findFirst()
                .orElse(null);

        T object;
        if (autowiredConstructor != null) {
            // Pre-process constructor parameters: guarantee they exist in the container
            for (Class<?> paramType : autowiredConstructor.getParameterTypes()) {
                if (getInstance().getBean(paramType) == null) {
                    // It's possible the param is an interface or a concrete @Bean class.
                    // We try to register it if marked with @Bean. Interfaces without @Bean
                    // might cause issues here if their implementations aren't scanned yet, 
                    // but for direct @Bean dependencies this handles out-of-order registration.
                    if (paramType.isAnnotationPresent(Bean.class)) {
                        registerBeanIfMarked(paramType);
                    }
                }
            }

            // Delegate fully to BeanFactoryImpl: it resolves constructor params and stores the bean
            getInstance().registerBean(clazz, null);
            @SuppressWarnings("unchecked")
            T registered = (T) findExistingBean(clazz);
            object = registered;
        } else {
            // No-arg constructor path: instantiate here, wire @Autowired fields, then store
            object = clazz.getConstructor().newInstance();
            registerDependencies(object);
            getInstance().registerBean(clazz, object);
        }
        return object;
    }

    /** Looks up a bean by its @Bean name; returns null if not found. */
    private static Object findExistingBean(Class<?> clazz) {
        Bean beanMarked = clazz.getAnnotation(Bean.class);
        if (beanMarked == null) return null;
        String name = (beanMarked.value() == null || beanMarked.value().isBlank())
                ? vn.phat.util.NameConverter.convertCLassToBeanName(clazz)
                : beanMarked.value();
        return getInstance().getBean(name);
    }

    private static void registerDependencies(Object object)
            throws InvocationTargetException, NoSuchMethodException,
                   InstantiationException, IllegalAccessException {
        // 1. Field injection
        Field[] fields = object.getClass().getDeclaredFields();
        Predicate<Field> isAutowiredField = f -> f.getAnnotation(Autowired.class) != null;
        List<Field> fieldAutowired = Arrays.stream(fields).filter(isAutowiredField).toList();
        for (Field f : fieldAutowired) {
            f.setAccessible(true);
            Class<?> declaredClass = f.getType();
            setBeanToField(declaredClass, f, object);
        }

        // 2. Setter injection
        java.lang.reflect.Method[] methods = object.getClass().getDeclaredMethods();
        for (java.lang.reflect.Method method : methods) {
            if (method.isAnnotationPresent(Autowired.class) && method.getName().startsWith("set") && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                Object dependency = getInstance().getBean(paramType);
                if (dependency == null && paramType.isAnnotationPresent(Bean.class)) {
                    dependency = registerBeanIfMarked(paramType);
                }
                method.setAccessible(true);
                method.invoke(object, dependency);
            }
        }
    }

    /**
     * Resolves a bean for the given {@code clazz} (handles both concrete
     * {@code @Bean}-annotated classes and interface types) and injects it
     * into {@code field} on {@code object}.
     *
     * <p>For interfaces / abstract types without {@code @Bean}, the container
     * performs a type-compatible scan via {@link BeanFactoryImpl#getBean(Class)}.
     */
    static <T> void setBeanToField(Class<T> clazz, Field field, Object object)
            throws InvocationTargetException, NoSuchMethodException,
                   InstantiationException, IllegalAccessException {
        // getBean(Class) now handles both @Bean-annotated and interface types
        T bean = getInstance().getBean(clazz);
        if (bean == null && clazz.isAnnotationPresent(Bean.class)) {
            // Not in container yet – register on demand (handles out-of-order deps)
            bean = registerBeanIfMarked(clazz);
        }
        field.set(object, bean);
    }

    // ── Classpath scanning helpers ────────────────────────────────────────

    private static File getFile(Class<?> mainClass) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        PackageScan packageScan = mainClass.getAnnotation(PackageScan.class);
        String packageName = (packageScan == null)
                ? mainClass.getPackageName()
                : packageScan.value();
        URL url = classLoader.getResource(packageName.replace(".", File.separator));
        assert url != null;
        return new File(url.getFile());
    }

    static void findAllBeanClass(File file, List<String> classes) {
        if (file.isDirectory() && file.listFiles() != null) {
            for (File f : Objects.requireNonNull(file.listFiles())) {
                findAllBeanClass(f, classes);
            }
        } else {
            if (!file.getPath().endsWith(".class")) return;
            String path = file.getPath().substring(0, file.getPath().indexOf(".class"));
            String fullClassName = path
                    .substring(path.indexOf(CLASSES) + CLASSES.length() + 1)
                    .replace(File.separator, ".");
            classes.add(fullClassName);
        }
    }
}
