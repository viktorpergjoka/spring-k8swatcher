package com.example.k8smcp;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

@Component
public class ClusterEventStore {

    private static final int MAX_EVENTS = 1000;

    private final ConcurrentLinkedDeque<ClusterEvent> events = new ConcurrentLinkedDeque<>();
    private final AtomicInteger size = new AtomicInteger();

    public void record(ClusterEvent event) {
        events.addFirst(event);
        if (size.incrementAndGet() > MAX_EVENTS) {
            events.removeLast();
            size.decrementAndGet();
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
                .filter(e -> e.resourceName().equals(name) && e.namespace().equals(namespace))
                .toList();
    }
}
