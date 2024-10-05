package io.k8swatcher.annotation;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan(basePackages = "io.k8swatcher.annotation.bean")
@Configuration
public class ValidateAnnotationTestConfig {}
