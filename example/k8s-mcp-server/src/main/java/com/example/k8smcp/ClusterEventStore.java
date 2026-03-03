package com.example.k8smcp;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Component;

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
                .filter(e -> e.resourceName().equals(name) && e.namespace().equals(namespace))
                .toList();
    }
}
