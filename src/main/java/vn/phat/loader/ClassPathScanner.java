package vn.phat.loader;

import vn.phat.annotation.PackageScan;

import java.io.File;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

class ClassPathScanner {

    private static final String CLASSES_MARKER = File.separator + "classes" + File.separator;
    private static final String TEST_CLASSES_MARKER = File.separator + "test-classes" + File.separator;

    File getFile(Class<?> mainClass) {
        PackageScan packageScan = mainClass.getAnnotation(PackageScan.class);
        String packageName = (packageScan == null)
                ? mainClass.getPackageName()
                : packageScan.value();
        String packagePath = packageName.replace(".", File.separator);

        try {
            URL rootUrl = mainClass.getProtectionDomain().getCodeSource().getLocation();
            File rootDirectory = new File(rootUrl.toURI());
            return new File(rootDirectory, packagePath);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot resolve package scan directory for " + mainClass.getName(), e);
        }
    }

    void findAllBeanClass(File file, List<String> classes) {
        if (file.isDirectory() && file.listFiles() != null) {
            for (File f : Objects.requireNonNull(file.listFiles())) {
                findAllBeanClass(f, classes);
            }
        } else {
            if (!file.getPath().endsWith(".class")) return;
            String path = file.getPath().substring(0, file.getPath().indexOf(".class"));
            int idx = path.indexOf(TEST_CLASSES_MARKER);
            String marker = TEST_CLASSES_MARKER;
            if (idx < 0) {
                idx = path.indexOf(CLASSES_MARKER);
                marker = CLASSES_MARKER;
            }
            if (idx < 0) return;
            String fullClassName = path.substring(idx + marker.length())
                    .replace(File.separator, ".");
            classes.add(fullClassName);
        }
    }
}
