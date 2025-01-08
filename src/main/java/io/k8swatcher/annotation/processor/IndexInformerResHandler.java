package io.k8swatcher.annotation.processor;

import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.k8swatcher.annotation.EventType;
import io.k8swatcher.annotation.Watch;
import io.k8swatcher.annotation.processor.executor.InformerEventExecutor;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

@Slf4j
@SuppressWarnings("rawtypes")
class IndexInformerResHandler implements ResourceEventHandler {

    private final ApplicationContext ctx;
    private final Method watchMethod;
    private final Class<?> beanClass;
    private InformerEventExecutor executor;

    public IndexInformerResHandler(ApplicationContext ctx, Method watchMethod, Class<?> beanClass) {
        this.ctx = ctx;
        this.watchMethod = watchMethod;
        this.beanClass = beanClass;
        this.executor = new InformerEventExecutor(Executors.newSingleThreadExecutor());
    }

    @Override
    public void onAdd(Object obj) {
        if (watchMethod.getAnnotation(Watch.class).event().equals(EventType.ADD)) {
            executor.execute(() -> {
                try {
                    Object instance = ctx.getBean(beanClass);
                    ReflectionUtils.invokeMethod(watchMethod, instance, obj);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    public void onUpdate(Object oldObj, Object newObj) {
        if (watchMethod.getAnnotation(Watch.class).event().equals(EventType.UPDATE)) {
            executor.execute(() -> {
                try {
                    Object instance = ctx.getBean(beanClass);
                    ReflectionUtils.invokeMethod(watchMethod, instance, oldObj, newObj);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    public void onDelete(Object obj, boolean deletedFinalStateUnknown) {
        if (watchMethod.getAnnotation(Watch.class).event().equals(EventType.DELETE)) {
            executor.execute(() -> {
                try {
                    Object instance = ctx.getBean(beanClass);
                    int parameterCount = watchMethod.getParameterCount();
                    if (parameterCount == 1) {
                        ReflectionUtils.invokeMethod(watchMethod, instance, obj);
                    } else {
                        ReflectionUtils.invokeMethod(watchMethod, instance, obj, deletedFinalStateUnknown);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
