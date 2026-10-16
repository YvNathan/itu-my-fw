package itu.myframework.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppProperties {
    private static final Properties properties = new Properties();

    private AppProperties() {
    }

    public static void load(String resourceName) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream in = classLoader.getResourceAsStream(resourceName)) {
            if (in != null) {
                properties.load(in);
                System.out.println("[AppProperties] " + resourceName
                        + (in == null ? " INTROUVABLE" : " chargé, clés = " + properties.stringPropertyNames()));
            }
        }
    }

    public static String get(String key) {
        return System.getProperty(key, properties.getProperty(key));
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value == null || value.isBlank() ? defaultValue : value;

    }
}
