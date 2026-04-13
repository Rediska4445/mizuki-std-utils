package rf.ebanina.utils.concurrency;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class PriorityThreadFactory
        implements ThreadFactory
{
    private final String namePrefix;
    private final int priority;
    private final boolean isDaemon;
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    public PriorityThreadFactory(String namePrefix, int priority, boolean isDaemon) {
        this.namePrefix = namePrefix + "-thread-";
        this.priority = priority;
        this.isDaemon = isDaemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, namePrefix + threadNumber.getAndIncrement());
        thread.setPriority(priority);
        thread.setDaemon(isDaemon);
        return thread;
    }
}