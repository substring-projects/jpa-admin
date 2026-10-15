package io.github.jpaadmin.metadata;

public enum FieldType {
    STRING,
    NUMBER,
    BOOLEAN,
    DATE,
    DATE_TIME,
    ENUM,
    RELATION_MANY_TO_ONE,
    RELATION_ONE_TO_ONE,
    RELATION_ONE_TO_MANY,
    RELATION_MANY_TO_MANY,
    EMBEDDED,
    UNKNOWN
}
