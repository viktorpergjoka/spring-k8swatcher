package io.k8swatcher.podinformer;

import io.fabric8.kubernetes.api.model.Pod;
import io.k8swatcher.annotation.EventType;
import io.k8swatcher.annotation.Informer;
import io.k8swatcher.annotation.Watch;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Informer(resLabels = {"k8swatcher.io/watched=true"})
public class PodInformer {

    private static final String webHook = "https://webhook.site/a6318eb5-d409-4c87-8ee3-90c2a3c8e96a";


    @Watch(event = EventType.ADD, resource = Pod.class)
    public void added(Pod pod){
        postRequest("pod " + pod.getMetadata().getName()+ " is added");
    }

    @Watch(event = EventType.UPDATE, resource = Pod.class)
    public void updated(Pod oldPod, Pod newPod){
        if (newPod.getStatus().getPhase().equals("Running")
                && (!oldPod.getMetadata()
                .getResourceVersion()
                .equals(newPod.getMetadata().getResourceVersion()))) {
            postRequest("pod " + newPod.getMetadata().getName()+ " is running");
        }
    }

    @Watch(event = EventType.DELETE, resource = Pod.class)
    public void deleted(Pod pod){
        if (pod.getStatus().getPhase().equals("Succeeded")) {
            postRequest("pod " + pod.getMetadata().getName()+ " is completed");
        }
    }

    private void postRequest(String message){
        try (HttpClient client = HttpClient.newHttpClient()){
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(webHook)).POST(HttpRequest.BodyPublishers.ofString(message)).build();
            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
