package com.substringtechnologies.jpaadmin.security.jdbc;

import javax.sql.DataSource;
import java.sql.SQLException;

/** Database-specific bootstrap DDL; JDBC drivers remain the host application's responsibility. */
enum IdentityDatabase {
    H2("jpa-admin/migrations/V1__admin_identity.sql"),
    POSTGRESQL("jpa-admin/migrations/V1__admin_identity.sql"),
    MYSQL("jpa-admin/migrations/mysql/V1__admin_identity.sql");

    final String migration;
    IdentityDatabase(String migration) { this.migration = migration; }

    static IdentityDatabase detect(DataSource source) {
        try (var connection = source.getConnection()) {
            var metadata = connection.getMetaData();
            return switch (metadata.getDatabaseProductName()) {
                case "H2" -> H2;
                case "PostgreSQL" -> POSTGRESQL;
                case "MySQL" -> {
                    if (metadata.getDatabaseMajorVersion() < 8)
                        throw new IllegalStateException("Admin identity requires MySQL 8.0 or newer");
                    yield MYSQL;
                }
                default -> throw new IllegalStateException("Unsupported admin identity database: "
                        + metadata.getDatabaseProductName() + ". Use H2, PostgreSQL or MySQL, or provide an AdminIdentityStore.");
            };
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to detect admin identity database", exception);
        }
    }
}
