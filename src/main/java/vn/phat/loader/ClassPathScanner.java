package vn.phat.loader;

import vn.phat.annotation.PackageScan;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Objects;

class ClassPathScanner {

    private static final String CLASSES = "/classes";

    File getFile(Class<?> mainClass) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        PackageScan packageScan = mainClass.getAnnotation(PackageScan.class);
        String packageName = (packageScan == null)
                ? mainClass.getPackageName()
                : packageScan.value();
        URL url = classLoader.getResource(packageName.replace(".", File.separator));
        assert url != null;
        return new File(url.getFile());
    }

    void findAllBeanClass(File file, List<String> classes) {
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
