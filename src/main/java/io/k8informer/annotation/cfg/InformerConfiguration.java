package io.k8informer.annotation.cfg;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InformerConfiguration {

    private Map<String, String> nsLabels = new HashMap<>();
    private Map<String, String> resLabels = new HashMap<>();
    private long resyncPeriod = 1000L;
    private String clientName = "";
}
