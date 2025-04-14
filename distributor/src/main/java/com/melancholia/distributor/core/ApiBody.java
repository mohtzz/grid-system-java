package com.melancholia.distributor.core;

public class ApiBody {

    private String message;
    private int statusCode;
    private Object data;

    public ApiBody(String message, int statusCode, Object data) {
        this.message = message;
        this.statusCode = statusCode;
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Object getData() {
        return data;
    }

}
