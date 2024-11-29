package io.k8swatcher.annotation.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.k8swatcher.annotation.cfg.InformerConfigurationProperty;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

// Integration Test
@SpringBootTest(classes = {InformerTestConfig.class})
@EnableKubernetesMockClient(https = false)
public class InformerTest {

    @Autowired
    private ApplicationContext testCtx;

    private KubeClientFactory clientFactory;

    private InformerCreator informerCreator;

    private KubernetesMockServer server;

    private KubernetesClient client;

    @Autowired
    private Map<String, String> resMaps;

    @BeforeEach
    public void setUp() {
        resMaps.clear();
        this.clientFactory = new KubeClientFactory(testCtx);
        clientFactory.init();
        this.client = clientFactory.getClient("default");

        InformerConfigurationProperty informerConfigurationProperty = new InformerConfigurationProperty();
        informerConfigurationProperty.postConstruct();

        Namespace testns = new NamespaceBuilder()
                .withNewMetadata()
                .withName("foo")
                .endMetadata()
                .build();
        client.namespaces().resource(testns).serverSideApply();

        informerCreator = new InformerCreator(testCtx, informerConfigurationProperty, clientFactory);
        List<SharedIndexInformer> informers = informerCreator.createInformers();
        informers.forEach(SharedIndexInformer::start);
    }

    @AfterEach
    public void afterAll() {
        client.pods().inNamespace("foo").delete();
    }

    @Test
    void testPodInformer() throws InterruptedException {
        assertEquals(resMaps.size(), 0);
        Pod pod = client.pods()
                .load(getClass().getResourceAsStream("/test-pod.yml"))
                .item();
        client.pods().resource(pod).serverSideApply();
        client.pods().resource(pod).waitUntilReady(5, TimeUnit.MINUTES);

        assertEquals("added", resMaps.get("ADD"));

        client.pods().inNamespace("foo").delete();
        Thread.sleep(5000);
        assertEquals("updated", resMaps.get("UPDATE"));
        assertEquals("deleted", resMaps.get("DELETE"));
    }
}
