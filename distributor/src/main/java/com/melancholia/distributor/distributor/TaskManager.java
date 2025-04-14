package com.melancholia.distributor.distributor;

import com.melancholia.distributor.dto.Task;
import com.melancholia.distributor.dto.Worker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class TaskManager {

    @Value("${task-manager.subtask-size}")
    private int subtaskSize;
    @Value("${task-manager.subtasks-count}")
    private int subtasksCount;
    @Autowired
    private String callbackUrl;
    private List<Task> tasks = new ArrayList<>();
    private BigInteger start = BigInteger.ZERO;
    private BigInteger finalEnd;

    public BigInteger getFinalEnd() {
        return finalEnd;
    }

    public void setFinalEnd(String finalEnd) {
        System.out.println("Конечная точка установлена: " + finalEnd);
        this.finalEnd = new BigInteger(finalEnd);
    }

    public Task getTask() {
        Optional<Task> task = tasks.stream().filter(taskDTO -> taskDTO.getWorker() == null).findFirst();
        if (task.isPresent()) return task.get();
        if (start.equals(finalEnd)) return null;

        generateTasks();
        return getTask();
    }

    private void generateTasks() {
        System.out.println("Генерация новых задач");

        for (int i = 0; i < subtasksCount; i++) {

            if (start.compareTo(finalEnd) >= 0) {
                break;
            }

            BigInteger end = start.add(BigInteger.valueOf(subtaskSize));
            if (end.compareTo(finalEnd) > 0) {
                end = finalEnd;
            }

            tasks.add(new Task(
                    start.toString(),
                    end.toString(),
                    callbackUrl
            ));

            start = end;

            if (start.compareTo(finalEnd) >= 0) {
                break;
            }
        }
    }

    public void removeCompetedTask(Task task) {
        System.out.println("Задача выполнена " + task.getStart() + ":" + task.getEnd());
        tasks.remove(task);
    }

    public void taskRedistribution(List<Worker> worker) {
        if (tasks.isEmpty()) return;
        List<String> workersAddresses = worker.stream()
                .map(Worker::fullAddress).toList();

        for (Task task : tasks.stream().filter(task -> task.getWorker() != null).toList()) {
            if (workersAddresses.contains(task.getWorker())) continue;
            System.out.println("Воркер забросил свою задачу " +  task.getWorker());
            task.setWorker(null);
        }
    }

    public void reset() {
        start = BigInteger.ZERO;
        tasks = new ArrayList<>();
    }

}
