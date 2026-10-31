package com.substringtechnologies.jpaadmin.storage;

/** An upload problem the user can act on; the message is shown in the UI. */
public class UploadException extends RuntimeException {

    private final int status;

    public UploadException(int status, String message) {
        super(message);
        this.status = status;
    }

    public UploadException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int getStatus() { return status; }
}
