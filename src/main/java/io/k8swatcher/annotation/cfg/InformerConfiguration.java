package io.k8swatcher.annotation.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private Long resyncPeriod;
    private String clientName = "";
    private List<String> nsNames = new ArrayList<>();
}
