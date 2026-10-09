package itu.myframework.binding;

import java.util.List;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.ArrayList;

import itu.myframework.annotation.RequestParam;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

public class Binder {
    public static Object bindRequestParam(RequestParam rp, Class<?> type, HttpServletRequest req)
            throws ServletException {
        String rawParam = req.getParameter(rp.value());

        if (rawParam == null) {
            if (rp.required()) {
                throw new ServletException("Paramètre requis : " + rp.value());
            } else {
                return null;
            }
        }

        try {
            return resolveParamType(rawParam, type);
        } catch (Exception e) {
            throw new ServletException("Valeur invalide : " + rp.value());
        }
    }

    public static Object bindModel(HttpServletRequest req, Class<?> type) throws ServletException {
        try {
            return bind(type, req, null);
        } catch (Exception e) {
            throw new ServletException("Modele invalide : " + type.getSimpleName());
        }
    }

    public static Object bind(Class<?> clazz, HttpServletRequest req, String prefix) throws Exception {
        Object instance = clazz.getDeclaredConstructor().newInstance();

        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                int mod = field.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isFinal(mod) || field.isSynthetic()) {
                    continue;
                }

                String fieldName = field.getName();
                String paramName = fieldName;
                if (prefix != null && !prefix.isEmpty()) {
                    paramName = prefix + "." + fieldName;
                }
                Class<?> type = field.getType();

                Object value;

                if (type.isArray()) {
                    value = bindArray(type, req, paramName);
                    if (value == null) {
                        continue;
                    }
                } else if (isSimpleType(type)) {
                    String paramValue = req.getParameter(paramName);

                    if (paramValue == null || (paramValue.isEmpty() && type != String.class)) {
                        continue;
                    }
                    value = resolveParamType(paramValue, type);
                } else {
                    if (!hasParamWithPrefix(req, paramName + ".")) {
                        continue;
                    }
                    value = bind(type, req, paramName);
                }

                setValue(instance, field, value);
            }
        }

        return instance;
    }

    private static Object bindArray(Class<?> type, HttpServletRequest req, String paramName) throws Exception {
        Class<?> itemType = type.getComponentType();

        List<Object> items = new ArrayList<>();

        if (isSimpleType(itemType)) {
            String[] values = req.getParameterValues(paramName);
            if (values == null) {
                return null;
            }

            for (String v : values) {
                if (v.isBlank()) {
                    continue;
                }
                items.add(resolveParamType(v, itemType));
            }
        } else {
            for (int i = 0; hasParamWithPrefix(req, paramName + "[" + i + "]."); i++) {
                items.add(bind(itemType, req, paramName + "[" + i + "]"));
            }
            if (items.isEmpty()) {
                return null;
            }
        }

        Object array = Array.newInstance(itemType, items.size());
        for (int i = 0; i < items.size(); i++) {
            Array.set(array, i, items.get(i));
        }
        return array;
    }

    private static boolean isSimpleType(Class<?> type) {
        return type == String.class || type == int.class || type == Integer.class
                || type == double.class || type == Double.class || type == long.class || type == Long.class
                || type == boolean.class || type == Boolean.class || type == LocalDate.class;
    }

    private static Object resolveParamType(String param, Class<?> type) {
        if (type == String.class)
            return param;
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(param);
        }
        if (type == long.class || type == Long.class) {
            return Long.parseLong(param);
        }
        if (type == double.class || type == Double.class) {
            return Double.parseDouble(param);
        }
        if (type == boolean.class || type == Boolean.class) {
            return param.equalsIgnoreCase("true") || param.equalsIgnoreCase("on");
        }
        if (type == LocalDate.class) {
            return LocalDate.parse(param);
        }

        throw new IllegalArgumentException("Type non supporté");
    }

    private static boolean hasParamWithPrefix(HttpServletRequest req, String prefix) {
        for (String name : req.getParameterMap().keySet()) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static void setValue(Object instance, Field field, Object value) throws Exception {
        String name = field.getName();
        String setterName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        try {
            instance.getClass().getMethod(setterName, field.getType()).invoke(instance, value);
        } catch (NoSuchMethodException e) {
            field.setAccessible(true);
            field.set(instance, value);
        }
    }
}
