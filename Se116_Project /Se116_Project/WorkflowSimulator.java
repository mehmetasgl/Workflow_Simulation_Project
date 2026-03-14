import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;

class WorkflowSimulator {
    private Map<String, Map<String, String>> jobParsedTaskTypesWithSize;
    private final Map<String, Double> taskTypeSizes;
    private ErrorReporter errorReporter;
    private List<Job> jobTypes;
    private final List<Station> stations;
    private final List<String> taskTypes;
    private final PriorityQueue<Event> eventQueue;
    private int currentTime;

    public WorkflowSimulator(String workflowFile, String jobFile, ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
        taskTypeSizes = new HashMap<>();
        taskTypes = new ArrayList<>();
        jobParsedTaskTypesWithSize = new HashMap<>();
        jobTypes = new ArrayList<>();
        stations = new ArrayList<>();
        eventQueue = new PriorityQueue<>(Comparator.comparingInt(Event::getEventTime));
        currentTime = 0;
        read(workflowFile);
        this.jobTypes = readJobFile(jobFile);
    }

    public List<Job> getJobTypes() {
        return jobTypes;
    }

    public List<Station> getStations() {
        return stations;
    }

    public void read(String workflowFile) {
        try {
            File file = new File(workflowFile);
            readWorkflowFile(file);
        } catch (FileNotFoundException e) {
            System.err.println("Workflow file not found.");
        }
    }

