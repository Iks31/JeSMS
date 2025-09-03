package com.github.Iks31.messagingapp.server.db;

import java.util.ArrayList;

public class DBResult<T>{
    private boolean success;
    private String message;
    private ArrayList<T> result = null;
    private Exception exception = null;

    public DBResult(boolean success, String message, ArrayList<T> result) {
        this.success = success;
        this.message = message;
        this.result = result;
    }
    public DBResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    public DBResult(boolean success, Exception exception) {
        this.success = success;
        this.exception = exception;
        message = "There is an internal error. Please try again.";
    }
    public boolean isSuccess() {
        return success;
    }
    public String getMessage() {
        return message;
    }
    public ArrayList<T> getResult() {
        return result;
    }
    public Exception getException() {
        return exception;
    }
}
