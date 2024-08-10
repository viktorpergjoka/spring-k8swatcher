package io.k8informer.processor;

import io.k8informer.annotation.Informer;
import io.k8informer.annotation.InformerConfigurationProperty;
import io.k8informer.annotation.Watch;
import io.k8informer.annotation.validate.AnnotationValidator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

@Component
@AllArgsConstructor
@Slf4j
public class K8InformerEntrypoint {

    private ApplicationContext ctx;
    private InformerConfigurationProperty cfg;
    private AnnotationValidator validator;

    @EventListener
    public void onStartUp(ApplicationReadyEvent event) {
        /*
        Map<String, Object> informerBeansMap = ctx.getBeansWithAnnotation(Informer.class);
        for (Map.Entry<String, Object> beanName : informerBeansMap.entrySet()) {
            Object bean = beanName.getValue();
            Class<?> beanClass = bean.getClass();
            Method[] methods = beanClass.getMethods();
            List<Method> k8watchMethods = Arrays.stream(methods).filter(method -> method.isAnnotationPresent(K8Watch.class)).toList();
            if (k8watchMethods.isEmpty()) {
                log.warn("No K8Watch annotated methods found for class {}", beanClass.getName());
            }
        }*/

        validator.validateInformerAnnotations();
        validator.validateWatchAnnotations();

        Map<String, Object> informerBeansMap = ctx.getBeansWithAnnotation(Informer.class);
        for (Map.Entry<String, Object> entry : informerBeansMap.entrySet()) {
            String beanName = entry.getKey();
            Class<?> beanClass = AopUtils.getTargetClass(ctx.getBean(beanName));
            for (Method method : beanClass.getMethods()) {
                if (method.isAnnotationPresent(Watch.class)) {
                    Watch annotation = method.getAnnotation(Watch.class);
                    Informer informer = beanClass.getAnnotation(Informer.class);


                }
            }


        }

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
}
