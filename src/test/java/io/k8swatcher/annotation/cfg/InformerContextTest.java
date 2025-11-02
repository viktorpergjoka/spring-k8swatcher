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
package io.k8swatcher.annotation.cfg;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.k8swatcher.annotation.Informer;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InformerContextTest {

    @Informer
    static class TestBean {}

    @Test
    void constructor_setsAllFields() {
        Class<?> beanClass = TestBean.class;
        Informer informer = TestBean.class.getAnnotation(Informer.class);
        InformerConfiguration cfg = new InformerConfiguration(
                Map.of("ns", "label"), Map.of("res", "label"), 2000L, "client", Set.of("ns1"));
        KubernetesClient client = mock(KubernetesClient.class);

        InformerContext context = new InformerContext(beanClass, informer, cfg, client);

        assertEquals(beanClass, context.getBeanClass());
        assertEquals(informer, context.getInformer());
        assertEquals(cfg, context.getCfg());
        assertEquals(client, context.getClient());
    }

    @Test
    void getters_returnCorrectValues() {
        Class<?> beanClass = TestBean.class;
        Informer informer = TestBean.class.getAnnotation(Informer.class);
        InformerConfiguration cfg = new InformerConfiguration();
        KubernetesClient client = mock(KubernetesClient.class);

        InformerContext context = new InformerContext(beanClass, informer, cfg, client);

        assertNotNull(context.getBeanClass());
        assertNotNull(context.getInformer());
        assertNotNull(context.getCfg());
        assertNotNull(context.getClient());
    }

    @Test
    void equals_returnsTrueForSameContent() {
        Class<?> beanClass = TestBean.class;
        Informer informer = TestBean.class.getAnnotation(Informer.class);
        InformerConfiguration cfg = new InformerConfiguration();
        KubernetesClient client = mock(KubernetesClient.class);

        InformerContext context1 = new InformerContext(beanClass, informer, cfg, client);
        InformerContext context2 = new InformerContext(beanClass, informer, cfg, client);

        assertEquals(context1, context2);
    }

    @Test
    void hashCode_isSameForEqualObjects() {
        Class<?> beanClass = TestBean.class;
        Informer informer = TestBean.class.getAnnotation(Informer.class);
        InformerConfiguration cfg = new InformerConfiguration();
        KubernetesClient client = mock(KubernetesClient.class);

        InformerContext context1 = new InformerContext(beanClass, informer, cfg, client);
        InformerContext context2 = new InformerContext(beanClass, informer, cfg, client);

        assertEquals(context1.hashCode(), context2.hashCode());
    }
}
