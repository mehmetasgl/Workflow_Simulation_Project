import java.util.ArrayList;
import java.util.List;

public class Job {
    public enum JobState {
        WAITING,
        EXECUTING,
        COMPLETED
    }

    private String jobId;
    private String jobType;
    private int startTime;
    private int duration;
    private int deadline;
    private JobState currentState;
    private List<Task> tasks;

    public Job(String jobId, String jobType, int startTime, int duration) {
        this.jobId = jobId;
        this.jobType = jobType;
        this.startTime = startTime;
        this.duration = duration;
        this.deadline = startTime + duration;
        this.currentState = JobState.WAITING;
        this.tasks = new ArrayList<>();
    }
    public Job(){

    }
    public String getJobId() {
        return jobId;
    }

    public String getJobType() {
        return jobType;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getDuration() {
        return duration;
    }

    public int getDeadline() {
        return deadline;
    }

    public JobState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(JobState currentState) {
        this.currentState = currentState;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public boolean isJobCompleted() {
        for (Task task : tasks) {
            if (!task.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    public Task getNextTask() {
        for (Task task : tasks) {
            if (!task.isCompleted()) {
                return task;
            }
        }
        return null;
    }

    public void updateCurrentState() {
        if (isJobCompleted()) {
            setCurrentState(JobState.COMPLETED);
        } else {
            Task nextTask = getNextTask();
            if (nextTask != null && nextTask.getState() == Task.TaskState.EXECUTING) {
                setCurrentState(JobState.EXECUTING);
            } else {
                setCurrentState(JobState.WAITING);
            }
        }
    }

    @Override
    public String toString() {
        return "Job{" +
                "jobId='" + jobId + '\'' +
                ", jobType='" + jobType + '\'' +
                ", startTime=" + startTime +
                ", duration=" + duration +
                ", deadline=" + deadline +
                ", currentState=" + currentState +
                '}';
    }
}