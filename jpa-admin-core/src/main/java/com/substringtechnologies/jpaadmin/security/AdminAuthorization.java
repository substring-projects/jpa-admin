package com.substringtechnologies.jpaadmin.security;

/** Current request identity. Implementations must resolve current grants, not stale login-time authorities. */
public interface AdminAuthorization {
    boolean canEnterAdmin();
    boolean isSuperuser();
    String subject();
    boolean isAllowed(String resource, String operation);
    default void require(String resource, String operation) {
        if (!canEnterAdmin() || !isAllowed(resource, operation)) throw new AdminAccessDeniedException();
    }
    default void requireSuperuser() {
        if (!canEnterAdmin() || !isSuperuser()) throw new AdminAccessDeniedException();
    }
}
