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

import io.fabric8.kubernetes.api.model.Namespace;
import io.k8swatcher.annotation.EventType;
import io.k8swatcher.annotation.Watch;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
class IndexInformerResHandlerTest {

    @Mock
    private ApplicationContext ctx;

    @Mock
    private TestBean testBean;

    private List<Method> allWatchMethods;
    private IndexInformerResHandler handlerAllEvents;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        allWatchMethods = List.of(
                TestBean.class.getMethod("onAdd", Namespace.class),
                TestBean.class.getMethod("onUpdate", Namespace.class, Namespace.class),
                TestBean.class.getMethod("onDeleteSingle", Namespace.class),
                TestBean.class.getMethod("onDeleteWithFlag", Namespace.class, boolean.class));
        handlerAllEvents = new IndexInformerResHandler(ctx, allWatchMethods, TestBean.class);
    }

    @Test
    void onAdd() {
        when(ctx.getBean(TestBean.class)).thenReturn(testBean);

        Namespace ns = new Namespace();
        handlerAllEvents.onAdd(ns);

        verify(testBean, timeout(1_000).times(1)).onAdd(ns);

        verify(testBean, never()).onUpdate(any(), any());
        verify(testBean, never()).onDeleteSingle(any());
        verify(testBean, never()).onDeleteWithFlag(any(), anyBoolean());
    }

    @Test
    void onUpdate() {
        when(ctx.getBean(TestBean.class)).thenReturn(testBean);

        Namespace oldObj = new Namespace();
        Namespace newObj = new Namespace();
        handlerAllEvents.onUpdate(oldObj, newObj);

        verify(testBean, timeout(1_000).times(1)).onUpdate(oldObj, newObj);

        verify(testBean, never()).onAdd(any());
        verify(testBean, never()).onDeleteSingle(any());
        verify(testBean, never()).onDeleteWithFlag(any(), anyBoolean());
    }

    @Test
    void onDelete() {
        when(ctx.getBean(TestBean.class)).thenReturn(testBean);

        Namespace ns = new Namespace();
        boolean flag = true;
        handlerAllEvents.onDelete(ns, flag);

        verify(testBean, timeout(1_000).times(1)).onDeleteSingle(ns);
        verify(testBean, timeout(1_000).times(1)).onDeleteWithFlag(ns, flag);

        verify(testBean, never()).onAdd(any());
        verify(testBean, never()).onUpdate(any(), any());
    }

    @Test
    void noMethodsForEvent() throws NoSuchMethodException {
        Method addOnly = TestBean.class.getMethod("onAdd", Namespace.class);
        IndexInformerResHandler handlerAddOnly = new IndexInformerResHandler(ctx, List.of(addOnly), TestBean.class);

        Namespace ns = new Namespace();
        handlerAddOnly.onUpdate(ns, ns);
        handlerAddOnly.onDelete(ns, false);

        verifyNoInteractions(testBean);
        verify(ctx, never()).getBean(TestBean.class);
    }

    public static class TestBean {
        @Watch(event = EventType.ADD, resource = Namespace.class)
        public void onAdd(Namespace obj) {}

        @Watch(event = EventType.UPDATE, resource = Namespace.class)
        public void onUpdate(Namespace oldObj, Namespace newObj) {}

        @Watch(event = EventType.DELETE, resource = Namespace.class)
        public void onDeleteSingle(Namespace obj) {}

        @Watch(event = EventType.DELETE, resource = Namespace.class)
        public void onDeleteWithFlag(Namespace obj, boolean deletedFinalStateUnknown) {}
    }
}
