package com.melancholia.worker;

import java.util.Objects;

public class Worker {

    private String host;
    private int port;

    public Worker(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String fullAddress() {
        return String.format("http://%s:%d", host, port);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
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
