package vn.phat.aop;

import vn.phat.annotation.Transactional;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * Factory that wraps a bean with a JDK dynamic proxy when the bean (or any
 * of its methods) is annotated with {@link Transactional}.
 *
 * <p>JDK Proxy requires the target to implement at least one interface.
 * If the bean has no interfaces the original instance is returned as-is with
 * a warning (CGLIB-style subclass proxying is out of scope for this demo).
 */
public class AopProxyFactory {

    private AopProxyFactory() {}

    /**
     * Inspect {@code bean}: if it needs transactional proxying, return a
     * proxy; otherwise return {@code bean} unchanged.
     *
     * @param bean               the raw bean instance from the container
     * @param transactionManager the manager to inject into the interceptor
     * @param <T>                bean type
     * @return proxied or original bean
     */
    @SuppressWarnings("unchecked")
    public static <T> T wrapIfTransactional(T bean, TransactionManager transactionManager) {
        Class<?> clazz = bean.getClass();

        if (!needsProxy(clazz)) {
            return bean;
        }

        Class<?>[] interfaces = clazz.getInterfaces();
        if (interfaces.length == 0) {
            System.out.printf(
                    "[AopProxyFactory] WARNING: %s has @Transactional but implements no interface." +
                    " Skipping proxy (JDK Proxy requires an interface).%n", clazz.getSimpleName());
            return bean;
        }

        System.out.printf("[AopProxyFactory] Creating transactional proxy for: %s%n",
                clazz.getSimpleName());

        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                interfaces,
                new TransactionalInterceptor(bean, transactionManager)
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * A bean needs a proxy if the class itself or any of its public methods
     * carries {@link Transactional}.
     */
    private static boolean needsProxy(Class<?> clazz) {
        // Class-level annotation
        if (clazz.isAnnotationPresent(Transactional.class)) return true;

        // Method-level annotation on any declared method
        return Arrays.stream(clazz.getMethods())
                .anyMatch(m -> m.isAnnotationPresent(Transactional.class));
    }
}
