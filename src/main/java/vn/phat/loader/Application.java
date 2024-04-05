package vn.phat.loader;

import vn.phat.annotation.Autowired;
import vn.phat.annotation.Bean;
import vn.phat.annotation.PackageScan;
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
            BeanFactory beanFact = getInstance();
            for (String className : classes) {
                Class<?> clazz = Class.forName(className);
                registerBeanIfMarked(clazz);

            }
            return beanFact;
        }
        catch (Exception e){
            return getInstance();
        }
    }

    private static void registerBeanIfMarked(Class<?> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Bean beanMarked = clazz.getAnnotation(Bean.class);
        if(beanMarked != null){
            Object object = clazz.getConstructor().newInstance();
            registerDependencies(object);
            getInstance().registerBean(clazz, object);
        }
    }

    private static void registerDependencies(Object object) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Field [] fields = object.getClass().getDeclaredFields();
        Predicate<Field> isAutowiredField = f -> f.getAnnotation(Autowired.class) != null;
        List<Field> fieldAutowired = Arrays.stream(fields).filter(isAutowiredField).toList();
        for(Field f : fieldAutowired){
            f.setAccessible(true);
            Class<?> declaredClass = f.getDeclaringClass();
            registerBeanIfMarked(declaredClass);
        }
    }

    private static File getFile(Class<?> mainClass) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        PackageScan packageScan = mainClass.getAnnotation(PackageScan.class);
        String packageName = null;
        if(packageScan == null){
            packageName = mainClass.getPackageName();
        }
        else {
            packageName = packageScan.value();
        }


        URL url = classLoader.getResource(packageName.replace(".", File.separator));
        assert url != null;
        return new File(url.getFile());
    }


    static void findAllBeanClass(File file, List<String> classes){
        if(file.isDirectory() && file.listFiles() != null){
            for(File f : Objects.requireNonNull(file.listFiles())){
                findAllBeanClass(f, classes);
            }
        }
        else{
            String path = file.getPath().substring(0, file.getPath().indexOf(".class"));
            String fullClassName = path.substring(path.indexOf(CLASSES)+ CLASSES.length()+1).replace(File.separator,".");
            classes.add(fullClassName);

        }

    }

}
