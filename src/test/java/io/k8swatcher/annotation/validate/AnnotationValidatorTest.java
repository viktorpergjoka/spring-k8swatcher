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
package io.k8swatcher.annotation.validate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.k8swatcher.annotation.EventType;
import io.k8swatcher.annotation.Informer;
import io.k8swatcher.annotation.ValidateAnnotationTestConfig;
import io.k8swatcher.annotation.Watch;
import io.k8swatcher.annotation.cfg.InformerConfiguration;
import io.k8swatcher.annotation.cfg.InformerConfigurationProperty;
import io.k8swatcher.annotation.processor.KubeClientFactory;
import java.lang.reflect.MalformedParametersException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {ValidateAnnotationTestConfig.class})
public class AnnotationValidatorTest {

    @MockitoBean
    private ApplicationContext ctx;

    @Autowired
    private ApplicationContext testCtx;

    private KubeClientFactory clientFactory;

    private AnnotationValidator validator;

    private InformerConfigurationProperty informerConfigurationProperty;

    private Map<String, Object> informerBeanMap;

    @BeforeEach
    public void setUp() {
        this.informerBeanMap = testCtx.getBeansWithAnnotation(Informer.class);
        this.clientFactory = new KubeClientFactory(ctx);
        this.informerConfigurationProperty = new InformerConfigurationProperty();
        this.informerConfigurationProperty.postConstruct();
        this.validator = new AnnotationValidator(ctx, clientFactory, informerConfigurationProperty);
        clientFactory.init();
        Mockito.clearInvocations(ctx);
    }

    @Test
    public void testInvalidNsLabel() {
        Mockito.when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Map.of(
                        "invalidNsLabelSyntaxTestBean", this.informerBeanMap.get("invalidNsLabelSyntaxTestBean")));
        validator.init();

        assertThrows(MalformedParametersException.class, () -> validator.validateInformerAnnotations());
    }

    @Test
    public void testInvalidResLabel() {
        Mockito.when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Map.of(
                        "invalidResLabelSyntaxTestBean", this.informerBeanMap.get("invalidResLabelSyntaxTestBean")));
        validator.init();

        assertThrows(MalformedParametersException.class, () -> validator.validateInformerAnnotations());
    }

    @Test
    public void testDuplicateLabel() {
        Mockito.when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Map.of("duplicateNsLabelTestBean", this.informerBeanMap.get("duplicateNsLabelTestBean")));
        validator.init();

        assertThrows(IllegalArgumentException.class, () -> validator.validateInformerAnnotations());
    }

    @Test
    public void testDuplicateNsNames() {
        Mockito.when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Map.of("duplicateNsNamesTestBean", this.informerBeanMap.get("duplicateNsNamesTestBean")));
        validator.init();

        assertThrows(IllegalArgumentException.class, () -> validator.validateInformerAnnotations());
    }

    @Test
    public void testAddParam() {
        Mockito.when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Map.of("invalidAddParamTestBean", this.informerBeanMap.get("invalidAddParamTestBean")));
        validator.init();

        assertThrows(MalformedParametersException.class, () -> validator.validateWatchAnnotations());
    }

    @Test
    public void checkUpdateParams() {
        Mockito.when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(
                        Map.of("invalidUpdateParamTestBean", this.informerBeanMap.get("invalidUpdateParamTestBean")));
        validator.init();

        assertThrows(MalformedParametersException.class, () -> validator.validateWatchAnnotations());
    }

    @Test
    public void checkDeleteParams() {
        Mockito.when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(
                        Map.of("invalidDeleteParamTestBean", this.informerBeanMap.get("invalidDeleteParamTestBean")));
        validator.init();

        assertThrows(MalformedParametersException.class, () -> validator.validateWatchAnnotations());
    }

    @Test
    public void checkIsAssignable() {
        Mockito.when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Map.of("isNotAssignableTestBean", this.informerBeanMap.get("isNotAssignableTestBean")));
        validator.init();

        assertThrows(MalformedParametersException.class, () -> validator.validateWatchAnnotations());
    }

    @Test
    public void testInvalidConfigName() {
        Mockito.when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Map.of("invalidConfigNameTestBean", this.informerBeanMap.get("invalidConfigNameTestBean")));
        validator.init();

        assertThrows(IllegalArgumentException.class, () -> validator.validateHasConfigName());
    }

    @Informer(
            nsLabels = {"env=prod", "tier=backend"},
            resLabels = {"app=myapp"})
    static class ValidInformerBean {
        @Watch(event = EventType.ADD, resource = Namespace.class)
        public void onAdd(Namespace ns) {}

        @Watch(event = EventType.UPDATE, resource = Namespace.class)
        public void onUpdate(Namespace oldNs, Namespace newNs) {}

        @Watch(event = EventType.DELETE, resource = Namespace.class)
        public void onDeleteSingle(Namespace ns) {}

        @Watch(event = EventType.DELETE, resource = Pod.class)
        public void onDeleteWithFlag(Pod pod, boolean flag) {}
    }

    @Test
    public void testValidInformerAnnotations() {
        Mockito.when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Map.of("validBean", new ValidInformerBean()));
        validator.init();

        assertDoesNotThrow(() -> validator.validateInformerAnnotations());
    }

    @Test
    public void testValidWatchAnnotations() {
        Mockito.when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Map.of("validBean", new ValidInformerBean()));
        validator.init();

        assertDoesNotThrow(() -> validator.validateWatchAnnotations());
    }

    @Test
    public void testValidConfigName_withDefault() {
        Mockito.when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Map.of("validBean", new ValidInformerBean()));
        validator.init();

        assertDoesNotThrow(() -> validator.validateHasConfigName());
    }

    @Informer(name = "custom")
    static class ValidCustomConfigBean {
        @Watch(event = EventType.ADD, resource = Namespace.class)
        public void onAdd(Namespace ns) {}
    }

    @Test
    public void testValidConfigName_withCustomConfig() {
        InformerConfiguration customConfig = new InformerConfiguration();
        informerConfigurationProperty.getConfig().put("custom", customConfig);

        Mockito.when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Map.of("customBean", new ValidCustomConfigBean()));
        validator.init();

        assertDoesNotThrow(() -> validator.validateHasConfigName());
    }

    @Informer
    static class ValidDeleteWithBooleanParam {
        @Watch(event = EventType.DELETE, resource = Namespace.class)
        public void onDelete(Namespace ns, Boolean deletedFinalStateUnknown) {}
    }

    @Test
    public void testValidDeleteParams_withBooleanObjectParam() {
        Mockito.when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Map.of("validDeleteBean", new ValidDeleteWithBooleanParam()));
        validator.init();

        assertDoesNotThrow(() -> validator.validateWatchAnnotations());
    }

    @Informer
    static class EmptyLabelsBean {
        @Watch(event = EventType.ADD, resource = Namespace.class)
        public void onAdd(Namespace ns) {}
    }

    @Test
    public void testValidInformerAnnotations_withEmptyLabels() {
        Mockito.when(ctx.getBeansWithAnnotation(Informer.class))
                .thenReturn(Map.of("emptyLabelsBean", new EmptyLabelsBean()));
        validator.init();

        assertDoesNotThrow(() -> validator.validateInformerAnnotations());
    }
}
