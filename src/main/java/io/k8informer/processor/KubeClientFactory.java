package io.k8informer.processor;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class KubeClientFactory {

    private final ApplicationContext ctx;
    private Map<String, KubernetesClient> clients;

    public KubeClientFactory(ApplicationContext ctx) {
        this.ctx = ctx;
        clients = new HashMap<>();
    }


    @PostConstruct
    public void init(){
        clients = ctx.getBeansOfType(KubernetesClient.class);
        clients.computeIfAbsent("default", (value) -> new KubernetesClientBuilder().build());
    }

    public KubernetesClient getClient(String name){
        return clients.getOrDefault(name, clients.get("default"));
    }

    @PreDestroy
    public void shutdown(){
        clients.values().forEach(KubernetesClient::close);
    }

}
