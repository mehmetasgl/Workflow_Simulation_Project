class Event {
    private final EventType eventType;
    private final int eventTime;
    private Job job;
    private Task task;
    private Station station;

    public Event(EventType eventType, int eventTime, Job job) {
        this.eventType = eventType;
        this.eventTime = eventTime;
        this.job = job;
    }

    public Event(EventType eventType, int eventTime, Task task, Station station) {
        this.eventType = eventType;
        this.eventTime = eventTime;
        this.task = task;
        this.station = station;
    }

    public EventType getEventType() {
        return eventType;
    }

    public int getEventTime() {
        return eventTime;
    }

    public Job getJob() {
        return job;
    }

    public Task getTask() {
        return task;
    }

    public Station getStation() {
        return station;
    }
    enum EventType {
        JOB_ARRIVAL,
        TASK_COMPLETION
    }

}

