package com.substringtechnologies.jpaadmin.service;

/** An admin action refused the request; the message is written for the person who ran it. */
public class AdminActionException extends RuntimeException {
    public AdminActionException(String message, Throwable cause) {
        super(message, cause);
    }
}
