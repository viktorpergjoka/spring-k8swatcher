package io.k8informer.annotation.cfg;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "k8watch.informer")
@Data
public class InformerConfigurationProperty {

    @Setter(AccessLevel.NONE)
    private Map<String, InformerConfiguration> config = new HashMap<>();

}
