package io.informerwatch.spring.annotation;

import io.fabric8.kubernetes.client.KubernetesClient;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface K8Watch {

    WatchType type();
    Class resource();
}
