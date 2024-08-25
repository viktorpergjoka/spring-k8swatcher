package io.k8informer.processor;

import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.k8informer.annotation.EventType;
import io.k8informer.annotation.Watch;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

@AllArgsConstructor
@Slf4j
@SuppressWarnings("rawtypes")
class IndexInformerResHandler implements ResourceEventHandler {

    private ApplicationContext ctx;
    private Method watchMethod;
    private Class<?> beanClass;

    @Override
    public void onAdd(Object obj) {
        if (watchMethod.getAnnotation(Watch.class).event().equals(EventType.ADD)) {
            try {
                Object instance = ctx.getBean(beanClass);
                ReflectionUtils.invokeMethod(watchMethod, instance, obj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onUpdate(Object oldObj, Object newObj) {
        if (watchMethod.getAnnotation(Watch.class).event().equals(EventType.UPDATE)) {
            try {
                Object instance = ctx.getBean(beanClass);
                ReflectionUtils.invokeMethod(watchMethod, instance, oldObj, newObj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onDelete(Object obj, boolean deletedFinalStateUnknown) {
        if (watchMethod.getAnnotation(Watch.class).event().equals(EventType.DELETE)) {
            try {
                Object instance = ctx.getBean(beanClass);
                ReflectionUtils.invokeMethod(watchMethod, instance, obj, deletedFinalStateUnknown);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
