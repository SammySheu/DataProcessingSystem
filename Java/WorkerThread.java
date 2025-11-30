import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

/**
 * WorkerThread implements Runnable to process tasks from the shared queue
 * Each worker retrieves tasks, processes them, and saves results
 */
public class WorkerThread implements Runnable {
    private static final Logger logger = Logger.getLogger(WorkerThread.class.getName());
    
    private final String workerName;
    private final BlockingQueue<Task> taskQueue;
    private final List<TaskResult> results;
    private final Object resultsLock;
    private int tasksProcessed;

    public WorkerThread(String workerName, BlockingQueue<Task> taskQueue, 
                       List<TaskResult> results, Object resultsLock) {
        this.workerName = workerName;
        this.taskQueue = taskQueue;
        this.results = results;
        this.resultsLock = resultsLock;
        this.tasksProcessed = 0;
    }

    @Override
    public void run() {
        logger.info(workerName + " started and waiting for tasks");
        
        try {
            while (true) {
                try {
                    // Retrieve task from blocking queue (blocks until available)
                    Task task = taskQueue.take();
                    
                    // Check for poison pill (shutdown signal)
                    if (task.getTaskId() == -1) {
                        logger.info(workerName + " received shutdown signal (poison pill)");
                        break;
                    }
                    
                    // Process the task
                    TaskResult result = processTask(task);
                    
                    // Save result to shared results list
                    saveResult(result);
                    
                    tasksProcessed++;
                    
                } catch (InterruptedException e) {
                    logger.warning(workerName + " was interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    break;
                } catch (Exception e) {
                    logger.severe(workerName + " encountered error processing task: " + e.getMessage());
                    // Continue processing other tasks even if one fails
                }
            }
            
        } finally {
            logger.info(workerName + " completed. Total tasks processed: " + tasksProcessed);
        }
    }

    /**
     * Performs CPU-intensive Fibonacci calculation
     * @param n The Fibonacci number to calculate
     * @return The nth Fibonacci number
     */
    private long fibonacci(int n) {
        if (n <= 1) {
            return n;
        }
        return fibonacci(n - 1) + fibonacci(n - 2);
    }

    /**
     * Processes task with actual CPU-intensive computation
     * @param task The task to process
     * @return TaskResult containing the processed result
     */
    private TaskResult processTask(Task task) {
        long startTime = System.currentTimeMillis();
        
        logger.info(workerName + " processing " + task);
        
        try {
            // Perform actual CPU-intensive work: recursive Fibonacci calculation
            long fibResult = fibonacci(task.getFibonacciNumber());
            
            // Process the data with computation result
            String processedData = String.format("Task%d_PROCESSED_BY_%s_FIB(%d)=%d", 
                task.getTaskId(), workerName, task.getFibonacciNumber(), fibResult);
            
            long endTime = System.currentTimeMillis();
            
            logger.info(workerName + " completed task " + task.getTaskId() + 
                       " [Fib(" + task.getFibonacciNumber() + ")=" + fibResult + "] in " + 
                       (endTime - startTime) + "ms");
            
            return new TaskResult(task.getTaskId(), processedData, startTime, endTime, workerName);
            
        } catch (Exception e) {
            logger.warning(workerName + " encountered error while processing task " + 
                         task.getTaskId() + ": " + e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Saves the result to the shared results list in a thread-safe manner
     * @param result The result to save
     */
    private void saveResult(TaskResult result) {
        if (result == null) return;
        
        // Synchronize access to shared results list
        synchronized (resultsLock) {
            results.add(result);
            logger.info(workerName + " saved result for task " + result.getTaskId());
        }
    }

    /**
     * Writes results to a file (used by main thread after all processing is complete)
     * @param results List of results to write
     * @param filename Output filename
     * @param numWorkers Number of workers
     * @param totalExecutionTime Total wall-clock time in milliseconds
     * @throws IOException if file writing fails
     */
    public static void writeResultsToFile(List<TaskResult> results, String filename, int numWorkers, long totalExecutionTime) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("=== Data Processing System Results ===\n\n");
            writer.write("Total results: " + results.size() + "\n\n");
            
            for (TaskResult result : results) {
                writer.write(result.toString() + "\n");
            }
            
            writer.write("\n=== Processing Summary ===\n");
            long totalDuration = results.stream().mapToLong(TaskResult::getProcessingDuration).sum();
            writer.write("Total workers: " + numWorkers + "\n");
            writer.write("Total Wall-Clock Time: " + totalExecutionTime + "ms (ACTUAL TIME)\n");
            // writer.write("Total processing time: " + totalDuration + "ms\n");
            writer.write("Average processing time: " + (results.isEmpty() ? 0 : totalDuration / results.size()) + "ms\n");
            
            logger.info("Results written to file: " + filename);
        }
    }
}

