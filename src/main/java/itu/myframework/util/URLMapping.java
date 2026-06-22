package itu.myframework.util;

import java.lang.reflect.Method;

public class URLMapping {
    private Class<?> targetClass;
    private Method annotatedMethod;

    public URLMapping(Class<?> targetClass, Method annotatedMethod) {
        this.targetClass = targetClass;
        this.annotatedMethod = annotatedMethod;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    public Method getAnnotatedMethod() {
        return annotatedMethod;
    }

    public void setAnnotatedMethod(Method annotatedMethod) {
        this.annotatedMethod = annotatedMethod;
    }
}
