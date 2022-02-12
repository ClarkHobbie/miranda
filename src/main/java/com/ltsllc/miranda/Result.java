package com.ltsllc.miranda;


public class Result {
    public static final int STATUS_FAILED = 400;
    public static final int STATUS_SUCCESS = 200;

    protected int statusCode;
    public int getStatus() {
        return statusCode;
    }

    public void setStatus (int status) {
        statusCode = status;
    }
}
