package io.k8informer.annotation.processor;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.k8informer.annotation.Informer;
import io.k8informer.annotation.Watch;
import io.k8informer.annotation.cfg.InformerConfiguration;
import io.k8informer.annotation.cfg.InformerConfigurationProperty;
import io.k8informer.annotation.cfg.InformerContext;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
            List<Method> watchMethods = Arrays.stream(methods)
                    .filter(method -> method.isAnnotationPresent(Watch.class))
                    .toList();

            InformerConfiguration informerConfiguration = context.getCfg();
            KubernetesClient client = context.getClient();

            Map<String, String> nsLabels = informerConfiguration.getNsLabels();
            Map<String, String> resLabels = informerConfiguration.getResLabels();

            List<String> namespaces = getNamespaces(client, nsLabels);

            watchMethods.forEach(watchMethod -> {
                Class resource = watchMethod.getAnnotation(Watch.class).resource();

                List<SharedIndexInformer> informers = namespaces.stream()
                        .map(nsName -> (NonNamespaceOperation)
                                client.resources(resource).inNamespace(nsName))
                        .map(nsOperation -> createSharedIndexInformer(nsOperation, resLabels))
                        .map(sharedIndexInformer -> sharedIndexInformer.addEventHandlerWithResyncPeriod(
                                new IndexInformerResHandler(ctx, watchMethod, beanClass),
                                informerConfiguration.getResyncPeriod()))
                        .toList();
                informerList.addAll(informers);
            });
        });
        return informerList;
    }

    private List<String> getNamespaces(KubernetesClient client, Map<String, String> nsLabels) {
        log.debug("nsLabel={}", nsLabels);
        Function<Namespace, String> namespaceToStringName =
                namespace -> namespace.getMetadata().getName();
        if (nsLabels.isEmpty()) {
            return client.namespaces().list().getItems().stream()
                    .map(namespaceToStringName)
                    .toList();
        }
        return client.namespaces().withLabels(nsLabels).list().getItems().stream()
                .map(namespaceToStringName)
                .collect(Collectors.toList());
    }

    private SharedIndexInformer createSharedIndexInformer(
            NonNamespaceOperation nsOperation, Map<String, String> resLabels) {
        log.debug("resLabels={}", resLabels);
        if (resLabels.isEmpty()) {
            return nsOperation.inform();
        }

        return ((FilterWatchListDeletable) nsOperation.withLabels(resLabels)).inform();
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
        InformerConfiguration newCfg = new InformerConfiguration(nsLabels, resLabels, resyncPeriod, clientName);

        return new InformerContext(bean.getClass(), informer, newCfg, kubeClientFactory.getClient(clientName));
    }
}
