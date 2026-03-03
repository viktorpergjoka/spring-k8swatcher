# Build a Real-Time Kubernetes MCP Server with Spring Boot

Most Kubernetes MCP servers wrap `kubectl` — they execute commands, parse the output, and return it to the AI. This works, but it's stateless. Every question requires a fresh API call. There is no memory of what happened between queries.

This article takes a different approach. We'll build a **stateful** MCP server that continuously watches a Kubernetes cluster using informers. The server maintains a live event stream in memory. When Claude asks "what happened in the last 5 minutes?", the answer is already there — no API call needed.

We'll combine two libraries:

- **[k8swatcher](https://github.com/viktorpergjoka/spring-k8informer)** — turns Kubernetes informers into annotated Spring beans. Instead of 50 lines of Fabric8 boilerplate, you write `@Watch(event = EventType.ADD, resource = Pod.class)` on a method.
- **[Spring AI MCP Server](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)** — exposes `@Tool`-annotated methods as MCP tools over SSE.

Annotations handle both sides: `@Informer` and `@Watch` bring Kubernetes events **in**, `@Tool` pushes tools **out** to any MCP client. By the end, you'll deploy a broken pod, watch the server capture crash events in real time, and have Claude diagnose the problem by calling your MCP tools.

## Project Setup

Let's start with the Gradle build. We need three dependencies: Spring Boot Web for the HTTP server, Spring AI's MCP server starter for tool exposure, and k8swatcher for Kubernetes event watching.

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.11'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.ai:spring-ai-bom:1.1.2"
    }
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.ai:spring-ai-starter-mcp-server-webmvc'
    implementation 'io.k8swatcher:spring-k8swatcher:1.0.8'
}
```

The Spring AI BOM at version 1.1.2 manages the MCP server starter version for us.

Next, the `application.yml` configures both the MCP server and k8swatcher:

```yaml
spring:
  ai:
    mcp:
      server:
        name: k8s-mcp-server
        version: 1.0.0
        type: SYNC
        sse-message-endpoint: /mcp/message

k8swatcher:
  config:
    default:
      nsNames:
        - default
```

The MCP server section tells Spring AI to expose tools over SSE with a synchronous request model. The k8swatcher section tells the informer to watch the `default` namespace. You can add more namespaces later.

The application entry point enables both systems with two annotations:

```java
@SpringBootApplication
@EnableInformer
public class K8sMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(K8sMcpServerApplication.class, args);
    }

    @Bean
    public KubernetesClient getKubernetesClient() {
        return new KubernetesClientBuilder().build();
    }
}
```

`@SpringBootApplication` bootstraps Spring Boot (and auto-configures the MCP server via the starter). `@EnableInformer` activates k8swatcher's informer infrastructure. The `KubernetesClient` bean is required explicitly — Fabric8's client isn't auto-configured by Spring Boot, so we create one that picks up the default kubeconfig from `~/.kube/config`.

## Watching Kubernetes Events with k8swatcher

Before we write the watcher, we need a way to store events. We'll define a simple record and an in-memory ring buffer.

```java
public record ClusterEvent(
    Instant timestamp,
    String eventType,
    String resourceKind,
    String resourceName,
    String namespace,
    String summary
) {}
```

The `ClusterEventStore` holds the last 1,000 events in a thread-safe deque:

```java
@Component
public class ClusterEventStore {

    private static final int MAX_EVENTS = 1000;
    private final ConcurrentLinkedDeque<ClusterEvent> events = new ConcurrentLinkedDeque<>();

    public void record(ClusterEvent event) {
        events.addFirst(event);
        while (events.size() > MAX_EVENTS) {
            events.removeLast();
        }
    }

