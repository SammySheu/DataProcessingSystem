package main

import (
	"fmt"
	"log"
	"os"
	"sync"
	"time"
)

// Task represents a unit of work to be processed
type Task struct {
	TaskID          int
	Data            string
	FibonacciNumber int // Fibonacci number to calculate (CPU-intensive)
}

// TaskResult represents the result of a processed task
type TaskResult struct {
	TaskID               int
	Result               string
	ProcessingStartTime  time.Time
	ProcessingEndTime    time.Time
	WorkerName           string
	ProcessingDurationMs int64
}

// String representation of Task
func (t Task) String() string {
	return fmt.Sprintf("Task{TaskID: %d, Data: '%s', FibonacciNumber: %d}",
		t.TaskID, t.Data, t.FibonacciNumber)
}

// String representation of TaskResult
func (r TaskResult) String() string {
	return fmt.Sprintf("TaskResult{TaskID: %d, Result: '%s', Duration: %dms, Worker: '%s'}",
		r.TaskID, r.Result, r.ProcessingDurationMs, r.WorkerName)
}

const (
	numWorkers = 4
	numTasks   = 24
	outputFile = "results_go.txt"
)

func main() {
	// Configure logging
	log.SetFlags(log.Ldate | log.Ltime | log.Lmicroseconds)

	log.Println("=== Data Processing System Starting ===")
	log.Printf("Configuration: %d workers, %d tasks\n", numWorkers, numTasks)

	// Record start time for total execution measurement
	systemStartTime := time.Now()

	// Create channels
	taskQueue := make(chan Task, 10)           // Buffered channel for tasks
	results := make(chan TaskResult, numTasks) // Buffered channel for results

	// WaitGroup to wait for all workers to complete
	var wg sync.WaitGroup

	// Start worker goroutines
	for i := 1; i <= numWorkers; i++ {
		wg.Add(1)
		workerName := fmt.Sprintf("Worker-%d", i)
		go worker(workerName, taskQueue, results, &wg)
	}

	// Start a goroutine to add tasks to the queue
	go func() {
		addTasksToQueue(taskQueue)
		close(taskQueue) // Close channel to signal no more tasks
		log.Println("Task queue closed - no more tasks will be added")
	}()

	// Start a goroutine to collect results
	allResults := make([]TaskResult, 0, numTasks)
	var resultsMutex sync.Mutex
	var resultsWg sync.WaitGroup
	resultsWg.Add(1)

	go func() {
		defer resultsWg.Done()
		for result := range results {
			resultsMutex.Lock()
			allResults = append(allResults, result)
			resultsMutex.Unlock()
			// log.Printf("%s saved result for task %d\n", result.WorkerName, result.TaskID)
		}
	}()

	// Wait for all workers to complete
	wg.Wait()
	log.Println("All workers completed")

	// Close results channel and wait for result collection to finish
	close(results)
	resultsWg.Wait()

	// Calculate total system execution time
	totalExecutionTime := time.Since(systemStartTime).Milliseconds()

	// Display and save results
	displayResults(allResults, totalExecutionTime)

	if err := saveResultsToFile(allResults, outputFile, totalExecutionTime); err != nil {
		log.Printf("ERROR: Failed to save results to file: %v\n", err)
	} else {
		log.Printf("Results successfully saved to %s\n", outputFile)
	}

	log.Println("=== Data Processing System Terminated ===")
}

// worker is a goroutine that processes tasks from the task queue
func worker(workerName string, taskQueue <-chan Task, results chan<- TaskResult, wg *sync.WaitGroup) {
	defer wg.Done()
	defer func() {
		// Handle any panics that might occur during task processing
		if r := recover(); r != nil {
			log.Printf("ERROR: %s recovered from panic: %v\n", workerName, r)
		}
	}()

	tasksProcessed := 0
	log.Printf("%s started and waiting for tasks\n", workerName)

	// Process tasks from the channel until it's closed
	for task := range taskQueue {
		result, err := processTask(workerName, task)
		if err != nil {
			log.Printf("ERROR: %s failed to process task %d: %v\n", workerName, task.TaskID, err)
			continue // Continue processing other tasks
		}

		// Send result to results channel
		results <- result
		tasksProcessed++
	}

	log.Printf("%s completed. Total tasks processed: %d\n", workerName, tasksProcessed)
}

// fibonacci performs recursive Fibonacci calculation (CPU-intensive)
func fibonacci(n int) int64 {
	if n <= 1 {
		return int64(n)
	}
	return fibonacci(n-1) + fibonacci(n-2)
}

// processTask performs actual CPU-intensive computation
func processTask(workerName string, task Task) (TaskResult, error) {
	startTime := time.Now()

	log.Printf("%s processing %s\n", workerName, task.String())

	// Perform actual CPU-intensive work: recursive Fibonacci calculation
	fibResult := fibonacci(task.FibonacciNumber)

	// Process the data with computation result
	processedData := fmt.Sprintf("%s_PROCESSED_BY_%s_FIB(%d)=%d",
		task.Data, workerName, task.FibonacciNumber, fibResult)

	endTime := time.Now()
	duration := endTime.Sub(startTime).Milliseconds()

	log.Printf("%s completed task %d [Fib(%d)=%d] in %dms\n",
		workerName, task.TaskID, task.FibonacciNumber, fibResult, duration)

	result := TaskResult{
		TaskID:               task.TaskID,
		Result:               processedData,
		ProcessingStartTime:  startTime,
		ProcessingEndTime:    endTime,
		WorkerName:           workerName,
		ProcessingDurationMs: duration,
	}

	return result, nil
}

