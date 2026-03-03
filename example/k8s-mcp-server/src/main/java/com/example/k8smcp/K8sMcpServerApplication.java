package com.example.k8smcp;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.k8swatcher.annotation.EnableInformer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableInformer
public class K8sMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(K8sMcpServerApplication.class, args);
    }

    @Bean
    public KubernetesClient getKubernetesClient() {
        return new KubernetesClientBuilder().build();
    }
}