    public List<ClusterEvent> getRecent(int minutes) {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(minutes));
        return events.stream()
                .filter(e -> e.timestamp().isAfter(cutoff))
                .toList();
    }

    public List<ClusterEvent> getByResource(String name, String namespace) {
        return events.stream()
                .filter(e -> e.resourceName().equals(name)
                          && e.namespace().equals(namespace))
                .toList();
    }
}
```

New events go to the front. When the buffer exceeds 1,000 entries, the oldest are dropped. Queries filter by time window or by resource name.

Now the watcher itself. This is where k8swatcher shines. Without it, setting up a Fabric8 informer requires creating a `SharedIndexInformer`, registering a `ResourceEventHandler` with `onAdd`, `onUpdate`, and `onDelete` callbacks, managing the informer lifecycle, and handling thread safety — typically 50+ lines of boilerplate per resource type.

With k8swatcher, the same thing is three annotated methods:

```java
@Informer
public class KubernetesEventWatcher {

    private final ClusterEventStore store;

    public KubernetesEventWatcher(ClusterEventStore store) {
        this.store = store;
    }

    @Watch(event = EventType.ADD, resource = Pod.class)
    public void podAdded(Pod pod) {
        store.record(new ClusterEvent(
                Instant.now(), "ADD", "Pod",
                pod.getMetadata().getName(),
                pod.getMetadata().getNamespace(),
                "Pod created, phase: " + pod.getStatus().getPhase()));
    }

    @Watch(event = EventType.UPDATE, resource = Pod.class)
    public void podUpdated(Pod oldPod, Pod newPod) {
        String oldPhase = oldPod.getStatus().getPhase();
        String newPhase = newPod.getStatus().getPhase();

        int restartCount = newPod.getStatus().getContainerStatuses()
                .stream()
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
                Instant.now(), "UPDATE", "Pod",
                newPod.getMetadata().getName(),
                newPod.getMetadata().getNamespace(),
                summary));
    }

    @Watch(event = EventType.DELETE, resource = Pod.class)
    public void podDeleted(Pod pod) {
        store.record(new ClusterEvent(
                Instant.now(), "DELETE", "Pod",
                pod.getMetadata().getName(),
                pod.getMetadata().getNamespace(),
                "Pod deleted"));
    }
}
```

`@Informer` marks the class as a Kubernetes resource watcher. It's also a `@Component`, so Spring handles its lifecycle and dependency injection. Each `@Watch` method declares what event type and resource kind it handles. For `UPDATE` events, k8swatcher passes both the old and new state of the resource — exactly what you need to detect phase changes or increasing restart counts.

The framework handles all the Fabric8 plumbing: creating the `SharedIndexInformer`, registering handlers, managing threads, and graceful shutdown. You focus on what to do with the events.

## Exposing Tools via MCP

Now we need to make this data accessible to AI assistants. Spring AI uses a single `@Tool` annotation for all tool exposure — there is no separate `@McpTool`. When the MCP server starter is on the classpath, Spring AI automatically discovers all `@Tool`-annotated methods in Spring beans and registers them as MCP tools. The same `@Tool` annotation works whether you're building an MCP server, a chat application, or any other Spring AI integration.

```java
@Service
public class KubernetesMcpTools {

    private final ClusterEventStore eventStore;
    private final KubernetesClient kubernetesClient;

    public KubernetesMcpTools(ClusterEventStore eventStore,
                              KubernetesClient kubernetesClient) {
        this.eventStore = eventStore;
        this.kubernetesClient = kubernetesClient;
    }

    @Tool(description = "Get recent Kubernetes cluster events captured by "
        + "the informer. Returns events from the last N minutes including "
        + "pod creations, updates, and deletions.")
    public List<ClusterEvent> getRecentEvents(
            @ToolParam(description = "Number of minutes to look back")
            int minutes) {
        return eventStore.getRecent(minutes);
    }

    @Tool(description = "Get detailed information about a specific pod "
        + "including its status, container states, restart counts, "
        + "and conditions.")
    public String getPodDetails(
            @ToolParam(description = "Name of the pod") String podName,
            @ToolParam(description = "Kubernetes namespace") String namespace) {
        Pod pod = kubernetesClient.pods()
                .inNamespace(namespace).withName(podName).get();
        if (pod == null) {
            return "Pod not found: " + podName + " in " + namespace;
        }
        // Format pod details: phase, node, containers, conditions...
        return formatPodDetails(pod);
    }

