package io.k8swatcher.annotation.bean;

import io.fabric8.kubernetes.api.model.Pod;
import io.k8swatcher.annotation.EventType;
import io.k8swatcher.annotation.Informer;
import io.k8swatcher.annotation.Watch;

@Informer(
        nsLabels = {"kubernetes.io/metadata.name=foo"},
        resLabels = {"app=foo"})
public class IsNotAssignableTestBean {

    @Watch(event = EventType.ADD, resource = Pod.class)
    public void test(String test) {}
}
