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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.k8swatcher.annotation.EventType;
import io.k8swatcher.annotation.Informer;
import io.k8swatcher.annotation.Watch;
import io.k8swatcher.annotation.cfg.InformerConfiguration;
import io.k8swatcher.annotation.cfg.InformerConfigurationProperty;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Test
    void createInformers_usesNamespaceLabels_whenNsNamesEmpty() {
        when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Collections.singletonMap("testBean", new TestBean()));

        InformerConfiguration defaultCfg = new InformerConfiguration(
                Map.of("env", "prod"), Collections.emptyMap(), 1500L, "", Collections.emptySet());
        when(cfgProp.getConfig()).thenReturn(Collections.singletonMap("default", defaultCfg));

        when(kubeClientFactory.getClient("default")).thenReturn(client);

        Namespace ns1 = new Namespace();
        ns1.setMetadata(new io.fabric8.kubernetes.api.model.ObjectMetaBuilder()
                .withName("prod-ns1")
                .build());
        Namespace ns2 = new Namespace();
        ns2.setMetadata(new io.fabric8.kubernetes.api.model.ObjectMetaBuilder()
                .withName("prod-ns2")
                .build());

        NamespaceList nsList = new NamespaceList();
        nsList.setItems(List.of(ns1, ns2));

        MixedOperation<Namespace, NamespaceList, Resource<Namespace>> namespacesOp = mock(MixedOperation.class);
        FilterWatchListDeletable<Namespace, NamespaceList, Resource<Namespace>> filteredOp =
                mock(FilterWatchListDeletable.class);

        when(client.namespaces()).thenReturn(namespacesOp);
        when(namespacesOp.withLabels(Map.of("env", "prod"))).thenReturn(filteredOp);
        when(filteredOp.list()).thenReturn(nsList);

        when(client.resources(Namespace.class)).thenReturn(nsOp);
        when(nsOp.inNamespace(anyString())).thenReturn(nsOp);
        when(nsOp.runnableInformer(1500L)).thenReturn(informer);
        when(informer.addEventHandlerWithResyncPeriod(any(IndexInformerResHandler.class), eq(1500L)))
                .thenReturn(informer);

        var result = informerCreator.createInformers();
        assertEquals(2, result.size());

        verify(namespacesOp).withLabels(Map.of("env", "prod"));
    }

    @Test
    void createInformers_queriesAllNamespaces_whenLabelsAndNamesEmpty() {
        when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Collections.singletonMap("testBean", new TestBean()));

        InformerConfiguration defaultCfg =
                new InformerConfiguration(Collections.emptyMap(), Collections.emptyMap(), 1500L, "", Set.of());
        when(cfgProp.getConfig()).thenReturn(Collections.singletonMap("default", defaultCfg));

        when(kubeClientFactory.getClient("default")).thenReturn(client);

        Namespace ns1 = new Namespace();
        ns1.setMetadata(new io.fabric8.kubernetes.api.model.ObjectMetaBuilder()
                .withName("default")
                .build());

        NamespaceList nsList = new NamespaceList();
        nsList.setItems(List.of(ns1));

        MixedOperation<Namespace, NamespaceList, Resource<Namespace>> namespacesOp = mock(MixedOperation.class);

        when(client.namespaces()).thenReturn(namespacesOp);
        when(namespacesOp.list()).thenReturn(nsList);

        when(client.resources(Namespace.class)).thenReturn(nsOp);
        when(nsOp.inNamespace(anyString())).thenReturn(nsOp);
        when(nsOp.runnableInformer(1500L)).thenReturn(informer);
        when(informer.addEventHandlerWithResyncPeriod(any(IndexInformerResHandler.class), eq(1500L)))
                .thenReturn(informer);

        var result = informerCreator.createInformers();
        assertEquals(1, result.size());

        verify(namespacesOp).list();
    }

    @Test
    void createInformers_usesResourceLabels_whenProvided() {
        when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Collections.singletonMap("testBean", new TestBean()));

        InformerConfiguration defaultCfg =
                new InformerConfiguration(Collections.emptyMap(), Map.of("app", "myapp"), 1500L, "", Set.of("ns-a"));
        when(cfgProp.getConfig()).thenReturn(Collections.singletonMap("default", defaultCfg));

        when(kubeClientFactory.getClient("default")).thenReturn(client);

        when(client.resources(Namespace.class)).thenReturn(nsOp);
        when(nsOp.inNamespace("ns-a")).thenReturn(nsOp);

        FilterWatchListDeletable filteredOp = mock(FilterWatchListDeletable.class);
        when(nsOp.withLabels(Map.of("app", "myapp"))).thenReturn(filteredOp);
        when(filteredOp.runnableInformer(1500L)).thenReturn(informer);
        when(informer.addEventHandlerWithResyncPeriod(any(IndexInformerResHandler.class), eq(1500L)))
                .thenReturn(informer);

        var result = informerCreator.createInformers();
        assertEquals(1, result.size());

        verify(nsOp).withLabels(Map.of("app", "myapp"));
        verify(filteredOp).runnableInformer(1500L);
    }

    @Test
    void createInformers_returnsEmptyList_whenNoWatchMethods() {
        @Informer
        class BeanWithoutWatch {}

        when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Collections.singletonMap("bean", new BeanWithoutWatch()));

        var result = informerCreator.createInformers();
        assertTrue(result.isEmpty());
    }

    @Informer(
            name = "custom",
            clientName = "customClient",
            nsLabels = {"env=test"},
            resLabels = {"app=test"},
            nsNames = {"test-ns"},
            resyncPeriod = 2000L)
    static class TestBeanWithAnnotationConfig {
        @Watch(event = EventType.ADD, resource = Pod.class)
        public void onPod(Pod pod) {}
    }

    @Test
    void createInformers_fallsBackToAnnotationValues_whenConfigEmpty() {
        when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Collections.singletonMap("testBean", new TestBeanWithAnnotationConfig()));

        Map<String, InformerConfiguration> configs = new HashMap<>();
        configs.put("default", new InformerConfiguration());
        configs.put("custom", new InformerConfiguration());
        when(cfgProp.getConfig()).thenReturn(configs);

        when(kubeClientFactory.getClient("customClient")).thenReturn(client);

        MixedOperation podOp = mock(MixedOperation.class);
        when(client.resources(Pod.class)).thenReturn(podOp);
        when(podOp.inNamespace("test-ns")).thenReturn(podOp);

        FilterWatchListDeletable filteredOp = mock(FilterWatchListDeletable.class);
        when(podOp.withLabels(Map.of("app", "test"))).thenReturn(filteredOp);
        when(filteredOp.runnableInformer(2000L)).thenReturn(informer);
        when(informer.addEventHandlerWithResyncPeriod(any(IndexInformerResHandler.class), eq(2000L)))
                .thenReturn(informer);

        var result = informerCreator.createInformers();
        assertEquals(1, result.size());

        verify(kubeClientFactory).getClient("customClient");
        verify(filteredOp).runnableInformer(2000L);
    }

    @Informer(resyncPeriod = 500L)
    static class TestBeanWithSmallResyncPeriod {
        @Watch(event = EventType.ADD, resource = Namespace.class)
        public void onNamespace(Namespace ns) {}
    }

    @Test
    void createInformers_adjustsResyncPeriod_whenTooSmall() {
        when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Collections.singletonMap("testBean", new TestBeanWithSmallResyncPeriod()));

        InformerConfiguration defaultCfg =
                new InformerConfiguration(Collections.emptyMap(), Collections.emptyMap(), null, "", Set.of("ns-a"));
        when(cfgProp.getConfig()).thenReturn(Collections.singletonMap("default", defaultCfg));

        when(kubeClientFactory.getClient("default")).thenReturn(client);

        when(client.resources(Namespace.class)).thenReturn(nsOp);
        when(nsOp.inNamespace("ns-a")).thenReturn(nsOp);
        when(nsOp.runnableInformer(1000L)).thenReturn(informer);
        when(informer.addEventHandlerWithResyncPeriod(any(IndexInformerResHandler.class), eq(1000L)))
                .thenReturn(informer);

        var result = informerCreator.createInformers();
        assertEquals(1, result.size());

        verify(nsOp).runnableInformer(1000L);
        verify(informer).addEventHandlerWithResyncPeriod(any(IndexInformerResHandler.class), eq(1000L));
    }

    @Informer
    static class TestBeanWithMultipleResources {
        @Watch(event = EventType.ADD, resource = Namespace.class)
        public void onNamespace(Namespace ns) {}

        @Watch(event = EventType.UPDATE, resource = Pod.class)
        public void onPod(Pod oldPod, Pod newPod) {}
    }

    @Test
    void createInformers_createsInformersForMultipleResources() {
        when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Collections.singletonMap("testBean", new TestBeanWithMultipleResources()));

        InformerConfiguration defaultCfg =
                new InformerConfiguration(Collections.emptyMap(), Collections.emptyMap(), 1500L, "", Set.of("ns-a"));
        when(cfgProp.getConfig()).thenReturn(Collections.singletonMap("default", defaultCfg));

        when(kubeClientFactory.getClient("default")).thenReturn(client);

        when(client.resources(Namespace.class)).thenReturn(nsOp);
        when(nsOp.inNamespace("ns-a")).thenReturn(nsOp);

        MixedOperation podOp = mock(MixedOperation.class);
        when(client.resources(Pod.class)).thenReturn(podOp);
        when(podOp.inNamespace("ns-a")).thenReturn(podOp);

        when(nsOp.runnableInformer(1500L)).thenReturn(informer);
        when(podOp.runnableInformer(1500L)).thenReturn(informer);
        when(informer.addEventHandlerWithResyncPeriod(any(IndexInformerResHandler.class), eq(1500L)))
                .thenReturn(informer);

        var result = informerCreator.createInformers();
        assertEquals(2, result.size());

        verify(client).resources(Namespace.class);
        verify(client).resources(Pod.class);
    }
}