    @Tool(description = "Get the last 50 lines of logs from a specific pod. "
        + "Useful for debugging crashes, errors, or unexpected behavior.")
    public String getPodLogs(
            @ToolParam(description = "Name of the pod") String podName,
            @ToolParam(description = "Kubernetes namespace") String namespace) {
        return kubernetesClient.pods()
                .inNamespace(namespace).withName(podName)
                .tailingLines(50).getLog();
    }

    @Tool(description = "Get a summary of all pods in watched namespaces "
        + "grouped by phase (Running, Pending, Failed, Succeeded). "
        + "Provides a quick overview of cluster health.")
    public String getClusterSummary() {
        // Count pods by phase, list unhealthy pods...
        return formatClusterSummary();
    }
}
```

Four tools, each with a clear purpose:

1. **`getRecentEvents`** — reads from the in-memory event store. This is the stateful differentiator. The events are already captured; no Kubernetes API call happens when this tool is invoked.
2. **`getPodDetails`** — makes a live API call to get current pod state. Complements the event history with a real-time snapshot.
3. **`getPodLogs`** — fetches the last 50 log lines. Essential for diagnosing crashes.
4. **`getClusterSummary`** — aggregates pod counts by phase. Gives the AI a quick health overview to decide where to dig deeper.

Notice the symmetry with the watcher annotations. On the input side, `@Watch` describes what Kubernetes events to capture. On the output side, `@Tool` describes what capabilities to expose. The `description` strings on `@Tool` and `@ToolParam` aren't just documentation — they're how the AI decides which tool to call. A description like "Get recent Kubernetes cluster events captured by the informer" tells Claude exactly when this tool is relevant. Vague descriptions lead to poor tool selection; specific descriptions lead to accurate, autonomous reasoning.

The `KubernetesClient` bean we defined in the application class is shared — the same client powers both the informers and these MCP tools.

## Connecting Claude Code

The MCP server is already running once you start the Spring Boot app. Claude Code can connect to it with a single command.

Claude Code has built-in support for MCP servers. To register your running server, use the `claude mcp add` command with the SSE transport:

```bash
claude mcp add --transport sse k8s-mcp-server http://localhost:8080/sse
```

That's it. This registers the server at the **local** scope (available to you in the current project). If you want the server available across all your projects, add the `--scope user` flag:

```bash
claude mcp add --transport sse --scope user k8s-mcp-server http://localhost:8080/sse
```

To verify the server is connected, use the `/mcp` command inside Claude Code:

```
> /mcp
```

This shows all connected MCP servers and their status. You should see `k8s-mcp-server` listed with four tools: `getRecentEvents`, `getPodDetails`, `getPodLogs`, and `getClusterSummary`.

You can also manage your servers with these commands:

```bash
# List all configured servers
claude mcp list

# Get details for a specific server
claude mcp get k8s-mcp-server

