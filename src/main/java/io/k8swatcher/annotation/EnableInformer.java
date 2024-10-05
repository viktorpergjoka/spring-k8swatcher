package io.k8swatcher.annotation;

import io.k8swatcher.annotation.cfg.SpringK8sInformerConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(SpringK8sInformerConfiguration.class)
public @interface EnableInformer {}
