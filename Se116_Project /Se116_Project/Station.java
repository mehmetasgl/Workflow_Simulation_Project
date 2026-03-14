import java.util.*;


class Station {
    private final String stationId;
    private final int maxCapacity;
    private final boolean multiFlag;
    private final boolean fifoFlag;
    private final Map<String, Double> taskSpeeds;
    private final List<Task> executingTasks;
    private final Queue<Task> waitingTasks;

    public Station(String stationId, int maxCapacity, boolean multiFlag, boolean fifoFlag, Map<String, Double> taskSpeeds) {
        this.stationId = stationId;
        this.maxCapacity = maxCapacity;
        this.multiFlag = multiFlag;
        this.fifoFlag = fifoFlag;
        this.taskSpeeds = taskSpeeds;
        this.executingTasks = new ArrayList<>();
        this.waitingTasks = fifoFlag ? new LinkedList<>() : new PriorityQueue<>(Comparator.comparingInt(Task::getDeadline));
    }

    public String getStationId() {
        return stationId;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public boolean isMultiFlag() {
        return multiFlag;
    }

    public boolean isFifoFlag() {
        return fifoFlag;
    }

    public Map<String, Double> getTaskSpeeds() {
        return taskSpeeds;
    }

    public List<Task> getExecutingTasks() {
        return executingTasks;
    }

    public Queue<Task> getWaitingTasks() {
        return waitingTasks;
    }

    public boolean isIdle() {
        return executingTasks.isEmpty() && waitingTasks.isEmpty();
    }

    public void assignTask(Task task) {
        executingTasks.add(task);
    }
    public void assignTaskForWaiting(Task task) {
        waitingTasks.offer(task);
        //waitingTasks.add(task);
    }

    public void completeTask(Task task) {
        executingTasks.remove(task);
    }

    public boolean canHandleTask(String taskType, double taskSize) {
        return taskSpeeds.containsKey(taskType) && (executingTasks.size() < maxCapacity);
    }

    public double getEffectiveSpeed(String taskType) {
        return taskSpeeds.getOrDefault(taskType, 0.0);
    }

    public int calculateTaskDuration(double taskSize, double stationSpeed) {
        return (int) Math.ceil(taskSize / stationSpeed);
    }

    public boolean hasWaitingTasks() {
        return !waitingTasks.isEmpty();
    }

    public Task getNextWaitingTask() {
        return waitingTasks.poll();
    }
}