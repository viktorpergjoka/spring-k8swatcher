package io.k8informer.processor;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.NamespaceListVisitFromServerGetDeleteRecreateWaitApplicable;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.k8informer.annotation.EventType;
import io.k8informer.annotation.Informer;
import io.k8informer.annotation.cfg.InformerConfiguration;
import io.k8informer.annotation.cfg.InformerConfigurationProperty;
import io.k8informer.annotation.Watch;
import io.k8informer.annotation.cfg.InformerContext;
import io.k8informer.annotation.validate.AnnotationValidator;
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
@AllArgsConstructor
@Slf4j
public class K8InformerEntrypoint {

    private ApplicationContext ctx;
    private InformerConfigurationProperty cfg;
    private AnnotationValidator validator;
    private KubeClientFactory kubeClientFactory;

    @EventListener
    public void onStartUp(ApplicationReadyEvent event) {
        validateAnnotations();

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
                        Map<String, String> resLabels = Arrays.stream(informer.resLabels()).collect(Collectors.toMap(item -> item.split("=")[0], item -> item.split("=")[1], (a, b) -> a.split("=")[0]));
                        informerConfiguration = new InformerConfiguration(nsLabels, resLabels, informer.resyncPeriod());
                    }
                    return new InformerContext(bean.getClass(), informer, informerConfiguration, client);
                }).toList();

        informerContextList.forEach(context -> {
            Class<?> beanClass = context.getBeanClass();
            Method[] methods = beanClass.getMethods();
            List<Method> k8watchMethods = Arrays.stream(methods).filter(method -> method.isAnnotationPresent(Watch.class)).toList();

            Informer informer = context.getInformer();
            InformerConfiguration informerConfiguration = context.getCfg();
            KubernetesClient client = context.getClient();

            Map<String, List<String>> nsLabels = informerConfiguration.getNsLabels();
            Map<String, String> resLabels = informerConfiguration.getResLabels();

            if (!nsLabels.isEmpty()){
                k8watchMethods.forEach(watchMethod -> {
                    Class resource = watchMethod.getAnnotation(Watch.class).resource();

                    nsLabels.keySet().forEach(key -> {
                        List<String> nsListValue = nsLabels.get(key);
                        nsListValue.forEach(nsvalue -> {
                            Map<String, String> label = Map.of(key, nsvalue);
                            List<Namespace> namespaces = client.namespaces().withLabels(label).list().getItems();
                            namespaces.stream()
                                    .map(ns -> ns.getMetadata().getName())
                                    .forEach(nsName -> {
                                        NonNamespaceOperation r = (NonNamespaceOperation) client.resources(resource).inNamespace(nsName);
                                        SharedIndexInformer sharedIndexInformer = ((FilterWatchListDeletable) r.withLabels(resLabels)).inform();
                                        sharedIndexInformer.addEventHandler(new ResourceEventHandler() {

                                            @Override
                                            public void onAdd(Object obj) {
                                                if (watchMethod.getAnnotation(Watch.class).event().equals(EventType.ADD)) {
                                                    try {
                                                        Object instance = beanClass.getDeclaredConstructor().newInstance();
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
                                                        Object instance = beanClass.getDeclaredConstructor().newInstance();
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
                                                        Object instance = beanClass.getDeclaredConstructor().newInstance();
                                                        ReflectionUtils.invokeMethod(watchMethod, instance, obj, deletedFinalStateUnknown);
                                                    } catch (Exception e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                }
                                            }
                                        });
                                        sharedIndexInformer.start();
                                    });
                        });
                    });

                });
            }
        });
    }

    private void validateAnnotations(){
        validator.validateInformerAnnotations();
        validator.validateWatchAnnotations();
    }
}
