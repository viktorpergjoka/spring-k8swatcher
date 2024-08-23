package io.k8informer.annotation.cfg;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InformerConfiguration {

    private Map<String, List<String>> nsLabels = new HashMap<>();
    private Map<String, List<String>> resLabels = new HashMap<>();
    private long resyncPeriod = 1000L;
}
