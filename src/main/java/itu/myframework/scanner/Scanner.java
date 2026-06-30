package itu.myframework.scanner;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import itu.myframework.annotation.Controller;
import itu.myframework.routing.MethodMapping;
import itu.myframework.routing.RequestMethod;
import itu.myframework.routing.URLMethod;

public class Scanner {
    public static List<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotationClass) throws Exception {
        return getAnnotatedClasses("", annotationClass);
    }

    public static List<Class<?>> getAnnotatedClasses(String packageName, Class<? extends Annotation> annotationClass)
            throws Exception {
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
            classes.addAll(findAnnotatedClasses(directory, packageName == null ? "" : packageName, classLoader,
                    annotationClass));
        }

        return classes;
    }

    private static List<Class<?>> findAnnotatedClasses(File directory, String packageName, ClassLoader classLoader,
            Class<? extends Annotation> annotationClass) throws Exception {
        List<Class<?>> classes = new ArrayList<>();

        if (!directory.exists())
            return classes;

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                classes.addAll(findAnnotatedClasses(file,
                        joinPackage(packageName, file.getName().replace(".class", "")), classLoader, annotationClass));
            } else if (file.getName().endsWith(".class")) {
                String simpleName = file.getName().substring(0, file.getName().length() - ".class".length());
                String className = joinPackage(packageName, simpleName);

                Class<?> clazz = classLoader.loadClass(className);

                if (clazz.isAnnotationPresent(annotationClass)) {
                    classes.add(clazz);
                }
            }
        }

        return classes;
    }

    public static List<Method> findAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        List<Method> methods = new ArrayList<>();

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotationClass)) {
                methods.add(method);
            }
        }

        return methods;
    }

    public static void getMappings(String packageName, HashMap<URLMethod, MethodMapping> mappings) throws Exception {
        List<Class<?>> controllers = getAnnotatedClasses(packageName, Controller.class);

        for (Class<?> ctrl : controllers) {
            List<Method> annotatedMethods = findAnnotatedMethods(ctrl, itu.myframework.annotation.URL.class);

            for (Method method : annotatedMethods) {
                itu.myframework.annotation.URL urlAnnotation = method.getAnnotation(itu.myframework.annotation.URL.class);

                String urlValue = urlAnnotation.value();
                RequestMethod requestMethod = urlAnnotation.requestMethod();
                URLMethod urlMethod = new URLMethod(urlValue, requestMethod);

                if (mappings.containsKey(urlMethod)) {
                    throw new Exception("la clé: " + requestMethod + " " + urlValue + " existe déjà");
                }

                mappings.put(urlMethod, new MethodMapping(ctrl, method));
            }
        }

    }

    private static String joinPackage(String packageName, String name) {
        if (packageName == null || packageName.isBlank()) {
            return name;
        }

        return packageName + "." + name;
    }
}
