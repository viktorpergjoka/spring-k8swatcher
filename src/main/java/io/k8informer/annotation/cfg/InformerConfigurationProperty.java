package io.k8informer.annotation.cfg;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "k8watch.informer")
@Data
public class InformerConfigurationProperty {

    @Setter(AccessLevel.NONE)
    private Map<String, InformerConfiguration> config = new HashMap<>();

    @PostConstruct
    public void postConstruct() {
        InformerConfiguration informerConfiguration = config.get("default");
        if (informerConfiguration == null) {
            config.put("default", new InformerConfiguration());
        }
    }
}
