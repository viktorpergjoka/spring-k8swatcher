package io.k8swatcher.annotation.processor;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.k8swatcher.annotation.Informer;
import io.k8swatcher.annotation.Watch;
import io.k8swatcher.annotation.cfg.InformerConfiguration;
import io.k8swatcher.annotation.cfg.InformerConfigurationProperty;
import io.k8swatcher.annotation.cfg.InformerContext;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class InformerCreator {

    private ApplicationContext ctx;
    private InformerConfigurationProperty cfg;
    private KubeClientFactory kubeClientFactory;

    List<SharedIndexInformer> createInformers() {
        List<InformerContext> informerContextList = getInformerContextList();
        List<SharedIndexInformer> informerList = new ArrayList<>();

        informerContextList.forEach(context -> {
            Class<?> beanClass = context.getBeanClass();
            Method[] methods = beanClass.getMethods();
            Map<? extends Class<? extends KubernetesResource>, List<Method>> watchMethods = Arrays.stream(methods)
                    .filter(method -> method.isAnnotationPresent(Watch.class))
                    .collect(Collectors.groupingBy(
                            method -> method.getAnnotation(Watch.class).resource()));

            InformerConfiguration informerConfiguration = context.getCfg();
            KubernetesClient client = context.getClient();
            Set<String> namespaces = getNamespaces(client, informerConfiguration);
            Map<String, String> resLabels = informerConfiguration.getResLabels();

            watchMethods.keySet().forEach(resource -> {
                List<SharedIndexInformer> informers = namespaces.stream()
                        .map(nsName -> (NonNamespaceOperation)
                                client.resources((Class) resource).inNamespace(nsName))
                        .map(nsOperation -> createSharedIndexInformer(
                                nsOperation, resLabels, informerConfiguration.getResyncPeriod()))
                        .map(sharedIndexInformer -> sharedIndexInformer.addEventHandlerWithResyncPeriod(
                                new IndexInformerResHandler(ctx, watchMethods.get(resource), beanClass),
                                informerConfiguration.getResyncPeriod()))
                        .toList();
                informerList.addAll(informers);
            });
            if (namespaces.isEmpty()) {
                log.error("No namespaces found with labels {}.", informerConfiguration.getNsLabels());
            } else {
                log.info("watching namespaces {}", namespaces);
            }
        });
        return informerList;
    }

    private Set<String> getNamespaces(KubernetesClient client, InformerConfiguration informerConfiguration) {
        Map<String, String> nsLabels = informerConfiguration.getNsLabels();
        Set<String> nsNames = informerConfiguration.getNsNames();
        if (!nsNames.isEmpty()) {
            return nsNames;
        }

        log.debug("nsLabel={}", nsLabels);
        Function<Namespace, String> namespaceToStringName =
                namespace -> namespace.getMetadata().getName();
        if (nsLabels.isEmpty() && nsNames.isEmpty()) {
            return client.namespaces().list().getItems().stream()
                    .map(namespaceToStringName)
                    .collect(Collectors.toSet());
        }
        return client.namespaces().withLabels(nsLabels).list().getItems().stream()
                .map(namespaceToStringName)
                .collect(Collectors.toSet());
    }

    private SharedIndexInformer createSharedIndexInformer(
            NonNamespaceOperation nsOperation, Map<String, String> resLabels, long resyncPeriod) {
        log.debug("resLabels={}", resLabels);
        if (resLabels.isEmpty()) {
            return nsOperation.runnableInformer(resyncPeriod);
        }

        return ((FilterWatchListDeletable) nsOperation.withLabels(resLabels)).runnableInformer(resyncPeriod);
    }

    private List<InformerContext> getInformerContextList() {
        Map<String, Object> informerBeansMap = ctx.getBeansWithAnnotation(Informer.class);
        return informerBeansMap.values().stream()
                .filter(bean -> Arrays.stream(AopUtils.getTargetClass(bean).getMethods())
                        .anyMatch(method -> method.isAnnotationPresent(Watch.class)))
                .map(bean -> {
                    Informer informer = AopUtils.getTargetClass(bean).getAnnotation(Informer.class);
                    Map<String, InformerConfiguration> configurationMap = cfg.getConfig();
                    InformerConfiguration informerConfiguration =
                            configurationMap.getOrDefault(informer.name(), configurationMap.get("default"));

                    return createInformerContext(bean, informer, informerConfiguration);
                })
                .toList();
    }

    private InformerContext createInformerContext(
            Object bean, Informer informer, InformerConfiguration informerConfiguration) {
        String clientName = informerConfiguration.getClientName();
        if (clientName.isEmpty()) {
            clientName = informer.clientName();
        }

        Map<String, String> nsLabels = informerConfiguration.getNsLabels();
        if (nsLabels.isEmpty()) {
            nsLabels = Arrays.stream(informer.nsLabels())
                    .collect(Collectors.toMap(item -> item.split("=")[0], item -> item.split("=")[1]));
        }
        Map<String, String> resLabels = informerConfiguration.getResLabels();
        if (resLabels.isEmpty()) {
            resLabels = Arrays.stream(informer.resLabels())
                    .collect(Collectors.toMap(item -> item.split("=")[0], item -> item.split("=")[1]));
        }
        Long resyncPeriod = informerConfiguration.getResyncPeriod();
        if (resyncPeriod == null) {
            resyncPeriod = informer.resyncPeriod();
        }
        if (resyncPeriod < 1000) {
            log.warn(
                    "Resync period {} for class {} is too small. Changing it to minimal allowed value of 1000",
                    resyncPeriod,
                    bean.getClass().getName());
            resyncPeriod = 1000L;
        }
        Set<String> nsNames = informerConfiguration.getNsNames();
        if (nsNames.isEmpty()) {
            nsNames = Set.of(informer.nsNames());
        }
        InformerConfiguration newCfg =
                new InformerConfiguration(nsLabels, resLabels, resyncPeriod, clientName, nsNames);

        return new InformerContext(bean.getClass(), informer, newCfg, kubeClientFactory.getClient(clientName));
    }
}
