package io.k8swatcher.annotation.processor.executor;

import java.util.Queue;
import java.util.concurrent.*;
import lombok.Getter;

/**
 * <br>
 * This is a modified version of https://github.com/fabric8io/kubernetes-client/blob/076a981c9845c691a3d45a727fd48e03377abe8f/kubernetes-client/src/main/java/io/fabric8/kubernetes/client/utils/internal/SerialExecutor.java
 * <br>
 */
public class InformerEventExecutor implements Executor {

    private final Queue<Runnable> tasks = new LinkedBlockingDeque<>();

    private final ExecutorService executor;
    private Runnable active;

    @Getter
    private volatile boolean shutdown;

    private Thread thread;

    private final Object threadLock = new Object();

    public InformerEventExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public synchronized void execute(final Runnable r) {
        if (shutdown) {
            throw new RejectedExecutionException();
        }
        tasks.offer(() -> {
            try {
                if (shutdown) {
                    return;
                }
                synchronized (threadLock) {
                    thread = Thread.currentThread();
                }
                CompletableFuture.runAsync(r);
            } catch (Throwable t) {
                thread.getUncaughtExceptionHandler().uncaughtException(thread, t);
            } finally {
                synchronized (threadLock) {
                    thread = null;
                }
                Thread.interrupted();
                scheduleNext();
            }
        });
        if (active == null) {
            scheduleNext();
        }
    }

    protected synchronized void scheduleNext() {
        if ((active = tasks.poll()) != null) {
            executor.execute(active);
        }
    }
}
