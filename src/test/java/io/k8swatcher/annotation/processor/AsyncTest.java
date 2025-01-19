package io.k8swatcher.annotation.processor;

import static org.junit.Assert.assertEquals;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.k8swatcher.annotation.cfg.InformerConfigurationProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

//@SpringBootTest(classes = {InformerTestConfig.class})
public class AsyncTest {
/*
    @Autowired
    private ApplicationContext testCtx;

    private KubeClientFactory clientFactory;

    private InformerCreator informerCreator;

    private KubernetesClient client;

    private List<Pod> pods;

    @Autowired
    private AtomicInteger eventsCount;

    @BeforeEach
    public void setUp() {
        pods = new ArrayList<>();
        eventsCount.set(0);
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
        client.namespaces().withName("foo").delete();
    }

    @Test
    public void testEventCount() throws InterruptedException {
        Pod pod = client.pods()
                .load(getClass().getResourceAsStream("/events.yml"))
                .item();

        IntStream.range(0, 5).forEach((index) -> {
            pods.add(client.pods()
                    .resource(pod.edit()
                            .editMetadata()
                            .withName("pod-" + Math.random())
                            .endMetadata()
                            .build())
                    .serverSideApply());
        });
        pods.forEach(currPod -> client.pods()
                .resource(currPod)
                .waitUntilCondition(p -> p.getStatus().getPhase().equals("Running"), 1, TimeUnit.MINUTES));
        client.resourceList(pods).delete();
        client.resourceList(pods).waitUntilCondition(Objects::isNull, 1, TimeUnit.MINUTES);
        Thread.sleep(5000);
        assertEquals(15, eventsCount.get());
    }
 */
}
