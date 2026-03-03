package com.example.k8smcp;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
public class KubernetesMcpTools {

    private final ClusterEventStore eventStore;
    private final KubernetesClient kubernetesClient;

    public KubernetesMcpTools(ClusterEventStore eventStore, KubernetesClient kubernetesClient) {
        this.eventStore = eventStore;
        this.kubernetesClient = kubernetesClient;
    }

    @Tool(description = "Get recent Kubernetes cluster events captured by the informer. "
            + "Returns events from the last N minutes including pod creations, updates, and deletions.")
    public List<ClusterEvent> getRecentEvents(
            @ToolParam(description = "Number of minutes to look back for events") int minutes) {
        return eventStore.getRecent(minutes);
    }

    @Tool(description = "Get detailed information about a specific pod including its status, "
            + "container states, restart counts, and conditions.")
    public String getPodDetails(
            @ToolParam(description = "Name of the pod") String podName,
            @ToolParam(description = "Kubernetes namespace of the pod") String namespace) {
        Pod pod = kubernetesClient.pods().inNamespace(namespace).withName(podName).get();
        if (pod == null) {
            return "Pod " + podName + " not found in namespace " + namespace;
        }

        StringBuilder details = new StringBuilder();
        details.append("Pod: ").append(pod.getMetadata().getName()).append("\n");
        details.append("Namespace: ").append(pod.getMetadata().getNamespace()).append("\n");
        details.append("Phase: ").append(pod.getStatus().getPhase()).append("\n");
        details.append("Node: ").append(pod.getSpec().getNodeName()).append("\n");

        if (pod.getStatus().getContainerStatuses() != null) {
            details.append("\nContainers:\n");
            for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                details.append("  - ").append(cs.getName()).append("\n");
                details.append("    Ready: ").append(cs.getReady()).append("\n");
                details.append("    Restarts: ").append(cs.getRestartCount()).append("\n");
                if (cs.getState().getWaiting() != null) {
                    details.append("    Waiting: ").append(cs.getState().getWaiting().getReason()).append("\n");
                    if (cs.getState().getWaiting().getMessage() != null) {
                        details.append("    Message: ").append(cs.getState().getWaiting().getMessage()).append("\n");
                    }
                }
                if (cs.getState().getTerminated() != null) {
                    details.append("    Terminated: ").append(cs.getState().getTerminated().getReason()).append("\n");
                    details.append("    Exit Code: ").append(cs.getState().getTerminated().getExitCode()).append("\n");
                }
                if (cs.getLastState() != null && cs.getLastState().getTerminated() != null) {
                    details.append("    Last Termination: ")
                            .append(cs.getLastState().getTerminated().getReason()).append("\n");
                    details.append("    Last Exit Code: ")
                            .append(cs.getLastState().getTerminated().getExitCode()).append("\n");
                }
            }
        }

        if (pod.getStatus().getConditions() != null) {
            details.append("\nConditions:\n");
            pod.getStatus().getConditions().forEach(c ->
                    details.append("  - ").append(c.getType())
                            .append(": ").append(c.getStatus())
                            .append(" (").append(c.getReason() != null ? c.getReason() : "N/A").append(")\n"));
        }

        return details.toString();
    }

    @Tool(description = "Get the last 50 lines of logs from a specific pod. "
            + "Useful for debugging crashes, errors, or unexpected behavior.")
    public String getPodLogs(
            @ToolParam(description = "Name of the pod") String podName,
            @ToolParam(description = "Kubernetes namespace of the pod") String namespace) {
        try {
            return kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .tailingLines(50)
                    .getLog();
        } catch (Exception e) {
            return "Failed to get logs for pod " + podName + ": " + e.getMessage();
        }
    }

    @Tool(description = "Get a summary of all pods in watched namespaces grouped by their phase "
            + "(Running, Pending, Failed, Succeeded, Unknown). "
            + "Provides a quick overview of cluster health.")
    public String getClusterSummary() {
        List<Pod> pods = kubernetesClient.pods().inNamespace("default").list().getItems();

        Map<String, Long> podsByPhase = pods.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getStatus().getPhase() != null ? p.getStatus().getPhase() : "Unknown",
                        Collectors.counting()));

        StringBuilder summary = new StringBuilder();
        summary.append("Cluster Summary (namespace: default)\n");
        summary.append("Total Pods: ").append(pods.size()).append("\n\n");
        summary.append("Pods by Phase:\n");
        podsByPhase.forEach((phase, count) ->
                summary.append("  ").append(phase).append(": ").append(count).append("\n"));

        long unhealthyCount = pods.stream()
                .filter(p -> !"Running".equals(p.getStatus().getPhase())
                        && !"Succeeded".equals(p.getStatus().getPhase()))
                .count();

        if (unhealthyCount > 0) {
            summary.append("\nUnhealthy Pods:\n");
            pods.stream()
                    .filter(p -> !"Running".equals(p.getStatus().getPhase())
                            && !"Succeeded".equals(p.getStatus().getPhase()))
                    .forEach(p -> summary.append("  - ").append(p.getMetadata().getName())
                            .append(" (").append(p.getStatus().getPhase()).append(")\n"));
        }

        return summary.toString();
    }
}
