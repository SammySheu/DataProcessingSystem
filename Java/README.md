# Multi-threaded Data Processing System - Java Implementation

## Overview
This Java implementation demonstrates a multi-threaded data processing system with proper concurrency management and error handling. The system uses multiple worker threads to process tasks from a shared queue concurrently.

## Architecture

### Components

1. **Task.java** - Represents a unit of work to be processed
   - Contains task ID, data, and processing time

2. **TaskResult.java** - Represents the result of a processed task
   - Contains task ID, processed result, timing information, and worker name

3. **BlockingQueue** - Java's built-in thread-safe queue
   - Uses `LinkedBlockingQueue` for efficient concurrent access
   - Provides `put()` and `take()` methods with automatic blocking
   - Implements poison pill pattern for graceful shutdown

4. **WorkerThread.java** - Worker thread implementation
   - Implements `Runnable` interface
   - Retrieves tasks from shared queue
   - Processes tasks with simulated delay
   - Saves results to shared results list

5. **DataProcessingSystem.java** - Main system coordinator
   - Creates thread pool using `ExecutorService`
   - Manages worker lifecycle
   - Handles exceptions and ensures proper shutdown
   - Saves results to file

## Concurrency Features

### Synchronization Techniques
- **BlockingQueue**: Java's built-in concurrent queue with automatic thread-safe operations
- **Poison Pill Pattern**: Special tasks (ID = -1) signal workers to shut down gracefully
- **Synchronized Blocks**: Used for protecting shared results list
- **ExecutorService**: Thread pool management for efficient resource utilization

### Deadlock Prevention
- `BlockingQueue` handles internal locking automatically
- Poison pill pattern ensures clean termination without deadlocks
- Proper use of `finally` blocks ensures resources are always released

### Error Handling
- **try-catch blocks**: Handle `InterruptedException` and `IOException`
- **Graceful degradation**: Workers continue processing even if one task fails
- **Proper cleanup**: Resources are released in `finally` blocks
- **Timeout handling**: Executor shutdown with timeout prevents indefinite waiting

## Requirements

- Java 8 or higher (uses lambda expressions and streams)
- No external dependencies required

## How to Compile

```bash
javac *.java
```

## How to Run

```bash
java DataProcessingSystem
```

## Configuration

You can modify these constants in `DataProcessingSystem.java`:
- `NUM_WORKERS`: Number of worker threads (default: 4)
- `NUM_TASKS`: Number of tasks to process (default: 24)
- `OUTPUT_FILE`: Name of output file (default: "results_java.txt")

## Key Concepts Demonstrated

1. **Thread Pool Management**: Using `ExecutorService` for efficient thread management
2. **Producer-Consumer Pattern**: Main thread produces tasks, workers consume
3. **BlockingQueue**: Java's thread-safe queue with automatic synchronization
4. **Poison Pill Pattern**: Elegant shutdown mechanism using sentinel values
5. **Exception Handling**: Comprehensive error handling with try-catch blocks
6. **Graceful Shutdown**: Proper cleanup and termination of all threads
7. **Synchronized Collections**: Thread-safe access to shared results

