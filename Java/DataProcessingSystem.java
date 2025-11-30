import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * DataProcessingSystem demonstrates multi-threaded data processing
 * with proper synchronization and error handling in Java
 */
public class DataProcessingSystem {
    private static final Logger logger = Logger.getLogger(DataProcessingSystem.class.getName());
    private static final int NUM_WORKERS = 4;
    private static final int NUM_TASKS = 24;
    private static final String OUTPUT_FILE = "results_java.txt";

    public static void main(String[] args) {
        // Configure logging
        setupLogging();
        
        logger.info("=== Data Processing System Starting ===");
        logger.info("Configuration: " + NUM_WORKERS + " workers, " + NUM_TASKS + " tasks");
        
        // Record start time for total execution measurement
        long systemStartTime = System.currentTimeMillis();
        
        // Create shared resources
        SharedTaskQueue taskQueue = new SharedTaskQueue();
        List<TaskResult> results = Collections.synchronizedList(new ArrayList<>());
        Object resultsLock = new Object();
        
        // Create thread pool using Executors
        ExecutorService executor = Executors.newFixedThreadPool(NUM_WORKERS);
        
        try {
            // Submit worker threads to executor
            for (int i = 1; i <= NUM_WORKERS; i++) {
                WorkerThread worker = new WorkerThread(
                    "Worker-" + i, 
                    taskQueue, 
                    results, 
                    resultsLock
                );
                executor.submit(worker);
            }
            
            logger.info("All workers submitted to executor");
            
            // Add tasks to the queue
            addTasksToQueue(taskQueue);
            
            // Signal that no more tasks will be added
            taskQueue.shutdown();
            
            // Shutdown executor gracefully
            executor.shutdown();
            
            logger.info("Waiting for all workers to complete...");
            
            // Wait for all tasks to complete (with timeout)
            boolean completed = executor.awaitTermination(60, TimeUnit.SECONDS);
            
            if (!completed) {
                logger.warning("Timeout occurred, forcing shutdown");
                executor.shutdownNow();
            } else {
                logger.info("All workers completed successfully");
            }
            
            // Calculate total system execution time
            long systemEndTime = System.currentTimeMillis();
            long totalExecutionTime = systemEndTime - systemStartTime;
            
            // Display and save results
            displayResults(results, totalExecutionTime);
            saveResultsToFile(results, NUM_WORKERS, totalExecutionTime);
            
        } catch (InterruptedException e) {
            logger.severe("Main thread was interrupted: " + e.getMessage());
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.severe("Unexpected error in main thread: " + e.getMessage());
            e.printStackTrace();
        } finally {
            logger.info("=== Data Processing System Terminated ===");
        }
    }

    /**
     * Configures logging to display detailed information
     */
    private static void setupLogging() {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);
        
        // Remove default handlers
        for (java.util.logging.Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        
        // Add console handler with custom format
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.INFO);
        handler.setFormatter(new SimpleFormatter());
        rootLogger.addHandler(handler);
    }

    /**
     * Adds tasks to the shared queue
     * @param taskQueue The shared task queue
     */
    private static void addTasksToQueue(SharedTaskQueue taskQueue) {
        logger.info("Adding tasks to queue...");
        
        String[] dataTypes = {
            "CustomerData", "SalesData", "InventoryData", 
            "LogData", "AnalyticsData", "UserActivity",
            "TransactionData", "SensorData", "ReportData", "MetricsData"
        };
        
        // All tasks will compute Fibonacci(30) for consistent CPU-intensive work
        int fibNumber = 43;
        
        for (int i = 1; i <= NUM_TASKS; i++) {
            String data = dataTypes[(i - 1) % dataTypes.length] + "_" + i;
            Task task = new Task(i, data, fibNumber);
            taskQueue.addTask(task);
        }
        
        logger.info("All tasks added to queue (each computing Fibonacci(" + fibNumber + "))");
    }

    /**
     * Displays results summary
     * @param results List of task results
     * @param totalExecutionTime Total wall-clock time in milliseconds
     */
    private static void displayResults(List<TaskResult> results, long totalExecutionTime) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("=== PROCESSING RESULTS SUMMARY ===");
        System.out.println("=".repeat(70));
        System.out.println("Total tasks processed: " + results.size());
        System.out.println("Total workers: " + NUM_WORKERS);
        
        if (!results.isEmpty()) {
            long totalDuration = results.stream()
                .mapToLong(TaskResult::getProcessingDuration)
                .sum();
            long avgDuration = totalDuration / results.size();
            
            // System.out.println("\n--- Timing Analysis ---");
            System.out.println("Total Wall-Clock Time:    " + totalExecutionTime + "ms (ACTUAL TIME)");
            // System.out.println("Sum of All Task Times:    " + totalDuration + "ms (if sequential)");
            System.out.println("Average Time per Task:    " + avgDuration + "ms");
            
            // Calculate speedup
            double speedup = (double) totalDuration / totalExecutionTime;
            // double efficiency = (speedup / NUM_WORKERS) * 100;
            // System.out.println("\n--- Concurrency Benefits ---");
            System.out.println("Speedup Factor:           " + String.format("%.2f", speedup) + "x");
            // System.out.println("Parallel Efficiency:      " + String.format("%.1f", efficiency) + "%");
            // System.out.println("Time Saved:               " + (totalDuration - totalExecutionTime) + "ms");
            
            // Count tasks by worker
            System.out.println("\n--- Tasks per Worker ---");
            results.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    TaskResult::getWorkerName,
                    java.util.stream.Collectors.counting()
                ))
                .forEach((worker, count) -> 
                    System.out.println(worker + ": " + count + " tasks")
                );
            
            // Display first few results
            // System.out.println("\n--- Sample Results (First 5) ---");
            // results.stream()
            //     .limit(5)
            //     .forEach(System.out::println);
        }
        System.out.println("=".repeat(70) + "\n");
    }

    /**
     * Saves results to a file
     * @param results List of task results
     * @param numWorkers Number of workers
     * @param totalExecutionTime Total wall-clock time in milliseconds
     */
    private static void saveResultsToFile(List<TaskResult> results, int numWorkers, long totalExecutionTime) {
        try {
            WorkerThread.writeResultsToFile(results, OUTPUT_FILE, numWorkers, totalExecutionTime);
            logger.info("Results successfully saved to " + OUTPUT_FILE);
        } catch (IOException e) {
            logger.severe("Failed to write results to file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

