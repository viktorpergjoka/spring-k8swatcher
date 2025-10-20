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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.k8swatcher.annotation.validate.AnnotationValidator;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;

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

    @Mock
    private ApplicationReadyEvent applicationReadyEvent;

    private InformerEntrypoint informerEntrypoint;

    @BeforeEach
    void setUp() {
        informerEntrypoint = new InformerEntrypoint(validator, informerCreator);
    }

    @Test
    void onStartUp_validatesAnnotations() {
        when(informerCreator.createInformers()).thenReturn(Collections.emptyList());

        informerEntrypoint.onStartUp(applicationReadyEvent);

        verify(validator).validateInformerAnnotations();
        verify(validator).validateWatchAnnotations();
    }

    @Test
    void onStartUp_createsAndStartsInformers() {
        List<SharedIndexInformer> informers = Arrays.asList(informer1, informer2);
        when(informerCreator.createInformers()).thenReturn(informers);
        when(informer1.isRunning()).thenReturn(false);
        when(informer2.isRunning()).thenReturn(false);

        informerEntrypoint.onStartUp(applicationReadyEvent);

        verify(informerCreator).createInformers();
        verify(informer1).start();
        verify(informer2).start();
    }

    @Test
    void onStartUp_executesInCorrectOrder() {
        List<SharedIndexInformer> informers = Arrays.asList(informer1);
        when(informerCreator.createInformers()).thenReturn(informers);
        when(informer1.isRunning()).thenReturn(false);

        informerEntrypoint.onStartUp(applicationReadyEvent);

        InOrder inOrder = inOrder(validator, informerCreator, informer1);
        inOrder.verify(validator).validateInformerAnnotations();
        inOrder.verify(validator).validateWatchAnnotations();
        inOrder.verify(informerCreator).createInformers();
        inOrder.verify(informer1).start();
    }

    @Test
    void onStartUp_waitsUntilAllInformersStopRunning() {
        List<SharedIndexInformer> informers = Arrays.asList(informer1, informer2);
        when(informerCreator.createInformers()).thenReturn(informers);

        when(informer1.isRunning()).thenReturn(true, true, false);
        when(informer2.isRunning()).thenReturn(true, false, false);

        informerEntrypoint.onStartUp(applicationReadyEvent);

        verify(informer1, atLeast(3)).isRunning();
        verify(informer2, atLeast(2)).isRunning();
    }

    @Test
    void onStartUp_throwsRuntimeExceptionOnInterruption() {
        List<SharedIndexInformer> informers = Arrays.asList(informer1);
        when(informerCreator.createInformers()).thenReturn(informers);
        when(informer1.isRunning()).thenReturn(true);

        Thread.currentThread().interrupt();

        assertThrows(RuntimeException.class, () -> informerEntrypoint.onStartUp(applicationReadyEvent));

        Thread.interrupted();
    }

    @Test
    void onStartUp_handlesEmptyInformerList() {
        when(informerCreator.createInformers()).thenReturn(Collections.emptyList());

        informerEntrypoint.onStartUp(applicationReadyEvent);

        verify(validator).validateInformerAnnotations();
        verify(validator).validateWatchAnnotations();
        verify(informerCreator).createInformers();
        verifyNoMoreInteractions(informer1, informer2);
    }

    @Test
    void shutdown_closesAllInformers() {
        List<SharedIndexInformer> informers = Arrays.asList(informer1, informer2);
        when(informerCreator.createInformers()).thenReturn(informers);
        when(informer1.isRunning()).thenReturn(false);
        when(informer2.isRunning()).thenReturn(false);

        informerEntrypoint.onStartUp(applicationReadyEvent);
        informerEntrypoint.shutdown();

        verify(informer1).close();
        verify(informer2).close();
    }

    @Test
    void shutdown_handlesEmptyInformerList() {
        when(informerCreator.createInformers()).thenReturn(Collections.emptyList());

        informerEntrypoint.onStartUp(applicationReadyEvent);
        informerEntrypoint.shutdown();

        verifyNoInteractions(informer1, informer2);
    }
}