# Remove the server
claude mcp remove k8s-mcp-server
```

No custom chat UI. No WebSocket server. No frontend code. Spring AI's MCP starter handles the SSE transport, tool discovery, and JSON-RPC protocol. Claude Code speaks MCP natively. The two just connect.

Once registered, you can use the MCP tools directly in your Claude Code conversations — the same terminal where you write code, run tests, and manage your project. Ask Claude to diagnose a pod issue while you're working on the fix, all without switching windows.

## Demo Walkthrough

Let's see it in action. Start the app and deploy a pod that's designed to fail:

```bash
./gradlew bootRun
```

In another terminal:

```bash
kubectl apply -f src/main/resources/k8s/broken-pod.yml
```

This creates a pod called `broken-demo` running nginx with only 5Mi of memory. Nginx needs more than that, so the container will be OOM-killed and restart in a loop.

Within seconds, k8swatcher captures the events. You'll see log output as the informer fires:

```
Pod created, phase: Pending
Phase changed: Pending -> Running
Pod updated, restart count: 1
Pod updated, restart count: 2
Pod updated, restart count: 3
```

Now in Claude Code, ask:

> "What's happening in my cluster?"

Claude calls `getClusterSummary` and sees a pod that isn't in the Running phase. It follows up by calling `getRecentEvents` to see the event history. It notices the escalating restart count and calls `getPodDetails` to check the container state. Finally, it calls `getPodLogs` to see if there's any output.

The conversation might look like this:

> **Claude:** I can see there's an issue with the `broken-demo` pod in the default namespace. Let me investigate.
>
> *[Calls getClusterSummary]*
>
> There's 1 pod in a CrashLoopBackOff state. Let me get more details.
>
> *[Calls getPodDetails("broken-demo", "default")]*
>
> The pod's container has been OOM-killed (exit code 137) and has restarted 4 times. The memory limit is set to only 5Mi, which is too low for nginx.
>
> **Recommendation:** Increase the memory limit. 64Mi should be sufficient for a basic nginx container:
> ```yaml
> resources:
>   limits:
>     memory: "64Mi"
> ```

Claude didn't just tell you the pod is broken — it identified the root cause (OOM), explained why (5Mi is too low for nginx), and suggested a fix with a specific value. All from the data your MCP server provided.

Clean up when you're done:

```bash
kubectl delete pod broken-demo
```

## Going Further

This example watches Pods, but k8swatcher supports any Kubernetes resource that implements Fabric8's `HasMetadata` interface. Adding Deployment watching takes one more method in the same `@Informer` class:

```java
@Watch(event = EventType.UPDATE, resource = Deployment.class)
public void deploymentUpdated(Deployment old, Deployment current) {
    int oldReady = old.getStatus().getReadyReplicas() != null
            ? old.getStatus().getReadyReplicas() : 0;
    int newReady = current.getStatus().getReadyReplicas() != null
            ? current.getStatus().getReadyReplicas() : 0;
    int desired = current.getSpec().getReplicas();

    if (oldReady != newReady) {
        store.record(new ClusterEvent(
                Instant.now(), "UPDATE", "Deployment",
                current.getMetadata().getName(),
                current.getMetadata().getNamespace(),
                "Replicas ready: " + newReady + "/" + desired));
    }
}
```

Because the event store and MCP tools are decoupled from the watcher, this new event data automatically flows through `getRecentEvents` without any changes to the MCP layer. Add a `getDeploymentDetails` tool if you want Claude to query Deployment state directly.

For production use, consider **write tools with guardrails** — you could expose a `scaleDeployment` tool, but validate inputs and restrict it to non-production namespaces. k8swatcher also handles Custom Resources the same way as built-in types, so you can build CRD controllers and expose their state via MCP.

## Limitations

This example is intentionally simple. A few things to be aware of before running it beyond a demo:

- **In-memory event store** — the `ClusterEventStore` is a ring buffer in the JVM heap. Events are lost on restart. For durability, swap the `ConcurrentLinkedDeque` for a database or a persistent queue.
- **No authentication on the MCP endpoint** — the SSE endpoint is open on `localhost:8080`. In a shared or remote environment, put it behind a reverse proxy with authentication, or use Spring Security to protect the endpoint.
- **Single-namespace default** — the example only watches the `default` namespace. Add more namespaces in `application.yml`, but be aware that watching many namespaces with high pod churn increases memory pressure and informer traffic.
- **No filtering on UPDATE events** — the watcher records every pod update, including metadata-only changes (label additions, annotation updates). In a busy cluster, you'll want to filter updates to only record meaningful state changes.

## Conclusion

The architecture of this project comes down to two annotation pairs:

- `@Informer` + `@Watch` bring Kubernetes events **into** your application
- `@Tool` pushes capabilities **out** to AI assistants via MCP

Spring Boot wires everything together. k8swatcher eliminates the informer boilerplate. Spring AI eliminates the MCP protocol boilerplate. What's left is your domain logic — the event store, the formatting, the tool descriptions.

This pattern isn't limited to Kubernetes. Any domain where you have a stream of events and want to give an AI assistant structured access to that data can follow the same approach: capture events with domain-specific tooling, store them in memory, expose them as MCP tools with clear descriptions. The annotations are different, but the architecture is the same.

The complete working example is available in the [companion repository](https://github.com/viktorpergjoka/spring-k8informer/tree/main/article/k8s-mcp-server). Clone it, point it at a cluster, and start asking Claude about your pods.
