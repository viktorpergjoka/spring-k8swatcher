package io.k8swatcher.annotation;

import java.lang.annotation.*;
import org.springframework.stereotype.Component;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
@Documented
public @interface Informer {

    String name() default "default";

    String clientName() default "default";

    String[] nsNames() default {};

    String[] nsLabels() default {};

    String[] resLabels() default {};

    long resyncPeriod() default 1000L;
}
