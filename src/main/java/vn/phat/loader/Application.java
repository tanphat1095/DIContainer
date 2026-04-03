package vn.phat.loader;

import vn.phat.container.BeanFactory;
import vn.phat.container.BeanFactoryImpl;
import vn.phat.exception.BeanCreationException;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Application {

    private Application(){}

    private static BeanFactory beanFactory;

    private static BeanFactory getInstance(){
        if(beanFactory == null)
            beanFactory = new BeanFactoryImpl();
        return beanFactory;
    }

    public static BeanFactory run(Class<?> mainClass){
        if(mainClass == null) return null;
        try {
            ClassPathScanner scanner = new ClassPathScanner();
            File file = scanner.getFile(mainClass);
            List<String> classes = new ArrayList<>();
            scanner.findAllBeanClass(file, classes);

            Set<Class<?>> beanClasses = new LinkedHashSet<>();
            for (String className : classes) {
                try {
                    beanClasses.add(Class.forName(className));
                } catch (Throwable ignored) {
                    // skip classes that cannot be loaded during scanning
                }
            }

            BeanRegistration beanReg = new BeanRegistration(getInstance(), beanClasses);
            for (Class<?> clazz : beanClasses) {
                try {
                    beanReg.registerBeanIfMarked(clazz);
                } catch (Throwable ignored) {
                    // skip individual bean failures (e.g. unresolvable dependencies, circular refs)
                }
            }

            AopPostProcessor aopProcessor = new AopPostProcessor(getInstance());
            aopProcessor.applyTransactionalProxies();

            return getInstance();
        }
        catch (Exception e){
            throw new BeanCreationException("Failed to initialize application with class: " + mainClass.getName(), e);
        }
    }

}
