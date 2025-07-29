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

import static org.mockito.Mockito.*;

import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.k8swatcher.annotation.validate.AnnotationValidator;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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

        verify(informer1, atLeastOnce()).isRunning();
        verify(informer2, atLeastOnce()).isRunning();
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

    @Test
    @Timeout(5)
    void blockingWaitsUntilInformerStops() throws Exception {
        AtomicBoolean running = new AtomicBoolean(true);
        when(informerCreator.createInformers()).thenReturn(List.of(informer1));
        when(informer1.isRunning()).thenAnswer(invocation -> running.get());

        entrypoint.onStartUp(event);

        verify(informer1).start();

        Thread.sleep(500);
        running.set(false);
        Thread.sleep(1000);

        verify(informer1, atLeastOnce()).isRunning();
    }

    @Test
    @Timeout(5)
    void blockingWaitsForAllInformersToStop() throws Exception {
        AtomicBoolean running1 = new AtomicBoolean(true);
        AtomicBoolean running2 = new AtomicBoolean(true);

        when(informerCreator.createInformers()).thenReturn(List.of(informer1, informer2));
        when(informer1.isRunning()).thenAnswer(invocation -> running1.get());
        when(informer2.isRunning()).thenAnswer(invocation -> running2.get());

        entrypoint.onStartUp(event);

        verify(informer1).start();
        verify(informer2).start();

        running1.set(false);
        Thread.sleep(500);
        verify(informer2, atLeastOnce()).isRunning();
        verify(informer1, atLeastOnce()).isRunning();

        running2.set(false);
        Thread.sleep(1000);

        verify(informer1, atLeastOnce()).isRunning();
        verify(informer2, atLeastOnce()).isRunning();
    }

    @Test
    @Timeout(5)
    void runUntilStoppedCompletesWhenInformerAlreadyStopped() throws Exception {
        when(informerCreator.createInformers()).thenReturn(List.of(informer1));
        when(informer1.isRunning()).thenReturn(false);

        entrypoint.onStartUp(event);

        Thread.sleep(200);

        verify(informer1).start();
        verify(informer1, atLeastOnce()).isRunning();
    }
}
