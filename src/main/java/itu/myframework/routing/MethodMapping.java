package itu.myframework.routing;

import java.lang.reflect.Method;

public class MethodMapping {
    private Class<?> targetClass;
    private Method annotatedMethod;

    public MethodMapping(Class<?> targetClass, Method annotatedMethod) {
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
