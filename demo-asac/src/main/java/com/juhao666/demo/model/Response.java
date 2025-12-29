package com.juhao666.demo.model;

public class Response {
    public static Result success(String message) {
        return new Result(true, message, null);
    }

    public static Result success(String message, Object data) {
        return new Result(true, message, data);
    }

    public static Result error(String message) {
        return new Result(false, message, null);
    }

}
