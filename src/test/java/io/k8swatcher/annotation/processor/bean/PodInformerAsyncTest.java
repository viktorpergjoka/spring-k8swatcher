package io.k8swatcher.annotation.processor.bean;

import io.fabric8.kubernetes.api.model.Pod;
import io.k8swatcher.annotation.EventType;
import io.k8swatcher.annotation.Informer;
import io.k8swatcher.annotation.Watch;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;

@Informer(nsLabels = "kubernetes.io/metadata.name=foo", resLabels = "async=test")
public class PodInformerAsyncTest {

    @Autowired
    private AtomicInteger eventsCount;

    @Watch(event = EventType.ADD, resource = Pod.class)
    public void podAdded(Pod pod) throws InterruptedException {
        System.out.printf("Pod %s added\n", pod.getMetadata().getName());
        // Thread.sleep(1000);
        eventsCount.incrementAndGet();
    }

    @Watch(event = EventType.UPDATE, resource = Pod.class)
    public void podUpdated(Pod oldPod, Pod newPod) throws InterruptedException {
        if ((oldPod.getStatus().getPhase().equals("Running")
                        && newPod.getStatus().getPhase().equals("Running"))
                && (!oldPod.getMetadata()
                        .getResourceVersion()
                        .equals(newPod.getMetadata().getResourceVersion()))) {
            System.out.printf("Pod %s running\n", newPod.getMetadata().getName());
            Thread.sleep(1000);
            eventsCount.incrementAndGet();
        }
    }

    @Watch(event = EventType.DELETE, resource = Pod.class)
    public void podDeleted(Pod pod) throws InterruptedException {
        System.out.printf("Pod %s deleted\n", pod.getMetadata().getName());
        // Thread.sleep(3000);
        eventsCount.incrementAndGet();
    }
}
