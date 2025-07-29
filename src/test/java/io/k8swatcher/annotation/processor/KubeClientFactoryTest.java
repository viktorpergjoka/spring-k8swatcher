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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.fabric8.kubernetes.client.KubernetesClient;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
class KubeClientFactoryTest {

    @Mock
    private ApplicationContext ctx;

    @Mock
    private KubernetesClient clientOne;

    @Mock
    private KubernetesClient clientTwo;

    @Mock
    private KubernetesClient defaultClientBean;

    private KubeClientFactory factory;

    @BeforeEach
    void setUp() {
        factory = new KubeClientFactory(ctx);
    }

    @Test
    void createsOnlyDefaultClient() {
        when(ctx.getBeansOfType(KubernetesClient.class)).thenReturn(new HashMap<>());

        factory.init();

        KubernetesClient defaultClient = factory.getClient("default");
        assertNotNull(defaultClient, "Should have a default client");

        assertSame(defaultClient, factory.getClient("anything"));
        assertSame(defaultClient, factory.getClient("default"));
    }

    @Test
    void provideDefaultClient() {
        Map<String, KubernetesClient> beans = new HashMap<>();
        beans.put("default", defaultClientBean);
        when(ctx.getBeansOfType(KubernetesClient.class)).thenReturn(beans);

        factory.init();

        assertSame(defaultClientBean, factory.getClient("default"));
        assertSame(defaultClientBean, factory.getClient("anythingElse"));
    }

    @Test
    void createClients() {
        Map<String, KubernetesClient> beans = new HashMap<>();
        beans.put("one", clientOne);
        beans.put("two", clientTwo);
        when(ctx.getBeansOfType(KubernetesClient.class)).thenReturn(beans);

        factory.init();

        assertSame(clientOne, factory.getClient("one"));
        assertSame(clientTwo, factory.getClient("two"));

        KubernetesClient defaultClient = factory.getClient("default");
        assertNotNull(defaultClient, "Should have created a default client");

        assertSame(defaultClient, factory.getClient("someOtherName"));

        assertNotSame(defaultClient, clientOne);
        assertNotSame(defaultClient, clientTwo);
    }

    @Test
    void closesAllClients() {
        Map<String, KubernetesClient> beans = new HashMap<>();
        beans.put("one", clientOne);
        beans.put("default", defaultClientBean);
        when(ctx.getBeansOfType(KubernetesClient.class)).thenReturn(beans);

        factory.init();
        factory.shutdown();

        verify(clientOne, times(1)).close();
        verify(defaultClientBean, times(1)).close();
    }
}
