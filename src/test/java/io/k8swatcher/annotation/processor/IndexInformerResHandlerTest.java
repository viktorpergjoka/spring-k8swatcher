package io.k8swatcher.annotation.processor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.fabric8.kubernetes.api.model.Pod;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class IndexInformerResHandlerTest {

    private IndexInformerResHandler handler;

    @MockitoBean
    private ApplicationContext ctx;

    @MockitoBean
    private Class<?> beanClass;

    @MockitoBean
    private List<Method> methods = new ArrayList<>();

    @BeforeEach
    void setUp() {
        handler = new IndexInformerResHandler(ctx, methods, beanClass);
    }

    @Test
    void testOnAdd_NoExceptions() {
        Pod newPod = new Pod();
        assertDoesNotThrow(() -> handler.onAdd(newPod));
    }

    @Test
    void testOnUpdate_NoExceptions() {
        Pod oldPod = new Pod();
        Pod newPod = new Pod();
        assertDoesNotThrow(() -> handler.onUpdate(oldPod, newPod));
    }

    @Test
    void testOnDelete_NoExceptions() {
        Pod pod = new Pod();
        assertDoesNotThrow(() -> handler.onDelete(pod, true));
    }
}
