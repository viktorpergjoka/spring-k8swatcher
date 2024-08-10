package io.k8informer.annotation;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "k8watch.informer")
@Data
public class InformerConfigurationProperty {

    private Map<String, InformerConfiguration> config = new HashMap<>();

    @Data
    static private class InformerConfiguration {

        private Map<String, String> nsLabels = new HashMap<>();
        private Map<String, String> resLabels = new HashMap<>();
        private long resyncPeriod = 1000L;
    }
}
