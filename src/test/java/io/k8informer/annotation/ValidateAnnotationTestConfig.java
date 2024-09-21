package io.k8informer.annotation;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan(basePackages = "io.k8informer.annotation.bean")
@Configuration
public class ValidateAnnotationTestConfig {}
