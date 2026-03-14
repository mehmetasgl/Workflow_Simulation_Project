public class Task {
    public enum TaskState {
        WAITING,
        EXECUTING,
        COMPLETED
    }

    private String taskType;
    private double taskSize;
    private int deadline;
    private TaskState state;
    private Job job;

    public Task(String taskType, double taskSize, int deadline, Job job) {
        this.taskType = taskType;
        this.taskSize = taskSize;
        this.deadline = deadline;
        this.state = TaskState.WAITING;
        this.job = job;
    }

    public String getTaskType() {
        return taskType;
    }

    public double getTaskSize() {
        return taskSize;
    }

    public int getDeadline() {
        return deadline;
    }

    public TaskState getState() {
        return state;
    }

    public void setState(TaskState state) {
        this.state = state;
    }

    public Job getJob() {
        return job;
    }

    public boolean isCompleted() {
        return state == TaskState.COMPLETED;
    }
    public void setCompleted(boolean completed) {
        if (completed) {
            this.state = TaskState.COMPLETED;
        }
    }
    @Override
    public String toString() {
        return "Task{" +
                "taskType='" + taskType + '\'' +
                ", taskSize=" + taskSize +
                ", deadline=" + deadline +
                ", state=" + state +
                '}';
    }
}