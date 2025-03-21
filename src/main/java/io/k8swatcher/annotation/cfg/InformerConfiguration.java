package io.k8swatcher.annotation.cfg;

import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InformerConfiguration {

    private Map<String, String> nsLabels = new HashMap<>();
    private Map<String, String> resLabels = new HashMap<>();
    private Long resyncPeriod;
    private String clientName = "";
    private Set<String> nsNames = new HashSet<>();
}
