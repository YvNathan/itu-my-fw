package itu.myframework.util;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import itu.myframework.controller.Controller;

public class Scanner {
    public static List<Class<?>> getClasses() throws Exception {
        return getClasses("");
    }

    public static List<Class<?>> getClasses(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        String path = packageName == null ? "" : packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();

            if (!"file".equals(resource.getProtocol())) {
                continue;
            }

            File directory = new File(resource.toURI());
            classes.addAll(findClasses(directory, packageName == null ? "" : packageName, classLoader));
        }

        return classes;
    }

    private static List<Class<?>> findClasses(File directory, String packageName, ClassLoader classLoader) throws Exception {
        List<Class<?>> classes = new ArrayList<>();

        if (!directory.exists())
            return classes;

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                classes.addAll(findClasses(file, joinPackage(packageName, file.getName().replace(".class", "")), classLoader));
            } else if (file.getName().endsWith(".class")) {
                String simpleName = file.getName().substring(0, file.getName().length() - ".class".length()); 
                String className = joinPackage(packageName, simpleName);

                Class<?> clazz = Class.forName(className, false, classLoader);

                if (clazz.isAnnotationPresent(Controller.class)) {
                    classes.add(clazz);
                }
            }
        }

        return classes;
    }

    private static String joinPackage(String packageName, String name) {
    if (packageName == null || packageName.isBlank()) {
        return name;
    }

    return packageName + "." + name;
}
}
