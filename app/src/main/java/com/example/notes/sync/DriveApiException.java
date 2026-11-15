package com.example.notes.sync;

import java.io.IOException;

public class DriveApiException extends IOException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public DriveApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isAuthError() {
        return statusCode == 401 || statusCode == 403;
    }
}
