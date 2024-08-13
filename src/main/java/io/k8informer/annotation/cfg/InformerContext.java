package io.k8informer.annotation.cfg;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.k8informer.annotation.Informer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

@Data
@AllArgsConstructor
public class InformerContext {


    @Setter(AccessLevel.NONE)
    private Class<?> beanClass;

    @Setter(AccessLevel.NONE)
    private Informer informer;

    @Setter(AccessLevel.NONE)
    private InformerConfiguration cfg;

    @Setter(AccessLevel.NONE)
    private KubernetesClient client;
}
