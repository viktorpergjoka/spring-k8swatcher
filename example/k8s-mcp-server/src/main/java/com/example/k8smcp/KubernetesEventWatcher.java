package com.example.k8smcp;

import java.time.Instant;
import java.util.Objects;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.k8swatcher.annotation.EventType;
import io.k8swatcher.annotation.Informer;
import io.k8swatcher.annotation.Watch;

@Informer
public class KubernetesEventWatcher {

    private final ClusterEventStore store;

    public KubernetesEventWatcher(ClusterEventStore store) {
        this.store = store;
    }

    @Watch(event = EventType.ADD, resource = Pod.class)
    public void podAdded(Pod pod) {
        store.record(new ClusterEvent(
                Instant.now(),
                "ADD",
                "Pod",
                pod.getMetadata().getName(),
                pod.getMetadata().getNamespace(),
                "Pod created, phase: " + pod.getStatus().getPhase()));
    }

    @Watch(event = EventType.UPDATE, resource = Pod.class)
    public void podUpdated(Pod oldPod, Pod newPod) {
        String oldPhase = oldPod.getStatus().getPhase();
        String newPhase = newPod.getStatus().getPhase();

        int restartCount = newPod.getStatus().getContainerStatuses().stream()
                .mapToInt(ContainerStatus::getRestartCount)
                .sum();

        String summary;
        if (!Objects.equals(oldPhase, newPhase)) {
            summary = "Phase changed: " + oldPhase + " -> " + newPhase;
        } else if (restartCount > 0) {
            summary = "Pod updated, restart count: " + restartCount;
        } else {
            summary = "Pod updated, phase: " + newPhase;
        }

        store.record(new ClusterEvent(
                Instant.now(),
                "UPDATE",
                "Pod",
                newPod.getMetadata().getName(),
                newPod.getMetadata().getNamespace(),
                summary));
    }

    @Watch(event = EventType.DELETE, resource = Pod.class)
    public void podDeleted(Pod pod) {
        store.record(new ClusterEvent(
                Instant.now(),
                "DELETE",
                "Pod",
                pod.getMetadata().getName(),
                pod.getMetadata().getNamespace(),
                "Pod deleted"));
    }
}
