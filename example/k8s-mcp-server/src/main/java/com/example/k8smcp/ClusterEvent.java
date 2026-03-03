package com.example.k8smcp;

import java.time.Instant;

public record ClusterEvent(
    Instant timestamp,
    String eventType,
    String resourceKind,
    String resourceName,
    String namespace,
    String summary
) {}
