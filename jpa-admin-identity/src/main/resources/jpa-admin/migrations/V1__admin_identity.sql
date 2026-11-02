CREATE TABLE IF NOT EXISTS jpa_admin_identity_lock (id INTEGER PRIMARY KEY);
CREATE TABLE IF NOT EXISTS jpa_admin_users (
 id VARCHAR(36) PRIMARY KEY, username VARCHAR(100) NOT NULL UNIQUE,
 password_hash VARCHAR(255) NOT NULL, active INTEGER NOT NULL,
 admin_access INTEGER NOT NULL, superuser INTEGER NOT NULL, version BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS jpa_admin_groups (id VARCHAR(36) PRIMARY KEY, name VARCHAR(100) NOT NULL UNIQUE);
CREATE TABLE IF NOT EXISTS jpa_admin_user_groups (
 user_id VARCHAR(36) NOT NULL REFERENCES jpa_admin_users(id),
 group_id VARCHAR(36) NOT NULL REFERENCES jpa_admin_groups(id), PRIMARY KEY(user_id, group_id)
);
CREATE TABLE IF NOT EXISTS jpa_admin_group_permissions (
 group_id VARCHAR(36) NOT NULL REFERENCES jpa_admin_groups(id),
 permission_key VARCHAR(512) NOT NULL, PRIMARY KEY(group_id, permission_key)
);
