package io.k8swatcher.annotation.bean;

import io.fabric8.kubernetes.api.model.Pod;
import io.k8swatcher.annotation.EventType;
import io.k8swatcher.annotation.Informer;
import io.k8swatcher.annotation.Watch;

@Informer(
        nsLabels = {"kubernetes.io/metadata.name=foo"},
        resLabels = {"app=foo"})
public class InvalidDeleteParamTestBean {

    @Watch(event = EventType.DELETE, resource = Pod.class)
    public void test3(Pod pod, Pod pod2, boolean deletedFinalStateUnknown) {}
}
