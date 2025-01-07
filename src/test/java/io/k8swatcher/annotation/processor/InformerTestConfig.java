package io.k8swatcher.annotation.processor;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan(basePackages = "io.k8swatcher.annotation.processor.bean")
@Configuration
public class InformerTestConfig {

    @Bean
    public KubernetesClient client() {
        return new KubernetesClientBuilder().build();
    }

    @Bean
    public Map<String, String> resMaps() {
        return new HashMap<>();
    }

    @Bean
    public AtomicInteger eventsCount() {
        return new AtomicInteger(0);
    }
}
