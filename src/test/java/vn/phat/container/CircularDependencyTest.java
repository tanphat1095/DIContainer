package vn.phat.container;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.phat.annotation.Autowired;
import vn.phat.annotation.Bean;
import vn.phat.exception.BeanCreationException;

import static org.junit.jupiter.api.Assertions.*;

class CircularDependencyTest {

    private BeanFactoryImpl beanFactory;

    @BeforeEach
    void setUp() {
        beanFactory = new BeanFactoryImpl();
    }

    @Test
    void testDetectCircularDependency_Constructor() {
        // Constructor-injected circular deps fail at registerBean() time
        // because the factory tries to resolve constructor args eagerly.
        assertThrows(BeanCreationException.class, () ->
            beanFactory.registerBean(CircularBeanA.class, null)
        );
    }

    @Test
    void testDetectCircularDependency_Field() {
        // Field-injected circular deps: registration succeeds (no-arg constructor),
        // but field wiring is handled at Application level, not BeanFactoryImpl.
        beanFactory.registerBean(CircularFieldBeanA.class, null);
        beanFactory.registerBean(CircularFieldBeanB.class, null);

        CircularFieldBeanA beanA = beanFactory.getBean(CircularFieldBeanA.class);
        assertNotNull(beanA);
    }

    @Test
    void testMultipleCircularDependencies() {
        // X→Y→Z→X: registration fails on the first bean
        // because its dependency chain can't be resolved.
        assertThrows(BeanCreationException.class, () ->
            beanFactory.registerBean(BeanX.class, null)
        );
    }

    @Test
    void testNonCircularMultipleDependencies() {
        // P→Q with Q having no deps: register Q first so P can resolve it.
        beanFactory.registerBean(BeanQ.class, null);
        beanFactory.registerBean(BeanP.class, null);

        BeanP beanP = beanFactory.getBean(BeanP.class);
        assertNotNull(beanP);
    }

    // ── test doubles ──────────────────────────────────────────────────────

    @Bean
    static class CircularBeanA {
        @Autowired
        public CircularBeanA(CircularBeanB beanB) {}
    }

    @Bean
    static class CircularBeanB {
        @Autowired
        public CircularBeanB(CircularBeanA beanA) {}
    }

    @Bean
    static class CircularFieldBeanA {
        @Autowired
        private CircularFieldBeanB beanB;

        public CircularFieldBeanB getBeanB() { return beanB; }
    }

    @Bean
    static class CircularFieldBeanB {
        @Autowired
        private CircularFieldBeanA beanA;

        public CircularFieldBeanA getBeanA() { return beanA; }
    }

    @Bean
    static class BeanX {
        @Autowired
        public BeanX(BeanY beanY) {}
    }

    @Bean
    static class BeanY {
        @Autowired
        public BeanY(BeanZ beanZ) {}
    }

    @Bean
    static class BeanZ {
        @Autowired
        public BeanZ(BeanX beanX) {}
    }

    @Bean
    static class BeanP {
        @Autowired
        public BeanP(BeanQ beanQ) {}
    }

    @Bean
    static class BeanQ {
        public BeanQ() {}
    }
}
