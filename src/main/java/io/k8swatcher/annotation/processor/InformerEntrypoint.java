/*
 * Copyright (C) 2025 Viktor Pergjoka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.k8swatcher.annotation.processor;

import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.k8swatcher.annotation.validate.AnnotationValidator;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

        boolean isRunning = informerList.stream().anyMatch(SharedIndexInformer::isRunning);
        while (isRunning) {
            try {
                isRunning = informerList.stream().anyMatch(SharedIndexInformer::isRunning);
                Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
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
