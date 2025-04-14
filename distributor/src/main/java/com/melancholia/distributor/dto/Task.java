package com.melancholia.distributor.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public class Task {

    private String start;
    private String end;
    private String callbackUrl;
    @JsonIgnore
    private String worker;

    public Task() {}

    public Task(String start, String end, String callbackUrl) {
        this.start = start;
        this.end = end;
        this.callbackUrl = callbackUrl;
        this.worker = null;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public String getWorker() {
        return worker;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public void setWorker(String worker) {
        this.worker = worker;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(start, task.start) && Objects.equals(end, task.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

}