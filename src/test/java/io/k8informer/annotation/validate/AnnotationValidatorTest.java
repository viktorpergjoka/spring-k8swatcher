package io.k8informer.annotation.validate;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.k8informer.annotation.Informer;
import io.k8informer.annotation.ValidateAnnotationTestConfig;
import io.k8informer.annotation.processor.KubeClientFactory;
import java.lang.reflect.MalformedParametersException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;

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
            validator.validateLabels(beanClass.getAnnotation(Informer.class), beanClass);
        });
    }

    @Test
    public void testInvalidResLabel() {
        Class<?> beanClass =
                informerBeanMap.get("invalidResLabelSyntaxTestBean").getClass();

        assertThrows(MalformedParametersException.class, () -> {
            validator.validateLabels(beanClass.getAnnotation(Informer.class), beanClass);
        });
    }

    @Test
    public void testInvalidResyncPeriod() {
        Class<?> beanClass = informerBeanMap.get("invalidResyncPeriodTestBean").getClass();

        assertThrows(IllegalArgumentException.class, () -> {
            validator.validateResyncPeriod(beanClass.getAnnotation(Informer.class), beanClass);
        });
    }
}
