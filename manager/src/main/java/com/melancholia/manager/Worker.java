package com.melancholia.manager;

import java.util.Objects;

public class Worker {

    private final String host;
    private final int port;
    private WorkerStatus workerStatus;

    public Worker(String host, int port) {
        this.host = host;
        this.port = port;
        workerStatus = WorkerStatus.UNINITIALIZED;
    }

    public String fullAddress() {
        return String.format("http://%s:%d", host, port);
    }

    public String checkStateAddress() {
        return String.format("%s/%s", fullAddress(), "check-state");
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public WorkerStatus getWorkerStatus() {
        return workerStatus;
    }

    public void setWorkerStatus(WorkerStatus workerStatus) {
        this.workerStatus = workerStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Worker worker = (Worker) o;
        return port == worker.port && Objects.equals(host, worker.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}
