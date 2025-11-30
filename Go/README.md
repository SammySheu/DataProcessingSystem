# Multi-threaded Data Processing System - Go Implementation

## Overview
This Go implementation demonstrates concurrent data processing using goroutines and channels. The system leverages Go's native concurrency primitives to process tasks efficiently and safely.

## Architecture

### Components

1. **Task struct** - Represents a unit of work to be processed
   - Contains task ID, data, and processing time

2. **TaskResult struct** - Represents the result of a processed task
   - Contains task ID, processed result, timing information, and worker name

3. **main()** - Main coordinator function
   - Creates and manages channels
   - Spawns worker goroutines
   - Coordinates task distribution and result collection
   - Handles graceful shutdown

4. **worker()** - Worker goroutine function
   - Receives tasks from task channel
   - Processes tasks with simulated delay
   - Sends results to results channel
   - Includes panic recovery for robustness

5. **processTask()** - Task processing function
   - Simulates computational work
   - Returns processed result or error

6. **Helper functions** - Utility functions
   - `addTasksToQueue()`: Creates and adds tasks to channel
   - `displayResults()`: Prints processing summary
   - `saveResultsToFile()`: Writes results to file with error handling

## Concurrency Features

### Go Concurrency Model
- **Goroutines**: Lightweight threads managed by Go runtime
- **Channels**: Type-safe communication between goroutines
- **Buffered Channels**: Used for task queue and results to prevent blocking
- **WaitGroups**: Synchronize completion of all goroutines

### Channel-Based Communication
```go
taskQueue := make(chan Task, 10)    // Buffered channel for tasks
results := make(chan TaskResult, numTasks) // Buffered channel for results
```

### Synchronization
- **sync.WaitGroup**: Ensures all workers complete before shutdown
- **sync.Mutex**: Protects shared results slice during collection
- **Channel closing**: Signals completion (closing taskQueue signals no more tasks)

### Error Handling
- **Error returns**: Functions return errors that are checked and handled
- **Panic recovery**: `defer recover()` in workers catches panics
- **Graceful degradation**: Workers continue processing even if one task fails
- **defer statements**: Ensure proper cleanup with `defer file.Close()`

### Deadlock Prevention
- Proper channel closing prevents goroutines from waiting indefinitely
- WaitGroups ensure orderly shutdown
- Buffered channels reduce blocking
- No circular dependencies in channel communication

## Requirements

- Go 1.21 or higher (uses Go modules)
- No external dependencies required

## How to Build

```bash
go build -o dataprocessing
```

## How to Run

```bash
go run main.go
```

Or after building:
```bash
./dataprocessing
```

## Configuration

You can modify these constants in `main.go`:
- `numWorkers`: Number of worker goroutines (default: 4)
- `numTasks`: Number of tasks to process (default: 20)
- `outputFile`: Name of output file (default: "results_go.txt")


## Key Go Concepts Demonstrated

1. **Goroutines**: Lightweight concurrent execution
2. **Channels**: Type-safe communication between goroutines
3. **Select Statement**: Could be added for timeout handling
4. **Buffered Channels**: Non-blocking communication up to buffer size
5. **WaitGroups**: Synchronization primitive for waiting on goroutines
6. **Mutex**: Mutual exclusion for shared data access
7. **Defer**: Ensure cleanup code runs (file closing, WaitGroup.Done())
8. **Error Handling**: Explicit error returns and checking
9. **Panic Recovery**: Catching and handling runtime panics