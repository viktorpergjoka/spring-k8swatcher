package io.k8swatcher.annotation.processor.executor;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;

public class InformerEventExecutor implements Executor {

    private final Queue<Runnable> tasks = new LinkedBlockingDeque<>();

    private final ExecutorService executor;

    private Runnable active;

    public InformerEventExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public synchronized void execute(final Runnable r) {
        tasks.offer(() -> {
            System.out.println("future before");
            CompletableFuture.runAsync(r);
            System.out.println("future after");
            scheduleNext();
        });
        if (active == null) {
            scheduleNext();
        }
    }

    protected synchronized void scheduleNext() {
        active = tasks.poll();
        if (active != null) {
            executor.execute(active);
        }
    }
}
