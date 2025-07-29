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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.k8swatcher.annotation.EventType;
import io.k8swatcher.annotation.Informer;
import io.k8swatcher.annotation.Watch;
import io.k8swatcher.annotation.cfg.InformerConfiguration;
import io.k8swatcher.annotation.cfg.InformerConfigurationProperty;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
class InformerCreatorTest {

    @Mock
    private ApplicationContext ctx;

    @Mock
    private InformerConfigurationProperty cfgProp;

    @Mock
    private KubeClientFactory kubeClientFactory;

    @Mock
    private KubernetesClient client;

    @Mock
    private MixedOperation<Namespace, KubernetesResourceList<Namespace>, Resource<Namespace>> nsOp;

    @Mock
    private SharedIndexInformer<Namespace> informer;

    private InformerCreator informerCreator;

    @BeforeEach
    void setUp() {
        informerCreator = new InformerCreator(ctx, cfgProp, kubeClientFactory);
    }

    @Informer
    static class TestBean {
        @Watch(event = EventType.ADD, resource = Namespace.class)
        public void onNamespace(Namespace ns) {}
    }

    @Test
    void createInformers_createsSharedIndexInformerForEachWatchedNamespace() {

        when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Collections.singletonMap("testBean", new TestBean()));

        InformerConfiguration defaultCfg = new InformerConfiguration(
                Collections.emptyMap(), Collections.emptyMap(), 1500L, "myClient", Set.of("ns-a", "ns-b"));
        when(cfgProp.getConfig()).thenReturn(Collections.singletonMap("default", defaultCfg));

        when(kubeClientFactory.getClient("myClient")).thenReturn(client);

        when(client.resources(Namespace.class)).thenReturn(nsOp);
        when(nsOp.inNamespace("ns-a")).thenReturn(nsOp);
        when(nsOp.inNamespace("ns-b")).thenReturn(nsOp);
        when(nsOp.runnableInformer(1500L)).thenReturn(informer);
        when(informer.addEventHandlerWithResyncPeriod(any(IndexInformerResHandler.class), eq(1500L)))
                .thenReturn(informer);

        var result = informerCreator.createInformers();
        assertEquals(2, result.size());

        verify(client, times(2)).resources(Namespace.class);

        verify(nsOp).inNamespace("ns-a");
        verify(nsOp).inNamespace("ns-b");

        verify(nsOp, times(2)).runnableInformer(1500L);
        verify(informer, times(2)).addEventHandlerWithResyncPeriod(any(IndexInformerResHandler.class), eq(1500L));
    }
}
