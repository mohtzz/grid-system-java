package com.melancholia.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

@Service
public class ManagerService {

    private final Set<Worker> workerList = new HashSet<>();

    @Autowired
    private RestApiClient restApiClient;
    @Autowired
    private Executor executor;
    @Autowired
    private TaskScheduler taskScheduler;


    public void registerWorker(Worker worker){
        System.out.println("Добавлен новый воркер " +  worker.getHost() + ":" + worker.getPort());
        workerList.add(worker);
    }

    public void leaveWorker(Worker worker) {
        System.out.println("Воркер удален " +  worker.getHost() + ":" + worker.getPort());
        workerList.remove(worker);
    }

    public Set<Worker> getWorkers(){
        return workerList;
    }

    @Scheduled(fixedRate = 5000)
    public void checkWorkersStatus() {
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(workerList.size(), 10));

        List<Future<?>> futures = new ArrayList<>();

        for (Worker worker : workerList) {
            futures.add(executor.submit(() -> {
                String workerAddress = worker.fullAddress();
                try {
                    ApiBody apiResponse = restApiClient.getRequest(worker.checkStateAddress(), ApiBody.class);
                    worker.setWorkerStatus(WorkerStatus.valueOf(apiResponse.getData().toString()));
                } catch (Exception e) {
                    leaveWorker(worker);
                }
            }));
        }

        try {
            for (Future<?> future : futures) {
                try {
                    future.get(3, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    future.cancel(true);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
        }  finally {
            executor.shutdownNow();
        }
    }

}
