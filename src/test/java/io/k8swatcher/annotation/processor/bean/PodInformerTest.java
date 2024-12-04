package io.k8swatcher.annotation.processor.bean;

import io.fabric8.kubernetes.api.model.Pod;
import io.k8swatcher.annotation.EventType;
import io.k8swatcher.annotation.Informer;
import io.k8swatcher.annotation.Watch;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

@Informer(nsLabels = "kubernetes.io/metadata.name=foo", resLabels = "app=foo")
public class PodInformerTest {

    @Autowired
    private Map<String, String> resMaps;

    @Watch(event = EventType.ADD, resource = Pod.class)
    public void podAdded(Pod pod) {
        resMaps.put("ADD", "added");
    }

    @Watch(event = EventType.UPDATE, resource = Pod.class)
    public void podUpdated(Pod oldPod, Pod newPod) {
        resMaps.put("UPDATE", "updated");
    }

    @Watch(event = EventType.DELETE, resource = Pod.class)
    public void podDeleted(Pod pod) {
        resMaps.put("DELETE", "deleted");
    }
}
