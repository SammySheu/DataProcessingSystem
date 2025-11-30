/**
 * TaskResult class representing the result of a processed task
 */
public class TaskResult {
    private final int taskId;
    private final String result;
    private final long processingStartTime;
    private final long processingEndTime;
    private final String workerName;

    public TaskResult(int taskId, String result, long startTime, long endTime, String workerName) {
        this.taskId = taskId;
        this.result = result;
        this.processingStartTime = startTime;
        this.processingEndTime = endTime;
        this.workerName = workerName;
    }

    public int getTaskId() {
        return taskId;
    }

    public String getResult() {
        return result;
    }

    public long getProcessingDuration() {
        return processingEndTime - processingStartTime;
    }

    public String getWorkerName() {
        return workerName;
    }

    @Override
    public String toString() {
        return "TaskResult{" +
                "taskId=" + taskId +
                ", result='" + result + '\'' +
                ", duration=" + getProcessingDuration() + "ms" +
                ", worker='" + workerName + '\'' +
                '}';
    }
}

