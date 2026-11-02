package com.substringtechnologies.jpaadmin.security;

import java.util.*;

/** Optional persistence adapter. Passwords supplied to this interface are already encoded. */
public interface AdminIdentityStore {
    record Account(String id, String username, String passwordHash, boolean active, boolean adminAccess,
                   boolean superuser, long version, Set<String> groupIds, Set<String> permissions) {}
    record Group(String id, String name, Set<String> permissions) {}
    Optional<Account> findByUsername(String username);
    Optional<Account> findById(String id);
    List<Account> users();
    List<Group> groups();
    void bootstrap(String username, String encodedPassword);
    Account saveUser(String id, String username, String encodedPassword, boolean active, boolean adminAccess,
                     boolean superuser, Set<String> groupIds);
    void deleteUser(String id);
    Group saveGroup(String id, String name, Set<String> permissions);
    void deleteGroup(String id);
}
