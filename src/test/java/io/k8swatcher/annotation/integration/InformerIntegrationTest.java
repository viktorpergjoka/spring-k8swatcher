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
package io.k8swatcher.annotation.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.k8swatcher.annotation.EventType;
import io.k8swatcher.annotation.Informer;
import io.k8swatcher.annotation.Watch;
import io.k8swatcher.annotation.cfg.InformerConfiguration;
import io.k8swatcher.annotation.cfg.InformerConfigurationProperty;
import io.k8swatcher.annotation.processor.InformerCreator;
import io.k8swatcher.annotation.processor.KubeClientFactory;
import io.k8swatcher.annotation.validate.AnnotationValidator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

@EnableKubernetesMockClient
class InformerIntegrationTest {

    private KubernetesMockServer server;
    private KubernetesClient client;

    private ApplicationContext ctx;
    private KubeClientFactory kubeClientFactory;
    private InformerConfigurationProperty cfgProp;
    private AnnotationValidator validator;

    @Informer(nsNames = {"test-ns"})
    static class SimpleInformerBean {
        @Watch(event = EventType.ADD, resource = Namespace.class)
        public void onNamespaceAdd(Namespace ns) {}
    }

    @Informer(
            name = "multi",
            nsNames = {"ns1", "ns2"})
    static class MultiResourceInformerBean {
        @Watch(event = EventType.ADD, resource = Namespace.class)
        public void onNamespaceAdd(Namespace ns) {}

        @Watch(event = EventType.UPDATE, resource = Namespace.class)
        public void onNamespaceUpdate(Namespace oldNs, Namespace newNs) {}

        @Watch(event = EventType.DELETE, resource = Pod.class)
        public void onPodDelete(Pod pod) {}
    }

    @Informer(nsLabels = {"invalid"})
    static class InvalidInformerBean {
        @Watch(event = EventType.ADD, resource = Namespace.class)
        public void onAdd(Namespace ns) {}
    }

