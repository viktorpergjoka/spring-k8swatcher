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
package io.k8swatcher.annotation.validate;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.k8swatcher.annotation.EventType;
import io.k8swatcher.annotation.Informer;
import io.k8swatcher.annotation.Watch;
import io.k8swatcher.annotation.cfg.InformerConfigurationProperty;
import io.k8swatcher.annotation.processor.KubeClientFactory;
import jakarta.annotation.PostConstruct;
import java.lang.reflect.MalformedParametersException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class AnnotationValidator {

    private final ApplicationContext ctx;
    private final KubeClientFactory clients;
    private Map<String, Object> informerBeansMap;
    private InformerConfigurationProperty informerConfigurationProperty;

    public AnnotationValidator(
            ApplicationContext ctx,
            KubeClientFactory clients,
            InformerConfigurationProperty informerConfigurationProperty) {
        this.ctx = ctx;
        this.clients = clients;
        informerBeansMap = new HashMap<>();
        this.informerConfigurationProperty = informerConfigurationProperty;
    }

    @PostConstruct
    public void init() {
        informerBeansMap = ctx.getBeansWithAnnotation(Informer.class);
    }

    public void validateInformerAnnotations() {
        for (Object bean : informerBeansMap.values()) {
            Class<?> beanClass = bean.getClass();
            Informer informer = beanClass.getAnnotation(Informer.class);
            KubernetesClient client = clients.getClient(informer.clientName());
            if (client == null) {
                throw new RuntimeException("Could not find Kubernetes client with name " + informer.clientName());
            }
            validateLabels(informer, beanClass);
        }
    }

    public void validateWatchAnnotations() {
        for (Object bean : informerBeansMap.values()) {
            Class<?> beanClass = bean.getClass();
            Method[] methods = beanClass.getMethods();
            List<Method> k8watchMethods = Arrays.stream(methods)
                    .filter(method -> method.isAnnotationPresent(Watch.class))
                    .toList();
            if (k8watchMethods.isEmpty()) {
                log.warn("No @Watch annotated methods found in class {}", beanClass.getName());
            }
            for (Method method : k8watchMethods) {
                Watch watch = method.getAnnotation(Watch.class);
                Class<? extends KubernetesResource> resource = watch.resource();
                EventType event = watch.event();

                switch (event) {
                    case ADD -> checkAddParams(beanClass, method, resource);
                    case UPDATE -> checkUpdateParams(beanClass, method, resource);
                    case DELETE -> checkDeleteParams(beanClass, method, resource);
                }
            }
        }
    }

    public void validateHasConfigName() {
        for (Object bean : informerBeansMap.values()) {
            Class<?> beanClass = bean.getClass();
            Informer informer = beanClass.getAnnotation(Informer.class);
            String name = informer.name();
            if (StringUtils.hasText(name)) {
                if (!informerConfigurationProperty.getConfig().containsKey(name)) {
                    throw new IllegalArgumentException("In class " + beanClass.getName() + " " + name
                            + " is not defined under k8swatcher.config." + name);
                }
            }
        }
    }

    private void checkAddParams(Class<?> beanClass, Method method, Class<? extends KubernetesResource> type) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1) {
            throw new MalformedParametersException("Invalid number of parameters for method "
                    + method.getName() + " in class " + beanClass.getName()
                    + " . Expected 1, got " + parameterTypes.length + ". Signature should be " + method.getName() + "("
                    + type.getSimpleName() + " "
                    + type.getSimpleName().toLowerCase() + ")");
        }
        Class<?> parameterType = parameterTypes[0];
        checkIsAssignableFrom(beanClass, method, parameterType, type);
    }

    private void checkUpdateParams(Class<?> beanClass, Method method, Class<? extends KubernetesResource> type) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 2) {
            throw new MalformedParametersException("Invalid number of parameters for method "
                    + method.getName() + " in class " + beanClass.getName()
                    + " . Expected 2, got "
                    + parameterTypes.length
                    + ". Signature should be " + method.getName() + "("
                    + type.getSimpleName() + " old"
                    + type.getSimpleName() + ", " + type.getSimpleName() + " new" + type.getSimpleName() + ")");
        }
        Class<?> firstParam = parameterTypes[0];
        Class<?> secondParam = parameterTypes[1];
        checkIsAssignableFrom(beanClass, method, firstParam, type);
        checkIsAssignableFrom(beanClass, method, secondParam, type);
    }

    private void checkDeleteParams(Class<?> beanClass, Method method, Class<? extends KubernetesResource> type) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        int paramsLength = parameterTypes.length;
        if (paramsLength < 1 || paramsLength > 2) {
            throw new MalformedParametersException("Invalid number of parameters for method "
                    + method.getName() + " in class " + beanClass.getName()
                    + " . Expected 1, got " + parameterTypes.length + ". Signature should be " + method.getName() + "("
                    + type.getSimpleName() + " "
                    + type.getSimpleName().toLowerCase() + ")");
        }
        Class<?> firstParam = parameterTypes[0];
        checkIsAssignableFrom(beanClass, method, firstParam, type);

        if (paramsLength == 2) {
            Class<?> secondParam = parameterTypes[1];
            if (!secondParam.getTypeName().equals("boolean")
                    && !secondParam.getTypeName().equals("java.lang.Boolean")) {
                throw new MalformedParametersException(secondParam.getTypeName() + " is not boolean");
            }
        }
    }

    private void checkIsAssignableFrom(
            Class<?> beanClass, Method method, Class<?> param, Class<? extends KubernetesResource> type) {
        if (!param.isAssignableFrom(type)) {
            throw new MalformedParametersException(param.getTypeName()
                    + " is not a type or subtype of "
                    + type.getTypeName()
                    + " in method "
                    + method.getName()
                    + " in class "
                    + beanClass.getName());
        }
    }

    private void validateLabels(Informer informer, Class<?> beanClass) {
        String[] nsLabels = informer.nsLabels();
        String[] resLabels = informer.resLabels();
        String[] nsNames = informer.nsNames();

        Consumer<String[]> validateLabel =
                (String[] labels) -> Arrays.asList(labels).forEach(label -> {
                    String[] splittedLabel = label.split("=");
                    if (splittedLabel.length != 2) {
                        throw new MalformedParametersException("Invalid label for "
                                + label
                                + "in class "
                                + beanClass.getName()
                                + ". Format has to be key=value");
                    }
                });
        validateLabel.accept(nsLabels);
        validateLabel.accept(resLabels);

        checkForDuplicateKey(nsNames, beanClass);
        checkForDuplicateKey(nsLabels, beanClass);
        checkForDuplicateKey(resLabels, beanClass);
    }

    private void checkForDuplicateKey(String[] labelValues, Class<?> beanClass) {
        List<String> keys =
                Arrays.stream(labelValues).map(value -> value.split("=")[0]).toList();
        Set<String> keySet = new HashSet<>(keys);
        if (keySet.size() < keys.size()) {
            throw new IllegalArgumentException("Duplicate key found in "
                    + beanClass.getName()
                    + ". You cannot define the same key. You defined the following keys: "
                    + keys.stream().sorted().toList());
        }
    }
}
