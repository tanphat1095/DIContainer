package vn.phat.loader;

import vn.phat.annotation.Autowired;
import vn.phat.annotation.Transactional;
import vn.phat.aop.AopProxyFactory;
import vn.phat.aop.TransactionManager;
import vn.phat.container.BeanFactory;
import vn.phat.container.BeanFactoryImpl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class AopPostProcessor {

    private final BeanFactory beanFactory;

    AopPostProcessor(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    void applyTransactionalProxies() {
        TransactionManager txManager = beanFactory.getBean(TransactionManager.class);
        if (txManager == null) {
            System.out.println("[Application] No TransactionManager found – skipping AOP proxy creation.");
            return;
        }

        System.out.println("\n[Application] ── Applying @Transactional AOP proxies ──────────────");

        List<String> beanNames = new ArrayList<>(beanFactory.getDeclaredBeans());

        List<Object> rawTargets = new ArrayList<>();
        for (String beanName : beanNames) {
            Object bean = beanFactory.getBean(beanName);
            if (bean == null || bean instanceof TransactionManager) continue;

            Class<?> clazz = bean.getClass();
            boolean needsProxy = clazz.isAnnotationPresent(Transactional.class)
                    || Arrays.stream(clazz.getMethods())
                             .anyMatch(m -> m.isAnnotationPresent(Transactional.class));

            if (needsProxy) {
                rawTargets.add(bean);
                Object proxy = AopProxyFactory.wrapIfTransactional(bean, txManager);
                ((BeanFactoryImpl) beanFactory).registerBeanByName(beanName, proxy);
            }
        }

        for (Object rawTarget : rawTargets) {
            reInjectAutowiredFields(rawTarget);
        }

        System.out.println("[Application] ── AOP proxies applied ─────────────────────────────\n");
    }

    private void reInjectAutowiredFields(Object target) {
        for (Field field : target.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(Autowired.class)) continue;
            field.setAccessible(true);
            Object dep = beanFactory.getBean(field.getType());
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
}
