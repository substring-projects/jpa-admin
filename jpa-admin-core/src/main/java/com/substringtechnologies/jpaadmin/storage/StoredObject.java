package com.substringtechnologies.jpaadmin.storage;

/**
 * What a storage reports about an uploaded object.
 *
 * @param size        bytes stored
 * @param contentType type the storage recorded (may be null)
 * @param head        leading bytes used to detect the real file type, or null when the provider can't read them
 * @param url         URL saved on the entity
 */
public record StoredObject(long size, String contentType, byte[] head, String url) {}
