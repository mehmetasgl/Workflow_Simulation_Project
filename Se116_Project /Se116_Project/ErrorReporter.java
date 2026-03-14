import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ErrorReporter {
    private List<String> errors;

    public ErrorReporter() {
        errors = new ArrayList<>();
    }

    public void reportError(String error) {
        errors.add(error);
    }

    public void printErrors() {
        if (errors.isEmpty()) {
            System.out.println("No errors detected.");
        } else {
            System.out.println("Errors detected:");
            for (String error : errors) {
                System.out.println(error);
            }
        }
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }


    public static void main(String[] args) {
        ErrorReporter errorReporter = new ErrorReporter();

        System.out.println("Printing:");

        String jobFilePath = "/Users/ebrub/source/repos/Se116_project2/src/jobfile";
        if (!checkFileAccessibility(jobFilePath, errorReporter)) return;

        String workflowFilePath = "/Users/ebrub/source/repos/Se116_project2/src/workfile";
        if (!checkFileAccessibility(workflowFilePath, errorReporter)) return;

        WorkflowSimulator simulator = new WorkflowSimulator(workflowFilePath, jobFilePath, errorReporter);

        // Print parsed workflow information
        simulator.printParsedWorkflow();
        for (Job job : simulator.getJobTypes()) {
            addTasksToJob(job);
            assignTasksToStations(simulator, job);
        }

        // Run the simulation
        simulator.simulateWorkflow();

        // Print all errors at the end
        errorReporter.printErrors();
        System.out.println();

        // Print job states
        printJobStates(simulator.getJobTypes());

        // Print station states
        printStationStates(simulator.getStations());

    }
    private static void addTasksToJob(Job job) {
        // Adding example tasks to the job
        job.addTask(new Task("T4", 5.0, job.getDeadline(), job));
        job.addTask(new Task("T21", 3.0, job.getDeadline(), job));
        job.addTask(new Task("T4", 2.5, job.getDeadline(), job));
    }
    private static void assignTasksToStations(WorkflowSimulator simulator, Job job) {
        for (Task task : job.getTasks()) {
            Station station = simulator.findSuitableStation(task.getTaskType(), task.getTaskSize());
            if (station != null) {

                    station.assignTask(task);


                    station.assignTaskForWaiting(task);

            } else {
                System.out.println("No suitable station found for Task " + task.getTaskType());
            }
        }
    }




    private static void printJobStates(List<Job> jobs) {
        System.out.println("=== Job States ===");
        for (Job job : jobs) {
            System.out.println("Job ID: " + job.getJobId());
            System.out.println("Job Type: " + job.getJobType());
            System.out.println("Start Time: " + job.getStartTime());
            System.out.println("Duration: " + job.getDuration());
            System.out.println("Current State: " + job.getCurrentState());
            System.out.println("Tasks:");
            for (Task task : job.getTasks()) {
                System.out.println("- Task Type: " + task.getTaskType() + ", Size: " + task.getTaskSize() + ", Completed: " + task.isCompleted());
            }
            System.out.println("Deadline: " + job.getDeadline());
            System.out.println("Tardiness: " + (job.getStartTime() + job.getDuration() - job.getDeadline()));
            System.out.println();
        }
    }



    private static void printStationStates(List<Station> stations) {

        System.out.println("=== Station States ===");
        for (Station station : stations) {
            System.out.println("Station ID: " + station.getStationId());
            System.out.println("Max Capacity: " + station.getMaxCapacity());
            System.out.println("Multi-Flag: " + station.isMultiFlag());
            System.out.println("FIFO-Flag: " + station.isFifoFlag());
            System.out.println("Task Speeds:");
            for (Map.Entry<String, Double> entry : station.getTaskSpeeds().entrySet()) {
                System.out.println("- Task Type: " + entry.getKey() + ", Speed: " + entry.getValue());
            }
            System.out.println("Executing tasks:");
            for (Task task : station.getExecutingTasks()) {
                System.out.println("- Executing Task: " + task.getTaskType() + " (Size: " + task.getTaskSize() + ")");
            }
            System.out.println("Waiting tasks:");


            for (Task waitingTask : station.getWaitingTasks()) {
                System.out.println("- Waiting Task: " + waitingTask.getTaskType() + " (Size: " + waitingTask.getTaskSize() + ")");

                if (station.isIdle()) {
                    System.out.println("Station is idle.");
                }
            }
            System.out.println();


        }
    }

    private static boolean checkFileAccessibility(String filePath, ErrorReporter errorReporter) {
        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            errorReporter.reportError("The file does not exist or is not accessible: " + filePath);
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            errorReporter.reportError("An error occurred while reading the file: " + e.getMessage());
            return false;
        }

        return true;
    }

}