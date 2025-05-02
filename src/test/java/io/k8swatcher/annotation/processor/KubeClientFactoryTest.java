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
