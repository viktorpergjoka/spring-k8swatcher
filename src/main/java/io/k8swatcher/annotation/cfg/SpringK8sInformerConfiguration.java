package io.k8swatcher.annotation.cfg;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan(basePackages = "io.k8swatcher.annotation")
@Configuration
public class SpringK8sInformerConfiguration {}
