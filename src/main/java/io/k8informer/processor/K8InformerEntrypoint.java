package io.k8informer.processor;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.k8informer.annotation.EventType;
import io.k8informer.annotation.Informer;
import io.k8informer.annotation.Watch;
import io.k8informer.annotation.cfg.InformerConfiguration;
import io.k8informer.annotation.cfg.InformerConfigurationProperty;
import io.k8informer.annotation.cfg.InformerContext;
import io.k8informer.annotation.validate.AnnotationValidator;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class K8InformerEntrypoint {

    private ApplicationContext ctx;
    private InformerConfigurationProperty cfg;
    private AnnotationValidator validator;
    private KubeClientFactory kubeClientFactory;
    private List<SharedIndexInformer> informerList;

    public K8InformerEntrypoint(ApplicationContext ctx, InformerConfigurationProperty cfg, AnnotationValidator validator, KubeClientFactory kubeClientFactory) {
        this.ctx = ctx;
        this.cfg = cfg;
        this.validator = validator;
        this.kubeClientFactory = kubeClientFactory;
        this.informerList = new ArrayList<>();
    }


    @EventListener
    public void onStartUp(ApplicationReadyEvent event) {
        validateAnnotations();

        List<InformerContext> informerContextList = getInformerContextList();

        informerContextList.forEach(context -> {
            Class<?> beanClass = context.getBeanClass();
            Method[] methods = beanClass.getMethods();
            List<Method> k8watchMethods = Arrays.stream(methods).filter(method -> method.isAnnotationPresent(Watch.class)).toList();

            InformerConfiguration informerConfiguration = context.getCfg();
            KubernetesClient client = context.getClient();

            Map<String, List<String>> nsLabels = informerConfiguration.getNsLabels();
            Map<String, List<String>> resLabels = informerConfiguration.getResLabels();

            List<String> namespaces = getNamespaces(client, nsLabels);

            k8watchMethods.forEach(watchMethod -> {
                Class resource = watchMethod.getAnnotation(Watch.class).resource();
                informerList = namespaces.stream()
                        .map(nsName -> (NonNamespaceOperation) client.resources(resource).inNamespace(nsName))
                        .flatMap(nsOperation -> createSharedIndexInformer(nsOperation, resLabels).stream()).toList();

                informerList.stream()
                        .map(sharedIndexInformer -> {
                            return sharedIndexInformer.addEventHandler(getResourceEventHandler(watchMethod, beanClass));
                        }).forEach(SharedIndexInformer::start);
            });
        });
    }

    private ResourceEventHandler getResourceEventHandler(Method watchMethod, Class<?> beanClass) {
        return new ResourceEventHandler() {

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
        };
    }

    private void validateAnnotations() {
        validator.validateInformerAnnotations();
        validator.validateWatchAnnotations();
    }

    private List<String> getNamespaces(KubernetesClient client, Map<String, List<String>> nsLabels) {
        log.debug("nsLabel={}", nsLabels);
        Function<Namespace, String> namespaceToStringName = namespace -> namespace.getMetadata().getName();
        if (nsLabels.isEmpty()) {
            return client.namespaces().list().getItems().stream()
                    .map(namespaceToStringName)
                    .toList();
        }
        return nsLabels.keySet().stream()
                .flatMap(nsKey -> client.namespaces().withLabelIn(nsKey, nsLabels.get(nsKey).toArray(new String[0])).list().getItems().stream())
                .map(namespaceToStringName)
                .toList();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<SharedIndexInformer> createSharedIndexInformer(NonNamespaceOperation nsOperation, Map<String, List<String>> resLabels) {
        log.debug("resLabels={}", resLabels);
        if (resLabels.isEmpty()) {
            return Collections.singletonList(nsOperation.inform());
        }
        return resLabels.keySet().stream()
                .map(resKey -> ((FilterWatchListDeletable) nsOperation.withLabelIn(resKey, resLabels.get(resKey).toArray(new String[0]))).inform()).toList();
    }

    private List<InformerContext> getInformerContextList() {
        Map<String, Object> informerBeansMap = ctx.getBeansWithAnnotation(Informer.class);
        List<InformerContext> informerContextList = informerBeansMap.values().stream()
                .filter(bean -> Arrays.stream(AopUtils.getTargetClass(bean).getMethods()).anyMatch(method -> method.isAnnotationPresent(Watch.class)))
                .map(bean -> {
                    Informer informer = AopUtils.getTargetClass(bean).getAnnotation(Informer.class);
                    Map<String, InformerConfiguration> configurationMap = cfg.getConfig();
                    InformerConfiguration informerConfiguration = configurationMap.getOrDefault(informer.name(), configurationMap.get("default"));

                    KubernetesClient client = kubeClientFactory.getClient(informer.clientName());

                    if (informerConfiguration == null) {
                        Map<String, List<String>> nsLabels = Arrays.stream(informer.nsLabels())
                                .collect(Collectors.groupingBy(item -> item.split("=")[0], Collectors.mapping(item -> item.split("=")[1], Collectors.toList())));
                        Map<String, List<String>> resLabels = Arrays.stream(informer.resLabels()).collect(Collectors.groupingBy(item -> item.split("=")[0], Collectors.mapping(item -> item.split("=")[1], Collectors.toList())));
                        informerConfiguration = new InformerConfiguration(nsLabels, resLabels, informer.resyncPeriod());
                    }
                    return new InformerContext(bean.getClass(), informer, informerConfiguration, client);
                }).toList();
        return informerContextList;
    }

    @PreDestroy
    public void shutdown(){
        log.debug("Stopping informers");
        informerList.forEach(SharedIndexInformer::stop);
    }
}
