package com.juhao666.demo.model;

public class Result {
    private boolean success;
    private String message;
    private Object data;
    private long timestamp;

    public Result() {
    }

    public Result(String message) {
        this.message = message;
        this.data = null;
        this.timestamp = System.currentTimeMillis();
    }

    public Result(boolean success, String message, Object data) {
        this.message = message;
        this.data = data;
        this.success = success;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
