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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.k8swatcher.annotation.validate.AnnotationValidator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
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

    @BeforeEach
    void setUp() {
        entrypoint = new InformerEntrypoint(validator, informerCreator);
    }

    @Test
    void shouldValidateAndStartInformersUntilTheyAreSynced() {
        when(informer1.hasSynced()).thenReturn(true);
        when(informer2.hasSynced()).thenReturn(true);
        when(informerCreator.createInformers()).thenReturn(List.of(informer1, informer2));

        entrypoint.start();

        InOrder inOrder = inOrder(validator);
        inOrder.verify(validator).validateInformerAnnotations();
        inOrder.verify(validator).validateWatchAnnotations();

        verify(informerCreator).createInformers();
        verify(informer1).start();
        verify(informer2).start();
        verify(informer1, atLeastOnce()).hasSynced();
        verify(informer2, atLeastOnce()).hasSynced();
    }

    @Test
    void shouldNotFailWhenThereAreNoInformers() {
        when(informerCreator.createInformers()).thenReturn(List.of());

        entrypoint.start();

        verify(validator).validateInformerAnnotations();
        verify(validator).validateWatchAnnotations();
        verify(informerCreator).createInformers();
    }

    @Test
    void shouldCloseAllInformersOnStop() {
        when(informerCreator.createInformers()).thenReturn(List.of(informer1, informer2));
        when(informer1.hasSynced()).thenReturn(true);
        when(informer2.hasSynced()).thenReturn(true);

        entrypoint.start();
        entrypoint.stop();

        verify(informer1).close();
        verify(informer2).close();
    }

    @Test
    void shouldReportRunningStateCorrectly() {
        when(informerCreator.createInformers()).thenReturn(List.of(informer1, informer2));
        when(informer1.hasSynced()).thenReturn(true);
        when(informer2.hasSynced()).thenReturn(true);

        entrypoint.start();

        when(informer1.isRunning()).thenReturn(false);
        when(informer2.isRunning()).thenReturn(true);
        assertThat(entrypoint.isRunning()).isTrue();

        when(informer1.isRunning()).thenReturn(false);
        when(informer2.isRunning()).thenReturn(false);
        assertThat(entrypoint.isRunning()).isFalse();
    }
}
