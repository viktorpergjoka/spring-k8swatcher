package io.k8informer.annotation;

import io.k8informer.annotation.cfg.SpringK8InformerConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(SpringK8InformerConfiguration.class)
public @interface EnableInformer {}