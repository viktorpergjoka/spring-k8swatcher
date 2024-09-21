package io.k8informer.annotation.validate;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.k8informer.annotation.EventType;
import io.k8informer.annotation.Informer;
import io.k8informer.annotation.ValidateAnnotationTestConfig;
import io.k8informer.annotation.Watch;
import io.k8informer.annotation.processor.KubeClientFactory;
import java.lang.reflect.MalformedParametersException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(classes = {ValidateAnnotationTestConfig.class})
public class AnnotationValidatorTest {

    @Autowired
    private ApplicationContext ctx;

    @MockBean
    private KubeClientFactory clientFactory;

    private AnnotationValidator validator;

    private Map<String, Object> informerBeanMap;

    @BeforeEach
    public void setUp() {
        this.validator = new AnnotationValidator(ctx, clientFactory);
        this.informerBeanMap = ctx.getBeansWithAnnotation(Informer.class);
    }

    @Test
    public void testInvalidNsLabel() {
        Class<?> beanClass = informerBeanMap.get("invalidNsLabelSyntaxTestBean").getClass();

        assertThrows(MalformedParametersException.class, () -> {
            ReflectionTestUtils.invokeMethod(
                    validator, "validateLabels", beanClass.getAnnotation(Informer.class), beanClass);
        });
    }

    @Test
    public void testInvalidResLabel() {
        Class<?> beanClass =
                informerBeanMap.get("invalidResLabelSyntaxTestBean").getClass();

        assertThrows(MalformedParametersException.class, () -> {
            ReflectionTestUtils.invokeMethod(
                    validator, "validateLabels", beanClass.getAnnotation(Informer.class), beanClass);
        });
    }

    @Test
    public void testInvalidResyncPeriod() {
        Class<?> beanClass = informerBeanMap.get("invalidResyncPeriodTestBean").getClass();

        assertThrows(IllegalArgumentException.class, () -> {
            ReflectionTestUtils.invokeMethod(
                    validator, "validateResyncPeriod", beanClass.getAnnotation(Informer.class), beanClass);
        });
    }

    @Test
    public void testDuplicateLabel() {
        Class<?> beanClass = informerBeanMap.get("duplicateNsLabelTestBean").getClass();

        assertThrows(IllegalArgumentException.class, () -> {
            ReflectionTestUtils.invokeMethod(
                    validator,
                    "checkForDuplicateKey",
                    beanClass.getAnnotation(Informer.class).nsLabels(),
                    beanClass);
        });
    }

    @Test
    public void testAddParam() {
        Class<?> beanClass = informerBeanMap.get("invalidParamTestBean").getClass();
        Method[] methods = beanClass.getMethods();
        Map<EventType, Method> eventMap = Arrays.stream(methods)
                .filter(method -> method.isAnnotationPresent(Watch.class))
                .collect(Collectors.toMap(m -> m.getAnnotation(Watch.class).event(), Function.identity()));

        assertThrows(MalformedParametersException.class, () -> {
            ReflectionTestUtils.invokeMethod(
                    validator,
                    "checkAddParams",
                    beanClass,
                    eventMap.get(EventType.ADD),
                    eventMap.get(EventType.ADD).getAnnotation(Watch.class).resource());
        });
    }

    @Test
    public void checkUpdateParams() {
        Class<?> beanClass = informerBeanMap.get("invalidParamTestBean").getClass();
        Method[] methods = beanClass.getMethods();
        Map<EventType, Method> eventMap = Arrays.stream(methods)
                .filter(method -> method.isAnnotationPresent(Watch.class))
                .collect(Collectors.toMap(m -> m.getAnnotation(Watch.class).event(), Function.identity()));

        assertThrows(MalformedParametersException.class, () -> {
            ReflectionTestUtils.invokeMethod(
                    validator,
                    "checkAddParams",
                    beanClass,
                    eventMap.get(EventType.UPDATE),
                    eventMap.get(EventType.UPDATE).getAnnotation(Watch.class).resource());
        });
    }

    @Test
    public void checkDeleteParams() {
        Class<?> beanClass = informerBeanMap.get("invalidParamTestBean").getClass();
        Method[] methods = beanClass.getMethods();
        Map<EventType, Method> eventMap = Arrays.stream(methods)
                .filter(method -> method.isAnnotationPresent(Watch.class))
                .collect(Collectors.toMap(m -> m.getAnnotation(Watch.class).event(), Function.identity()));

        assertThrows(MalformedParametersException.class, () -> {
            ReflectionTestUtils.invokeMethod(
                    validator,
                    "checkDeleteParams",
                    beanClass,
                    eventMap.get(EventType.DELETE),
                    eventMap.get(EventType.DELETE).getAnnotation(Watch.class).resource());
        });
    }

    @Test
    public void checkIsAssignable() {
        Class<?> beanClass = informerBeanMap.get("isNotAssignableTestBean").getClass();
        Method[] methods = beanClass.getMethods();
        Map<EventType, Method> eventMap = Arrays.stream(methods)
                .filter(method -> method.isAnnotationPresent(Watch.class))
                .collect(Collectors.toMap(m -> m.getAnnotation(Watch.class).event(), Function.identity()));

        assertThrows(MalformedParametersException.class, () -> {
            ReflectionTestUtils.invokeMethod(
                    validator,
                    "checkAddParams",
                    beanClass,
                    eventMap.get(EventType.ADD),
                    eventMap.get(EventType.ADD).getAnnotation(Watch.class).resource());
        });
    }
}