// addTasksToQueue creates and adds tasks to the task queue
func addTasksToQueue(taskQueue chan<- Task) {
	log.Println("Adding tasks to queue...")

	// dataTypes := []string{
	// 	"CustomerData", "SalesData", "InventoryData",
	// 	"LogData", "AnalyticsData", "UserActivity",
	// 	"TransactionData", "SensorData", "ReportData", "MetricsData",
	// }

	// All tasks will compute Fibonacci(30) for consistent CPU-intensive work
	fibNumber := 43

	for i := 1; i <= numTasks; i++ {
		// data := fmt.Sprintf("%s_%d", dataTypes[(i-1)%len(dataTypes)], i)
		task := Task{
			TaskID: i,
			// Data:            data,
			FibonacciNumber: fibNumber,
		}

		taskQueue <- task
		log.Printf("Task %d added to queue\n", task.TaskID)
	}

	log.Printf("All tasks added to queue (each computing Fibonacci(%d))\n", fibNumber)
}

// displayResults prints a summary of processing results
func displayResults(results []TaskResult, totalExecutionTime int64) {
	fmt.Println()
	fmt.Println("======================================================================")
	fmt.Println("=== PROCESSING RESULTS SUMMARY ===")
	fmt.Println("======================================================================")
	fmt.Printf("Total tasks processed: %d\n", len(results))
	fmt.Printf("Total workers: %d\n", numWorkers)

	if len(results) > 0 {
		var totalDuration int64
		workerTaskCount := make(map[string]int)

		for _, result := range results {
			totalDuration += result.ProcessingDurationMs
			workerTaskCount[result.WorkerName]++
		}

		avgDuration := totalDuration / int64(len(results))

		// fmt.Println("\n--- Timing Analysis ---")
		fmt.Printf("Total Wall-Clock Time:    %dms (ACTUAL TIME)\n", totalExecutionTime)
		// fmt.Printf("Sum of All Task Times:    %dms (if sequential)\n", totalDuration)
		fmt.Printf("Average Time per Task:    %dms\n", avgDuration)

		// Calculate speedup
		speedup := float64(totalDuration) / float64(totalExecutionTime)
		// efficiency := (speedup / float64(numWorkers)) * 100
		// fmt.Println("\n--- Concurrency Benefits ---")
		fmt.Printf("Speedup Factor:           %.2fx\n", speedup)
		// fmt.Printf("Parallel Efficiency:      %.1f%%\n", efficiency)
		// fmt.Printf("Time Saved:               %dms\n", totalDuration-totalExecutionTime)

		// Display tasks per worker
		fmt.Println("\n--- Tasks per Worker ---")
		for worker, count := range workerTaskCount {
			fmt.Printf("%s: %d tasks\n", worker, count)
		}

		// Display first few results
		// fmt.Println("\n--- Sample Results (First 5) ---")
		// displayCount := 5
		// if len(results) < displayCount {
		// 	displayCount = len(results)
		// }
		// for i := 0; i < displayCount; i++ {
		// 	fmt.Println(results[i].String())
		// }
	}
	fmt.Println("======================================================================")
	fmt.Println()
}

// saveResultsToFile writes results to a file
func saveResultsToFile(results []TaskResult, filename string, totalExecutionTime int64) error {
	file, err := os.Create(filename)
	if err != nil {
		return fmt.Errorf("failed to create file: %w", err)
	}
	defer file.Close()

	// Write header
	if _, err := fmt.Fprintf(file, "=== Data Processing System Results ===\n\n"); err != nil {
		return fmt.Errorf("failed to write header: %w", err)
	}

	if _, err := fmt.Fprintf(file, "Total results: %d\n\n", len(results)); err != nil {
		return fmt.Errorf("failed to write total count: %w", err)
	}

	// Write individual results
	for _, result := range results {
		if _, err := fmt.Fprintf(file, "%s\n", result.String()); err != nil {
			return fmt.Errorf("failed to write result: %w", err)
		}
	}

	// Write summary
	if len(results) > 0 {
		var totalDuration int64
		for _, result := range results {
			totalDuration += result.ProcessingDurationMs
		}
		avgDuration := totalDuration / int64(len(results))

		if _, err := fmt.Fprintf(file, "\n=== Processing Summary ===\n"); err != nil {
			return fmt.Errorf("failed to write summary header: %w", err)
		}
		if _, err := fmt.Fprintf(file, "Total workers: %d\n", numWorkers); err != nil {
			return fmt.Errorf("failed to write total workers: %w", err)
		}
		if _, err := fmt.Fprintf(file, "Total Wall-Clock Time:    %dms (ACTUAL TIME)\n", totalExecutionTime); err != nil {
			return fmt.Errorf("failed to write total wall-clock time: %w", err)
		}
		if _, err := fmt.Fprintf(file, "Average processing time: %dms\n", avgDuration); err != nil {
			return fmt.Errorf("failed to write average duration: %w", err)
		}
	}

	return nil
}
