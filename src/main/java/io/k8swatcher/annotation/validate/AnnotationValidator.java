package io.k8swatcher.annotation.validate;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.k8swatcher.annotation.EventType;
import io.k8swatcher.annotation.Informer;
import io.k8swatcher.annotation.Watch;
import io.k8swatcher.annotation.processor.KubeClientFactory;
import jakarta.annotation.PostConstruct;
import java.lang.reflect.MalformedParametersException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AnnotationValidator {

    private final ApplicationContext ctx;
    private final KubeClientFactory clients;
    private Map<String, Object> informerBeansMap;

    public AnnotationValidator(ApplicationContext ctx, KubeClientFactory clients) {
        this.ctx = ctx;
        this.clients = clients;
        informerBeansMap = new HashMap<>();
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
            validateResyncPeriod(informer, beanClass);
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

        BiConsumer<String[], String> validateLabel = (String[] labels, String name) -> {
            if (labels == null || labels.length == 0) {
                log.warn(
                        "{} are not provided for class {}. If not specified all will be" + " used.",
                        name,
                        beanClass.getName());
            } else {
                Arrays.asList(labels).forEach(label -> {
                    String[] splittedLabel = label.split("=");
                    if (splittedLabel.length != 2) {
                        throw new MalformedParametersException("Invalid label for "
                                + label
                                + "in class "
                                + beanClass.getName()
                                + ". Format has to be key=value");
                    }
                });
            }
        };
        validateLabel.accept(nsLabels, "nsLabels (namespace labels)");
        validateLabel.accept(resLabels, "resLabels (resource labels)");

        checkForDuplicateKey(nsLabels, beanClass);
        checkForDuplicateKey(resLabels, beanClass);
    }

    private void checkForDuplicateKey(String[] labelValues, Class<?> beanClass) {
        List<String> keys = Arrays.asList(labelValues).stream()
                .map(value -> value.split("=")[0])
                .toList();
        Set<String> keySet = new HashSet<>(keys);
        if (keySet.size() < keys.size()) {
            throw new IllegalArgumentException("Duplicate key found in "
                    + beanClass.getName()
                    + ". You cannot define the same key. You defined the following keys: "
                    + keys.stream().sorted());
        }
    }

    private void validateResyncPeriod(Informer informer, Class<?> beanClass) {
        if (informer.resyncPeriod() < 1000) {
            throw new IllegalArgumentException(
                    "Resync period for class " + beanClass.getName() + " must be greater than 1000.");
        }
    }
}
