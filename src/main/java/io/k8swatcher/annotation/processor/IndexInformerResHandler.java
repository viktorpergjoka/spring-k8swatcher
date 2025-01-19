package io.k8swatcher.annotation.processor;

import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.utils.internal.SerialExecutor;
import io.k8swatcher.annotation.EventType;
import io.k8swatcher.annotation.Watch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings("rawtypes")
class IndexInformerResHandler implements ResourceEventHandler {

    private final ApplicationContext ctx;
    private final Class<?> beanClass;
    private Map<EventType, List<Method>> methods;
    private SerialExecutor executor;

    public IndexInformerResHandler(ApplicationContext ctx, List<Method> watchMethods, Class<?> beanClass) {
        this.ctx = ctx;
        this.beanClass = beanClass;
        this.methods = watchMethods.stream().collect(Collectors.groupingBy(method -> method.getAnnotation(Watch.class)
                .event()));
        this.executor = new SerialExecutor(Executors.newSingleThreadExecutor());
    }

    @Override
    public void onAdd(Object obj) {
        methods.get(EventType.ADD).forEach(watchMethod -> {
            executor.execute(() -> {
                try {
                    Object instance = ctx.getBean(beanClass);
                    ReflectionUtils.invokeMethod(watchMethod, instance, obj);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    @Override
    public void onUpdate(Object oldObj, Object newObj) {
        methods.get(EventType.UPDATE).forEach(watchMethod -> {
            executor.execute(() -> {
                try {
                    Object instance = ctx.getBean(beanClass);
                    ReflectionUtils.invokeMethod(watchMethod, instance, oldObj, newObj);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    @Override
    public void onDelete(Object obj, boolean deletedFinalStateUnknown) {
        methods.get(EventType.DELETE).forEach(watchMethod -> {
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
        });
    }
}
