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

import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.utils.internal.SerialExecutor;
import io.k8swatcher.annotation.EventType;
import io.k8swatcher.annotation.Watch;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

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
        List<Method> addMethods = methods.get(EventType.ADD);
        if (addMethods != null) {
            addMethods.forEach(watchMethod -> executor.execute(() -> {
                try {
                    Object instance = ctx.getBean(beanClass);
                    ReflectionUtils.invokeMethod(watchMethod, instance, obj);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
    }

    @Override
    public void onUpdate(Object oldObj, Object newObj) {
        List<Method> updateMethods = methods.get(EventType.UPDATE);
        if (updateMethods != null) {
            updateMethods.forEach(watchMethod -> executor.execute(() -> {
                try {
                    Object instance = ctx.getBean(beanClass);
                    ReflectionUtils.invokeMethod(watchMethod, instance, oldObj, newObj);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
    }

    @Override
    public void onDelete(Object obj, boolean deletedFinalStateUnknown) {
        List<Method> deleteMethods = methods.get(EventType.DELETE);
        if (deleteMethods != null) {
            deleteMethods.forEach(watchMethod -> executor.execute(() -> {
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
            }));
        }
    }
}
