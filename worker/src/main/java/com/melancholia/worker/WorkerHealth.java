package com.melancholia.worker;

import org.springframework.stereotype.Component;

@Component
public class WorkerHealth {

    private WorkerStatus workerStatus = WorkerStatus.UNINITIALIZED;

    public WorkerStatus getWorkerStatusEnum() {
        return workerStatus;
    }

    public void setWorkerStatusEnum(WorkerStatus workerStatus) {
        this.workerStatus = workerStatus;
    }

}
