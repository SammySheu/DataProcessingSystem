import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * SharedTaskQueue implements a thread-safe queue for tasks
 * Uses ReentrantLock for fine-grained synchronization control
 */
public class SharedTaskQueue {
    private static final Logger logger = Logger.getLogger(SharedTaskQueue.class.getName());
    
    private final Queue<Task> queue;
    private final ReentrantLock lock;
    private final Condition notEmpty;
    private boolean shutdownRequested;
    private int totalTasksAdded;
    private int totalTasksRetrieved;

    public SharedTaskQueue() {
        this.queue = new LinkedList<>();
        this.lock = new ReentrantLock();
        this.notEmpty = lock.newCondition();
        this.shutdownRequested = false;
        this.totalTasksAdded = 0;
        this.totalTasksRetrieved = 0;
    }

    /**
     * Adds a task to the queue in a thread-safe manner
     * @param task The task to be added
     */
    public void addTask(Task task) {
        lock.lock();
        try {
            if (shutdownRequested) {
                logger.warning("Cannot add task - queue is shutting down");
                return;
            }
            queue.offer(task);
            totalTasksAdded++;
            notEmpty.signal(); // Wake up one waiting thread
            logger.info("Task " + task.getTaskId() + " added to queue. Queue size: " + queue.size());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves a task from the queue in a thread-safe manner
     * Blocks if queue is empty until a task becomes available or shutdown is requested
     * @return Task or null if shutdown is requested
     * @throws InterruptedException if thread is interrupted while waiting
     */
    public Task getTask() throws InterruptedException {
        lock.lock();
        try {
            while (queue.isEmpty() && !shutdownRequested) {
                notEmpty.await(); // Wait for a task to be available
            }
            
            if (shutdownRequested && queue.isEmpty()) {
                return null; // No more tasks and shutdown requested
            }
            
            Task task = queue.poll();
            if (task != null) {
                totalTasksRetrieved++;
                logger.info("Task " + task.getTaskId() + " retrieved from queue. Queue size: " + queue.size());
            }
            return task;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Signals that no more tasks will be added to the queue
     */
    public void shutdown() {
        lock.lock();
        try {
            shutdownRequested = true;
            notEmpty.signalAll(); // Wake up all waiting threads
            logger.info("Queue shutdown requested. Tasks added: " + totalTasksAdded + 
                       ", Tasks retrieved: " + totalTasksRetrieved);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the current size of the queue
     * @return number of tasks in queue
     */
    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Checks if the queue is empty
     * @return true if queue is empty
     */
    public boolean isEmpty() {
        lock.lock();
        try {
            return queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }
}

