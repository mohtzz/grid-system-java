package com.melancholia.distributor.dto;

import com.melancholia.distributor.enums.WorkerStatusEnum;

import java.util.Objects;

public class Worker {

    private String host;
    private int port;
    private WorkerStatusEnum workerStatus;
    private static final String INIT_ENDPOINT = "init";
    private static final String SOLVE_ENDPOINT = "solve";
    private static final String RESET_ENDPOINT = "reset";

    public Worker() {}

    public Worker(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String fullAddress() {
        return String.format("http://%s:%d", host, port);
    }

    public String initAddress() {
        return String.format("%s/%s", fullAddress(), INIT_ENDPOINT);
    }
    public String solveAddress() {
        return String.format("%s/%s", fullAddress(), SOLVE_ENDPOINT);
    }
    public String resetAddress() {
        return String.format("%s/%s", fullAddress(), RESET_ENDPOINT);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public WorkerStatusEnum getWorkerStatus() {
        return workerStatus;
    }

    public void setWorkerStatus(WorkerStatusEnum workerStatus) {
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
