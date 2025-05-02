package io.k8swatcher.annotation.processor;

import static org.mockito.Mockito.*;

import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.k8swatcher.annotation.validate.AnnotationValidator;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;

@ExtendWith(MockitoExtension.class)
class InformerEntrypointTest {

    @Mock
    private AnnotationValidator validator;

    @Mock
    private InformerCreator informerCreator;

    @Mock
    private SharedIndexInformer informer1;

    @Mock
    private SharedIndexInformer informer2;

    private InformerEntrypoint entrypoint;
    private ApplicationReadyEvent event;

    @BeforeEach
    void setUp() {
        entrypoint = new InformerEntrypoint(validator, informerCreator);

        SpringApplication app = new SpringApplication();
        ConfigurableApplicationContext ctx = mock(ConfigurableApplicationContext.class);
        event = new ApplicationReadyEvent(app, new String[] {}, ctx, Duration.ZERO);
    }

    @Test
    void onStartUpWithNoInformers_() {
        when(informerCreator.createInformers()).thenReturn(Collections.emptyList());

        entrypoint.onStartUp(event);

        verify(validator).validateInformerAnnotations();
        verify(validator).validateWatchAnnotations();
        verify(informerCreator).createInformers();

        verifyNoInteractions(informer1, informer2);
    }

    @Test
    void onStartUpWithInformers() {
        List<SharedIndexInformer> informers = List.of(informer1, informer2);
        when(informerCreator.createInformers()).thenReturn(informers);

        when(informer1.isRunning()).thenReturn(false);
        when(informer2.isRunning()).thenReturn(false);

        entrypoint.onStartUp(event);

        verify(validator).validateInformerAnnotations();
        verify(validator).validateWatchAnnotations();
        verify(informerCreator).createInformers();

        verify(informer1).start();
        verify(informer2).start();

        verify(informer1).isRunning();
        verify(informer2).isRunning();
    }

    @Test
    void shutdownClosesAllInformers() {
        List<SharedIndexInformer> informers = List.of(informer1, informer2);
        when(informerCreator.createInformers()).thenReturn(informers);
        when(informer1.isRunning()).thenReturn(false);
        when(informer2.isRunning()).thenReturn(false);

        entrypoint.onStartUp(event);

        entrypoint.shutdown();

        verify(informer1).close();
        verify(informer2).close();
    }
}
