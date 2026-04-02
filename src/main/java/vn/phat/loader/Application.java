package vn.phat.loader;

import vn.phat.container.BeanFactory;
import vn.phat.container.BeanFactoryImpl;
import vn.phat.exception.BeanCreationException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

            BeanRegistration beanReg = new BeanRegistration(getInstance());
            for (String className : classes) {
                Class<?> clazz = Class.forName(className);
                beanReg.registerBeanIfMarked(clazz);
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
