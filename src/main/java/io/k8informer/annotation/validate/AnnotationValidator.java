package io.k8informer.annotation.validate;


import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.k8informer.annotation.EventType;
import io.k8informer.annotation.Informer;
import io.k8informer.annotation.Watch;
import io.k8informer.processor.KubeClientFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.MalformedParametersException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class AnnotationValidator {

    private ApplicationContext ctx;
    private KubeClientFactory clients;
    private Map<String, Object> informerBeansMap;

    public AnnotationValidator(ApplicationContext ctx, KubeClientFactory clients) {
        this.ctx = ctx;
        this.clients = clients;
        informerBeansMap = new HashMap<>();
    }

    @PostConstruct
    public void init(){
        informerBeansMap = ctx.getBeansWithAnnotation(Informer.class);
    }

    public void validateInformerAnnotations(){
        for (Object bean : informerBeansMap.values()) {
            Class<?> beanClass = bean.getClass();
            Informer informer = beanClass.getAnnotation(Informer.class);
            KubernetesClient client = clients.getClient(informer.clientName());
            if (client == null){
                throw new RuntimeException("Could not find Kubernetes client for " + informer.clientName());
            }
        }
    }

    public void validateWatchAnnotations(){
        for (Object bean : informerBeansMap.values()) {
            Class<?> beanClass = bean.getClass();
            Method[] methods = beanClass.getMethods();
            List<Method> k8watchMethods = Arrays.stream(methods).filter(method -> method.isAnnotationPresent(Watch.class)).toList();
            if (k8watchMethods.isEmpty()) {
                log.warn("No @Watch annotated methods found for class {}", beanClass.getName());
            }
            for(Method method : k8watchMethods){
                Watch watch = method.getAnnotation(Watch.class);
                Class<? extends KubernetesResource> resource = watch.resource();
                EventType event = watch.event();

                switch (event) {
                    case ADD -> checkAddParams(method, resource);
                    case UPDATE -> checkUpdateParams(method, resource);
                    case DELETE -> checkDeleteParams(method, resource);
                }
            }
        }
    }

    private void checkAddParams(Method method, Class<? extends KubernetesResource> type){
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1) {
            throw new MalformedParametersException("Invalid number of parameters for method " + method.getName() + " . Expected 1, got " + parameterTypes.length);
        }
        Class<?> parameterType = parameterTypes[0];
        checkIsAssignableFrom(parameterType, type);
    }

    private void checkUpdateParams(Method method, Class<? extends KubernetesResource> type){
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 2) {
            throw new MalformedParametersException("Invalid number of parameters for method " + method.getName() + " . Expected 2, got " + parameterTypes.length);
        }
        Class<?> firstParam = parameterTypes[0];
        Class<?> secondParam = parameterTypes[1];
        checkIsAssignableFrom(firstParam, type);
        checkIsAssignableFrom(secondParam, type);
    }

    private void checkDeleteParams(Method method, Class<? extends KubernetesResource> type){
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length < 2 || parameterTypes.length > 3) {
            throw new MalformedParametersException("Invalid number of parameters for method " + method.getName());
        }
        Class<?> firstParam = parameterTypes[0];
        Class<?> secondParam = parameterTypes[1];
        checkIsAssignableFrom(firstParam, type);

        if (!secondParam.getTypeName().equals("boolean") && !secondParam.getTypeName().equals("java.lang.Boolean")){
            throw new MalformedParametersException(secondParam.getTypeName() + " is not boolean");
        }
    }

    private void checkIsAssignableFrom(Class<?> param, Class<? extends KubernetesResource> type){
        if (!param.isAssignableFrom(type)){
            throw new MalformedParametersException(param.getTypeName() + " is not assignable from " + type.getTypeName());
        }
    }

}
