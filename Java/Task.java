/**
 * Task class representing a unit of work to be processed
 */
public class Task {
    private final int taskId;
    private final int fibonacciNumber; // Fibonacci number to calculate (CPU-intensive)

    public Task(int taskId, int fibonacciNumber) {
        this.taskId = taskId;
        this.fibonacciNumber = fibonacciNumber;
    }

    public int getTaskId() {
        return taskId;
    }

    // public String getData() {
    //     return data;
    // }

    public int getFibonacciNumber() {
        return fibonacciNumber;
    }

    @Override
    public String toString() {
        return "Task{" +
                "taskId=" + taskId +
                // ", data='" + data + '\'' +
                ", fibonacciNumber=" + fibonacciNumber +
                '}';
    }
}

