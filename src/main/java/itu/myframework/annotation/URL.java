package itu.myframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import itu.myframework.routing.RequestMethod;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface URL {
    String value();

    RequestMethod requestMethod() default RequestMethod.GET;
}
