package io.k8informer.annotation.cfg;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.k8informer.annotation.Informer;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InformerContext {


    private Informer informer;
    private InformerConfiguration cfg;
    private KubernetesClient client;
}