    private void readWorkflowFile(File file) throws FileNotFoundException {
        try (Scanner fileScanner = new Scanner(file)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim();
                if (line.startsWith("(TASKTYPES")) {
                    parseTaskTypes(line, new Scanner(file));
                } else if (line.startsWith("(STATIONS")) {
                    parseStations(new Scanner(file));
                } else if (line.startsWith("(JOBTYPES")) {
                    parseJobTypes(new Scanner(file));
                }
            }
        }
    }

    private void parseTaskTypes(String line, Scanner fileScanner) {
        Pattern pattern = Pattern.compile("\\(TASKTYPES\\s+([^)]+)\\)");
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            String content = matcher.group(1).trim();
            String[] parts = content.split("\\s+");
            Set<String> taskTypeSet = new HashSet<>();

            for (int i = 0; i < parts.length; i++) {
                String taskType = parts[i];

                if (taskType.isEmpty()) {
                    continue;
                }

                if (i + 1 < parts.length) {
                    String taskSizeStr = parts[i + 1].trim();
                    try {
                        double taskSize = Double.parseDouble(taskSizeStr);
                        if (taskSize < 0) {
                            errorReporter.reportError("Task type " + taskType + " has a negative task size.");
                            i++;
                            continue;
                        }
                        taskTypeSizes.put(taskType, taskSize);
                        i++;
                    } catch (NumberFormatException e) {
                        errorReporter.reportError("Invalid task size for task type " + taskType + ": " + taskSizeStr);
                    }
                } else {
                    double taskSize = findTaskSizeForTaskType(taskType, fileScanner);
                    if (taskSize >= 0) {
                        taskTypeSizes.put(taskType, taskSize);
                    } else {
                        errorReporter.reportError("Task size not found for task type " + taskType);
                    }
                }

                if (!isValidTaskType(taskType)) {
                    errorReporter.reportError("Invalid task type: " + taskType);
                    continue;
                }

                if (taskTypeSet.contains(taskType)) {
                    errorReporter.reportError("Duplicate task type: " + taskType);
                    continue;
                }

                taskTypeSet.add(taskType);
                taskTypes.add(taskType);
                fileScanner.reset();
            }
        } else {
            fileScanner.reset();
            errorReporter.reportError("Error: 'TASKTYPES' pattern not found in the line: " + line);
        }
        fileScanner.reset();
    }

    private double findTaskSizeForTaskType(String taskType, Scanner fileScanner) {
        Pattern sizePattern = Pattern.compile("\\b" + taskType + "\\s+(\\d+\\.?\\d*)\\b");

        while (fileScanner.hasNextLine()) {
            String line = fileScanner.nextLine();

            Matcher matcher = sizePattern.matcher(line);
            if (matcher.find()) {
                fileScanner.reset();
                return Double.parseDouble(matcher.group(1));
            }
            fileScanner.reset();
        }return -1;
    }

    public void parseJobTypes(Scanner scanner) {
        boolean jobTypesStarted = false;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            System.out.println("Read line: " + line);

            if (line.startsWith("(STATIONS")) {
                break;
            }

            if (line.endsWith("(JOBTYPES")) {
                jobTypesStarted = true;
                continue;
            }

            if (jobTypesStarted) {
                if (line.startsWith("(")) {
                    String jobTypeLine = line.replace("(", "").trim();
                    if (!jobTypeLine.isEmpty()) {
                        String[] parts = jobTypeLine.split("\\s+");
                        if (parts.length > 0) {
                            String jobType = parts[0];
                            Map<String, String> taskTypesWithSize = new HashMap<>();
                            String currentTaskType = null;

                            for (int i = 1; i < parts.length; i++) {
                                String part = parts[i];
                                if (part.isEmpty() || part.equals(")")) {
                                    continue;
                                }

                                if (part.endsWith(")")) {
                                    part = part.substring(0, part.length() - 1);
                                }

                                if (Character.isDigit(part.charAt(0))) {
                                    if (currentTaskType != null) {
                                        taskTypesWithSize.put(currentTaskType, part);
                                    } else {
                                        System.out.println("No task type found for size: " + part);
                                        System.out.println("ALERT: Task type is null for job type: " + jobType);
                                    }
                                } else {
                                    currentTaskType = part;
                                    taskTypesWithSize.put(currentTaskType, "null");
                                }
                            }
                            if (jobParsedTaskTypesWithSize.containsKey(jobType)) {
                                Map<String, String> existingTaskTypesWithSize = jobParsedTaskTypesWithSize.get(jobType);
                                existingTaskTypesWithSize.putAll(taskTypesWithSize);
                                jobParsedTaskTypesWithSize.put(jobType, existingTaskTypesWithSize);
                            } else {
                                jobParsedTaskTypesWithSize.put(jobType, taskTypesWithSize);
                            }
                        }
                    }
                }
            }
        }
    }

    private void printJobTypesWithSizes(Map<String, Map<String, String>> jobParsedTaskTypesWithSize, Map<String, Double> taskTypeSizes) {
        for (Map.Entry<String, Map<String, String>> entry : jobParsedTaskTypesWithSize.entrySet()) {
            String jobType = entry.getKey();
            Map<String, String> taskTypesWithSize = entry.getValue();
            if (taskTypesWithSize.isEmpty()) {
                System.out.println("Job Type: " + jobType + " Task Type: null" + " Task Size: null");
            } else {
                for (Map.Entry<String, String> taskEntry : taskTypesWithSize.entrySet()) {
                    String taskType = taskEntry.getKey();
                    String taskSize = taskEntry.getValue();

                    if (taskType != null) {
                        if (taskSize.equals("null")) {
                            Double sizeFromMap = taskTypeSizes.get(taskType);
                            if (sizeFromMap != null) {
                                System.out.println("Job Type: " + jobType + " Task Type: " + taskType + " Task Size: " + sizeFromMap);
                            } else {
                                System.out.println("Job Type: " + jobType + " Task Type: " + taskType + " Task Size: null");
                            }
                        } else {
                            System.out.println("Job Type: " + jobType + " Task Type: " + taskType + " Task Size: " + taskSize);
                        }
                    } else {
                        System.out.println("Job Type: " + jobType + " Task Type: " + taskType + " Task Size: " + taskSize);
                    }
                }
            }
        }
    }

    private boolean isValidTaskType(String taskType) {
        return taskType.matches("[A-Za-z0-9_]+");
    }

    private void parseStations(Scanner scanner) {
        boolean stationsStarted = false;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();

            if (line.equals("(STATIONS")) {
                stationsStarted = true;
                continue;
            }

            if (stationsStarted) {
                if (line.startsWith("(")) {
                    // Remove the starting '(' and trailing ')' if present
                    line = line.replace("(", "").replace(")", "").trim();
                    String[] stationParts = line.split("\\s+");
                    if (stationParts.length >= 6) {
                        String stationId = stationParts[0];
                        int maxCapacity = Integer.parseInt(stationParts[1]);
                        boolean multiFlag = stationParts[2].equals("Y");
                        boolean fifoFlag = stationParts[3].equals("Y");
                        Map<String, Double> taskSpeeds = new HashMap<>();
                        for (int i = 4; i < stationParts.length; i += 2) {
                            if (i + 1 < stationParts.length) {
                                String taskType = stationParts[i];
                                try {
                                    double speed = Double.parseDouble(stationParts[i + 1]);
                                    taskSpeeds.put(taskType, speed);
                                } catch (NumberFormatException e) {
                                    System.err.println("Invalid speed value for task type " + taskType + ": " + stationParts[i + 1]);
                                }
                            } else {
                                System.err.println("Incomplete stationParts entry: " + Arrays.toString(stationParts));
                            }
                        }
                        Station station = new Station(stationId, maxCapacity, multiFlag, fifoFlag, taskSpeeds);
                        stations.add(station);
                    } else {
                        System.err.println("Invalid stationParts length: " + Arrays.toString(stationParts));
                    }
                }
            }
        }
    }
    private Map<String, Double> getStringDoubleMap(String[] stationParts) {
        Map<String, Double> taskSpeeds = new HashMap<>();

        for (int i = 4; i < stationParts.length; i += 2) {
            if (i + 1 < stationParts.length) {
                String taskType = stationParts[i];
                try {
                    double speed = Double.parseDouble(stationParts[i + 1]);
                    taskSpeeds.put(taskType, speed);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid speed value for task type " + taskType + ": " + stationParts[i + 1]);
                }
            } else {
                System.err.println("Incomplete stationParts entry: " + Arrays.toString(stationParts));
            }
        }

        return taskSpeeds;
    }
    private void printEventDetails(String eventType, int time, String details) {
        System.out.println("Time: " + time + ", Event: " + eventType + ", Details: " + details);
    }

    public void simulateWorkflow() {
        if (errorReporter.hasErrors()) {
            errorReporter.printErrors();
            return;
        } else {
            printParsedWorkflow();
            while (!eventQueue.isEmpty()) {
                Event currentEvent = eventQueue.poll();
                currentTime = currentEvent.getEventTime();
                printEventDetails(currentEvent.getEventType().toString(), currentTime, currentEvent.toString());
                handleEvent(currentEvent);
            }

            reportResults();
        }
    }
    public void runSimulation() {
        while (!eventQueue.isEmpty()) {
            Event event = eventQueue.poll();
            currentTime = event.getEventTime();
            handleEvent(event);
        }
    }


    private void handleEvent(Event event) {
        switch (event.getEventType()) {
            case JOB_ARRIVAL:
                handleJobArrival(event.getJob());
                break;
            case TASK_COMPLETION:
                handleTaskCompletion(event.getTask(), event.getStation());
                break;
        }
    }

    private void handleJobArrival(Job job) {
        System.out.println("Simulating Job Arrival: " + job.getJobId() + " (" + job.getJobType() + ")");
        List<Task> tasks = job.getTasks();
        for (Task task : tasks) {
            if (!isValidTaskType(task.getTaskType())) {
                System.out.println("Invalid task type: " + task.getTaskType());
            } else {
                String taskType = task.getTaskType();
                double taskSize = task.getTaskSize();
                Station station = findSuitableStation(taskType, taskSize);
                if (station != null) {
                    assignTaskToStation(station, task);
                } else {
                    System.out.println("No suitable station found for Task " + taskType);
                }
            }
        }
        reportUnmappedTasks(tasks);
    }

    private void assignTaskToStation(Station station, Task task) {
        if (station.canHandleTask(task.getTaskType(), task.getTaskSize())) {
            double stationSpeed = getStationSpeed(station, task.getTaskType());
            int taskDuration = station.calculateTaskDuration(task.getTaskSize(), stationSpeed);
            int completionTime = currentTime + taskDuration;
            int taskDeadline = task.getDeadline();
            int urgency = taskDeadline - completionTime;

            boolean stationIsBusy = !station.isMultiFlag() && station.getExecutingTasks().size() >= station.getMaxCapacity();

            if (!stationIsBusy) {
                station.assignTask(task);
                System.out.println("Assigned Task " + task.getTaskType() + " to Station " + station.getStationId());
                task.setState(Task.TaskState.EXECUTING);
            } else {
                Task mostUrgentTask = getMostUrgentTask(station);
                if (mostUrgentTask != null && urgency > getTaskUrgency(mostUrgentTask, station)) {
                    station.completeTask(mostUrgentTask);
                    station.assignTask(task);
                    System.out.println("Preempted Task " + mostUrgentTask.getTaskType() + " at Station " + station.getStationId()
                            + " and assigned Task " + task.getTaskType());
                } else {
                    System.out.println("Station " + station.getStationId() + " is at full capacity. Task " + task.getTaskType() + " is waiting.");
                    station.getWaitingTasks().offer(task);
                    task.setState(Task.TaskState.WAITING);
                }
            }
            eventQueue.offer(new Event(Event.EventType.TASK_COMPLETION, completionTime, task, station));
        } else {
            System.out.println("Station " + station.getStationId() + " cannot handle Task " + task.getTaskType());
        }
    }

    private Task getMostUrgentTask(Station station) {
        Task mostUrgentTask = null;
        int maxUrgency = Integer.MIN_VALUE;

        for (Task task : station.getExecutingTasks()) {
            int taskUrgency = getTaskUrgency(task, station);
            if (taskUrgency > maxUrgency) {
                maxUrgency = taskUrgency;
                mostUrgentTask = task;
            }
        }

        return mostUrgentTask;
    }

    private int getTaskUrgency(Task task, Station station) {
        double stationSpeed = getStationSpeed(station, task.getTaskType());
        int remainingTime = (int) Math.ceil(task.getTaskSize() / stationSpeed);
        return task.getDeadline() - (currentTime + remainingTime);
    }

    private boolean isTaskAssigned(String taskType) {
        for (Station station : stations) {
            if (station.canHandleTask(taskType, 0.0)) {
                return true;
            }
        }
        return false;
    }

    private void reportUnmappedTasks(List<Task> jobTasks) {
        List<String> unmappedTasks = new ArrayList<>();
        for (Task task : jobTasks) {
            if (!isTaskAssigned(task.getTaskType())) {
                unmappedTasks.add(task.getTaskType());
            }
        }
        if (!unmappedTasks.isEmpty()) {
            System.out.println("Warning: Tasks not assigned to any station - " + unmappedTasks);
        }
    }
    private Task getNextTaskForJob(Job job, Task completedTask) {
        List<Task> tasks = job.getTasks();
        int completedTaskIndex = tasks.indexOf(completedTask);
        if (completedTaskIndex != -1 && completedTaskIndex + 1 < tasks.size()) {
            return tasks.get(completedTaskIndex + 1);
        }
        return null;
    }


    public void updateCurrentState() {
        Job job = new Job();
        if (job.isJobCompleted()) {
            job.setCurrentState(Job.JobState.COMPLETED);
        } else {
            Task nextTask = job.getNextTask();
            if (nextTask != null && nextTask.getState() == Task.TaskState.EXECUTING) {
                job.setCurrentState(Job.JobState.EXECUTING);
            } else {
                job.setCurrentState(Job.JobState.WAITING);
            }
        }
    }

    private void handleTaskCompletion(Task task, Station station) {
        System.out.println("Task Completion: " + task.getTaskType() + " at Station " + station.getStationId());
        station.completeTask(task);
        task.setState(Task.TaskState.COMPLETED);

        Job job = task.getJob();
        job.updateCurrentState();  // Update job state after task completion
        Task nextTask = job.getNextTask();

        if (nextTask != null) {
            Station nextStation = findSuitableStation(nextTask.getTaskType(), nextTask.getTaskSize());
            if (nextStation != null) {
                assignTaskToStation(nextStation, nextTask);
            } else {
                System.out.println("No suitable station found for next task " + nextTask.getTaskType());
            }
        }

        if (station.hasWaitingTasks()) {
            Task nextWaitingTask = station.getNextWaitingTask();
            assignTaskToStation(station, nextWaitingTask);
        }
    }

    protected Station findSuitableStation(String taskType, double taskSize) {
        Station bestStation = null;
        double minLoad = Double.MAX_VALUE;

        for (Station station : stations) {
            if (station.getTaskSpeeds().containsKey(taskType)) {
                if (station.canHandleTask(taskType, taskSize)) {
                    double currentLoad = station.getExecutingTasks().size() + station.getWaitingTasks().size();
                    if (currentLoad < minLoad) {
                        bestStation = station;
                        minLoad = currentLoad;
                    }
                }
            }
        }
        return bestStation;
    }

    private double getStationSpeed(Station station, String taskType) {
        if (station.getTaskSpeeds().containsKey(taskType)) {
            return station.getTaskSpeeds().get(taskType);
        }
        return 0.0;
    }

    private void simulateJob(Job job) {
        System.out.println("Simulating Job: " + job.getJobId() + " (" + job.getJobType() + ")");
        int jobStartTime = job.getStartTime();
        job.setCurrentState(Job.JobState.EXECUTING);
        List<Task> tasks = job.getTasks();
        for (Task task : tasks) {
            String taskType = task.getTaskType();
            double taskSize = task.getTaskSize();
            Station station = findSuitableStation(taskType, taskSize);
            job.setCurrentState(Job.JobState.EXECUTING);

            if (station != null) {
                station.assignTask(task);
                double stationSpeed = getStationSpeed(station, taskType);
                int taskDuration = station.calculateTaskDuration(taskSize, stationSpeed);
                System.out.println("Executing Task " + taskType + " at Station " + station.getStationId()
                        + " (Duration: " + taskDuration + " minutes)");
                try {
                    Thread.sleep(taskDuration * 1000L);
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                }
                station.completeTask(task);
                task.setCompleted(true);
                System.out.println("Task " + taskType + " completed at Station " + station.getStationId());
            } else {
                System.out.println("No suitable station found for Task " + taskType);
            }
        }
        if (job.isJobCompleted()) {
            job.setCurrentState(Job.JobState.COMPLETED);
        }

        boolean allTasksCompleted = true;
        for (Task task : tasks) {
            if (!task.isCompleted()) {
                allTasksCompleted = false;
                break;
            }
        }
        if (allTasksCompleted) {
            job.setCurrentState(Job.JobState.COMPLETED);
        }

        int jobTardiness = Math.max(0, jobStartTime - job.getDeadline());
        System.out.println("Job " + job.getJobId() + " completed at " + jobStartTime + " minutes");
        System.out.println("Job Tardiness: " + jobTardiness + " minutes");
    }

    protected void printParsedWorkflow() {
        System.out.println("Parsed Workflow Information:");
        System.out.println("=== Task Types ===");
        for (String taskType : taskTypes) {
            System.out.println("Task Type: " + taskType);
        }
        System.out.println();

        System.out.println("=== Job Types ===");
        printJobTypesWithSizes(jobParsedTaskTypesWithSize, taskTypeSizes);
        printJobTypes(jobTypes);

        System.out.println("=== Stations ===");
        for (Station station : stations) {
            System.out.println("Station ID: " + station.getStationId());
            System.out.println("Max Capacity: " + station.getMaxCapacity());
            System.out.println("Multi-Flag: " + station.isMultiFlag());
            System.out.println("FIFO-Flag: " + station.isFifoFlag());
            System.out.println("Task Speeds:");
            for (Map.Entry<String, Double> entry : station.getTaskSpeeds().entrySet()) {
                System.out.println("- Task Type: " + entry.getKey() + ", Speed: " + entry.getValue());
            }
            System.out.println();
        }
    }

    private void reportResults() {
        int totalJobTardiness = 0;
        int totalJobs = jobTypes.size();

        for (Job job : jobTypes) {
            totalJobTardiness += Math.max(0, job.getStartTime() + job.getDuration() - job.getDeadline());
        }

        double averageJobTardiness = (double) totalJobTardiness / totalJobs;
        System.out.println("Average Job Tardiness: " + averageJobTardiness);

        for (Station station : stations) {
            double stationUtilization = calculateStationUtilization(station, jobTypes);
            System.out.println("Station " + station.getStationId() + " Utilization: " + stationUtilization + "%");
        }
        for (Job job : jobTypes) {
            System.out.println("Job " + job.getJobId() + " State: " + job.getCurrentState());
        }
    }

    private double calculateStationUtilization(Station station, List<Job> jobs) {
        int totalExecutionTime = 0;
        int totalSimulationTime = 0;

        for (Job job : jobs) {
            for (Task task : job.getTasks()) {
                String taskType = task.getTaskType();
                double taskSize = task.getTaskSize();
                double stationSpeed = station.getEffectiveSpeed(taskType);
                int taskDuration = station.calculateTaskDuration(taskSize, stationSpeed);
                totalExecutionTime += taskDuration;
            }
            totalSimulationTime += job.getDuration();
        }

        if (totalSimulationTime > 0) {
            return (double) totalExecutionTime / totalSimulationTime * 100.0;
        } else {
            return 0.0;
        }
    }

    public List<Job> readJobFile(String jobFile) {
        List<Job> jobTypes = new ArrayList<>();
        int lineNumber = 0;
        Set<String> jobIDs = new HashSet<>();
        final int MAX_DEADLINE = 1000;

        try (Scanner scanner = new Scanner(new File(jobFile))) {
            while (scanner.hasNextLine()) {
                lineNumber++;
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split("\\s+");
                    if (parts.length != 4) {
                        System.err.println("Syntax error at line " + lineNumber + ": Incorrect number of elements.");
                        continue;
                    }

                    String jobID = parts[0];
                    String jobType = parts[1];
                    int startTime;
                    int duration;

                    try {
                        startTime = Integer.parseInt(parts[2]);
                        duration = Integer.parseInt(parts[3]);
                    } catch (NumberFormatException e) {
                        System.err.println("Semantic error at line " + lineNumber + ": Start time and duration must be numeric.");
                        continue;
                    }

                    if (jobIDs.contains(jobID)) {
                        System.err.println("Semantic error at line " + lineNumber + ": Duplicate job ID detected.");
                        continue;
                    }

                    if (startTime < 0 || duration < 0) {
                        System.err.println("Semantic error at line " + lineNumber + ": Start time and duration must be non-negative.");
                        continue;
                    }

                    int deadline = startTime + duration;
                    if (deadline > MAX_DEADLINE) {
                        System.err.println("Semantic error at line " + lineNumber + ": Deadline exceeds maximum allowed value.");
                        continue;
                    }

                    jobIDs.add(jobID);
                    jobTypes.add(new Job(jobID, jobType, startTime, duration));
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return jobTypes;
    }

    private void printJobTypes(List<Job> jobTypes) {
        for (Job job : jobTypes) {
            System.out.println(job);
        }
    }
}