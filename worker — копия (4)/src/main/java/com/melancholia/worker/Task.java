package com.melancholia.worker;

public class Task {

    private final String start;
    private final String end;
    private final String callbackUrl;

    public Task(String start, String count, String callbackUrl) {
        this.start = start;
        this.end = count;
        this.callbackUrl = callbackUrl;
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

}
