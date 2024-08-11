package io.k8informer.processor;

import io.fabric8.kubernetes.client.KubernetesClient;
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

import java.util.*;
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
        List<InformerContext> informers = informerBeansMap.values().stream()
                .filter(bean -> Arrays.stream(AopUtils.getTargetClass(bean).getMethods()).anyMatch(method -> method.isAnnotationPresent(Watch.class)))
                .map(bean -> {
                    Informer informer = AopUtils.getTargetClass(bean).getAnnotation(Informer.class);
                    Map<String, InformerConfiguration> configurationMap = cfg.getConfig();
                    InformerConfiguration informerConfiguration = configurationMap.getOrDefault(informer.name(), configurationMap.get("default"));

                    KubernetesClient client = kubeClientFactory.getClient(informer.clientName());

                    if (informerConfiguration == null) {
                        validator.validateLabels(informer, bean.getClass());
                        Map<String, String> nsLabels = Arrays.stream(informer.nsLabels()).collect(Collectors.toMap(item -> item.split("=")[0], item -> item.split("=")[1]));
                        Map<String, String> resLabels = Arrays.stream(informer.resLabels()).collect(Collectors.toMap(item -> item.split("=")[0], item -> item.split("=")[1]));
                        informerConfiguration = new InformerConfiguration(nsLabels, resLabels, informer.resyncPeriod());
                    }
                    return new InformerContext(informer, informerConfiguration, client);
                }).toList();


        /*
        for(String beanName: ctx.getBeanDefinitionNames()){
            Class<?> clazz = AopUtils.getTargetClass(ctx.getBean(beanName));
            Method[] methods = clazz.getDeclaredMethods();

            for(Method m: methods){
                if(m.isAnnotationPresent(K8Watch.class)){
                    K8Watch annotation = m.getAnnotation(K8Watch.class);
                    Class resource = annotation.resource();
                    String[] namespaces = annotation.namespaces();

                    System.out.println(cfg);


                    KubernetesClient client = new KubernetesClientBuilder().build();

                    NonNamespaceOperation r = (NonNamespaceOperation) client.resources(resource).inNamespace(namespaces[0]);

                    SharedIndexInformer f = ((FilterWatchListDeletable)r.withLabel("app", "http-echo")).inform();
                    //FilterWatchListDeletable<Pod, KubernetesResourceList<Pod>, Resource<Pod>> q = client.resources(Pod.class).inNamespace("").withLabel("", "");



                    SharedIndexInformer inform = f.addEventHandler(new ResourceEventHandler() {
                        @Override
                        public void onAdd(Object obj) {
                            try {
                                Object instance = clazz.getDeclaredConstructor().newInstance();
                                ReflectionUtils.invokeMethod(m, instance, obj);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public void onUpdate(Object oldObj, Object newObj) {

                        }

                        @Override
                        public void onDelete(Object obj, boolean deletedFinalStateUnknown) {

                        }
                    });
                    inform.start();
                }
            }
        }
            */
    }

    private void validateAnnotations(){
        validator.validateInformerAnnotations();
        validator.validateWatchAnnotations();
    }
}
