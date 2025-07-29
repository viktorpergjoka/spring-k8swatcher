/*
 * Copyright (C) 2025 Viktor Pergjoka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.k8swatcher.annotation.processor;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

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
    public void init() {
        clients = ctx.getBeansOfType(KubernetesClient.class);
        clients.computeIfAbsent("default", (value) -> new KubernetesClientBuilder().build());
    }

    public KubernetesClient getClient(String name) {
        return clients.getOrDefault(name, clients.get("default"));
    }

    @PreDestroy
    public void shutdown() {
        log.info("Closing Kubernetes clients");
        clients.values().forEach(KubernetesClient::close);
    }
}
