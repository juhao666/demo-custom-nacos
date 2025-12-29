package com.juhao666.demo.order.model;

public class Result {
    private boolean success;
    private String message;
    private Object data;
    private long timestamp;

    public Result(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static Result success(String message) {
        return new Result(true, message, null);
    }

    public static Result success(String message, Object data) {
        return new Result(true, message, data);
    }

    public static Result error(String message) {
        return new Result(false, message, null);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
