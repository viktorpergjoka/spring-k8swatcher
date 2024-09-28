package io.k8informer.annotation.processor;

import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.k8informer.annotation.validate.AnnotationValidator;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@SuppressWarnings("rawtypes")
public class K8InformerEntrypoint {

    private AnnotationValidator validator;
    private InformerCreator informerCreator;
    private List<SharedIndexInformer> informerList;

    public K8InformerEntrypoint(AnnotationValidator validator, InformerCreator informerCreator) {
        this.validator = validator;
        this.informerCreator = informerCreator;
        this.informerList = new ArrayList<>();
    }

    @EventListener
    public void onStartUp(ApplicationReadyEvent event) {
        validateAnnotations();
        this.informerList = informerCreator.createInformers();
    }

    private void validateAnnotations() {
        validator.validateInformerAnnotations();
        validator.validateWatchAnnotations();
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping informers");
        informerList.forEach(SharedIndexInformer::close);
    }
}
