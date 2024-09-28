package io.k8informer.annotation.bean;

import io.fabric8.kubernetes.api.model.Pod;
import io.k8informer.annotation.EventType;
import io.k8informer.annotation.Informer;
import io.k8informer.annotation.Watch;

@Informer(
        nsLabels = {"kubernetes.io/metadata.name=foo"},
        resLabels = {"app=foo"})
public class InvalidUpdateParamTestBean {

    @Watch(event = EventType.UPDATE, resource = Pod.class)
    public void test2(Pod pod1, Pod pod2, Pod pod3) {}
}
