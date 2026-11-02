package com.substringtechnologies.jpaadmin.security;
public class AdminAccessDeniedException extends RuntimeException {
    public AdminAccessDeniedException() { super("You do not have permission to perform this operation."); }
}
