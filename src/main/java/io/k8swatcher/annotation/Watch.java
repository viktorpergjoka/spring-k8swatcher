package io.k8swatcher.annotation;


import io.fabric8.kubernetes.api.model.KubernetesResource;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Watch {

    EventType event();
    Class<? extends KubernetesResource> resource();
}
