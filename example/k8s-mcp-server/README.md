# K8s MCP Server

A Spring Boot application that combines [k8swatcher](https://github.com/viktorpergjoka/spring-k8informer) with [Spring AI MCP Server](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html) to give Claude Code real-time visibility into your Kubernetes cluster.

This is the companion example project for the article: **"Build a Real-Time Kubernetes MCP Server with Spring Boot"**.

## Prerequisites

- **Java 21** (or later)
- **A Kubernetes cluster** (minikube, kind, Docker Desktop, or remote)
- **kubectl** configured to talk to your cluster
- **Claude Code** (to connect as an MCP client)

## Quick Start

### 1. Start the application

```bash
./gradlew bootRun
```

The app will:
- Start a Spring Boot web server on port `8080`
- Begin watching Pod events in the `default` namespace via k8swatcher
- Expose MCP tools over SSE at `http://localhost:8080/sse`

### 2. Connect Claude Code

Register the running MCP server with a single command:

```bash
claude mcp add --transport sse k8s-mcp-server http://localhost:8080/sse
```

This adds the server at the **local** scope (current project only). To make it available across all projects, use `--scope user`:

```bash
claude mcp add --transport sse --scope user k8s-mcp-server http://localhost:8080/sse
```

Verify the connection inside Claude Code:

```
> /mcp
```

You should see `k8s-mcp-server` listed with four tools.

### 3. Deploy the broken demo pod

```bash
kubectl apply -f src/main/resources/k8s/broken-pod.yml
```

This creates a pod (`broken-demo`) with only 5Mi of memory — nginx will OOM-crash and restart repeatedly.

### 4. Ask Claude

In Claude Code, try:

- "What's happening in my cluster?"
- "Show me recent events from the last 5 minutes"
- "Get the details for the broken-demo pod"
- "Show me the logs for broken-demo"
- "Give me a cluster summary"

## Available MCP Tools

| Tool | Description |
|---|---|
| `getRecentEvents` | Get cluster events from the last N minutes |
| `getPodDetails` | Get detailed pod info (status, containers, conditions) |
| `getPodLogs` | Get the last 50 lines of logs from a pod |
| `getClusterSummary` | Get pod counts grouped by phase |

## Managing the MCP Server

```bash
# List all configured servers
claude mcp list

# Get details for this server
claude mcp get k8s-mcp-server

# Remove the server
claude mcp remove k8s-mcp-server
```

## Configuration

Edit `src/main/resources/application.yml` to watch additional namespaces:

```yaml
k8swatcher:
  config:
    default:
      nsNames:
        - default
        - kube-system
        - my-app
```

## Project Structure

```
src/main/java/com/example/k8smcp/
├── K8sMcpServerApplication.java     # Entry point with @EnableInformer
├── ClusterEvent.java                # Event record
├── ClusterEventStore.java           # In-memory ring buffer (last 1000 events)
├── KubernetesEventWatcher.java      # @Informer + @Watch handlers for Pods
└── KubernetesMcpTools.java          # @Tool methods exposed via MCP
```
