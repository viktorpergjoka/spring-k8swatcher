package io.k8swatcher.annotation.processor;

import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.k8swatcher.annotation.validate.AnnotationValidator;
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
public class InformerEntrypoint {

    private final AnnotationValidator validator;
    private final InformerCreator informerCreator;
    private List<SharedIndexInformer> informerList;

    public InformerEntrypoint(AnnotationValidator validator, InformerCreator informerCreator) {
        this.validator = validator;
        this.informerCreator = informerCreator;
        this.informerList = new ArrayList<>();
    }

    @EventListener
    public void onStartUp(ApplicationReadyEvent event) {
        validateAnnotations();
        this.informerList = informerCreator.createInformers();
        informerList.forEach(SharedIndexInformer::start);
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