    @BeforeEach
    void setUp() {
        ctx = mock(ApplicationContext.class);

        cfgProp = new InformerConfigurationProperty();
        cfgProp.postConstruct();

        kubeClientFactory = mock(KubeClientFactory.class);
        when(kubeClientFactory.getClient(anyString())).thenReturn(client);

        validator = new AnnotationValidator(ctx, kubeClientFactory, cfgProp);
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.informers().stopAllRegisteredInformers();
        }
    }

    @Test
    void fullWorkflow_validatesInformers() {
        when(ctx.getBeansWithAnnotation(Informer.class)).thenReturn(Map.of("simpleBean", new SimpleInformerBean()));

        validator.init();

        assertDoesNotThrow(() -> validator.validateInformerAnnotations());
        assertDoesNotThrow(() -> validator.validateWatchAnnotations());
        assertDoesNotThrow(() -> validator.validateHasConfigName());
    }

    @Test
    void fullWorkflow_multipleBeansAndResources() {
        when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Map.of(
                        "simpleBean", new SimpleInformerBean(),
                        "multiBean", new MultiResourceInformerBean()));

        InformerConfiguration multiConfig = new InformerConfiguration(
                Collections.emptyMap(), Collections.emptyMap(), 2000L, "", Set.of("ns1", "ns2"));
        cfgProp.getConfig().put("multi", multiConfig);

        validator.init();

        assertDoesNotThrow(() -> validator.validateInformerAnnotations());
        assertDoesNotThrow(() -> validator.validateWatchAnnotations());
        assertDoesNotThrow(() -> validator.validateHasConfigName());
    }

    @Test
    void fullWorkflow_validationFails_whenInvalidAnnotation() {
        when(ctx.getBeansWithAnnotation(Informer.class)).thenReturn(Map.of("invalidBean", new InvalidInformerBean()));

        validator.init();

        assertThrows(Exception.class, () -> validator.validateInformerAnnotations());
    }

    @Test
    void configurationProperty_defaultConfigCreated() {
        InformerConfigurationProperty prop = new InformerConfigurationProperty();

        prop.postConstruct();

        assertTrue(prop.getConfig().containsKey("default"));
        assertNotNull(prop.getConfig().get("default"));
    }

    @Test
    void informerWithMockServer_canCreateNamespaceInformer() {
        Namespace testNs = new NamespaceBuilder()
                .withNewMetadata()
                .withName("test-namespace")
                .addToLabels("env", "test")
                .endMetadata()
                .build();

        io.fabric8.kubernetes.api.model.NamespaceList nsList = new io.fabric8.kubernetes.api.model.NamespaceList();
        nsList.setItems(List.of(testNs));

        server.expect()
                .get()
                .withPath("/api/v1/namespaces?labelSelector=env%3Dtest")
                .andReturn(200, nsList)
                .once();

        InformerConfiguration cfg = new InformerConfiguration(
                Map.of("env", "test"), Collections.emptyMap(), 1000L, "default", Collections.emptySet());
        cfgProp.getConfig().put("default", cfg);

        SharedIndexInformer<Namespace> informer =
                client.namespaces().withLabels(Map.of("env", "test")).runnableInformer(1000L);

        assertNotNull(informer);
        informer.start();
        informer.stop();
    }

    @Test
    void informerWithMockServer_canCreatePodInformer() {
        Pod testPod = new PodBuilder()
                .withNewMetadata()
                .withName("test-pod")
                .withNamespace("test-ns")
                .endMetadata()
                .build();

        io.fabric8.kubernetes.api.model.PodList podList = new io.fabric8.kubernetes.api.model.PodList();
        podList.setItems(List.of(testPod));

        server.expect()
                .get()
                .withPath("/api/v1/namespaces/test-ns/pods")
                .andReturn(200, podList)
                .once();

        SharedIndexInformer<Pod> informer = client.pods().inNamespace("test-ns").runnableInformer(1000L);

        informer.addEventHandler(new io.fabric8.kubernetes.client.informers.ResourceEventHandler<>() {
            @Override
            public void onAdd(Pod pod) {}

            @Override
            public void onUpdate(Pod oldPod, Pod newPod) {}

            @Override
            public void onDelete(Pod pod, boolean deletedFinalStateUnknown) {}
        });

        assertNotNull(informer);
        informer.start();
        informer.stop();
    }

    @Test
    void kubeClientFactory_retrievesMockClient() {
        ApplicationContext realCtx = mock(ApplicationContext.class);
        KubeClientFactory factory = new KubeClientFactory(realCtx);

        Map<String, KubernetesClient> clientMap = new java.util.HashMap<>();
        clientMap.put("mockClient", client);
        clientMap.put("default", client);

        when(realCtx.getBeansOfType(KubernetesClient.class)).thenReturn(clientMap);

        factory.init();

        KubernetesClient retrievedClient = factory.getClient("mockClient");
        assertNotNull(retrievedClient);
        assertSame(client, retrievedClient);

        KubernetesClient defaultClient = factory.getClient("default");
        assertNotNull(defaultClient);
        assertSame(client, defaultClient);
    }

    @Test
    void informerCreator_createsInformersWithMockServer() {
        Namespace ns1 = new NamespaceBuilder()
                .withNewMetadata()
                .withName("ns1")
                .endMetadata()
                .build();

        Namespace ns2 = new NamespaceBuilder()
                .withNewMetadata()
                .withName("ns2")
                .endMetadata()
                .build();

        io.fabric8.kubernetes.api.model.NamespaceList nsList = new io.fabric8.kubernetes.api.model.NamespaceList();
        nsList.setItems(List.of(ns1, ns2));

        server.expect()
                .get()
                .withPath("/api/v1/namespaces")
                .andReturn(200, nsList)
                .always();

        InformerConfiguration defaultCfg = new InformerConfiguration(
                Collections.emptyMap(), Collections.emptyMap(), 1000L, "", Set.of("ns1", "ns2"));
        cfgProp.getConfig().put("default", defaultCfg);

        when(ctx.getBeansWithAnnotation(Informer.class)).thenReturn(Map.of("simpleBean", new SimpleInformerBean()));
        when(ctx.getBean(SimpleInformerBean.class)).thenReturn(new SimpleInformerBean());

        InformerCreator informerCreator = new InformerCreator(ctx, cfgProp, kubeClientFactory);

        List<SharedIndexInformer<?>> informers =
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(informerCreator, "createInformers");

        assertNotNull(informers);
        assertFalse(informers.isEmpty());

        informers.forEach(SharedIndexInformer::stop);
    }
}
