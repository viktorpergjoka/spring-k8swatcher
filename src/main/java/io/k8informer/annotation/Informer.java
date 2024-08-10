package io.k8informer.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
@Documented
public @interface  Informer {

    String name() default "default";
    String clientName() default "default";
    String[] nsLabels() default {};
    String[] resLabels() default {};
    long resyncPeriod() default 1000L;
}
