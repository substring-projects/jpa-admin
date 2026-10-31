package com.substringtechnologies.jpaadmin.storage;

import java.util.Map;

/**
 * Instructions for the browser upload.
 *
 * @param method    {@code PUT} sends the raw file as the body; {@code POST} sends multipart form data
 * @param url       destination; same-origin URLs also receive the admin CSRF header
 * @param headers   request headers that must be sent unchanged (they may be part of a signature)
 * @param fields    multipart form fields sent before the file (POST only)
 * @param fileField multipart field name for the file (POST only)
 */
public record UploadTicket(String method, String url, Map<String, String> headers, Map<String, String> fields, String fileField) {

    public static UploadTicket put(String url, Map<String, String> headers) {
        return new UploadTicket("PUT", url, headers, Map.of(), null);
    }

    public static UploadTicket multipart(String url, Map<String, String> fields, String fileField) {
        return new UploadTicket("POST", url, Map.of(), fields, fileField);
    }
}
