package com.substringtechnologies.jpaadmin.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * A place uploaded files are written to. Browsers upload directly to the storage using the {@link UploadTicket} it
 * issues, then the server {@link #inspect inspects} the stored object before the URL is accepted.
 * <p>
 * Applications can add their own provider (Cloudinary, Azure Blob, ...) by declaring a {@code FileStorage} bean whose
 * {@link #type()} is referenced by {@code spring.jpa.admin.storage.<name>.type}.
 */
public interface FileStorage {

    String type();

    /**
     * How the browser should send the file.
     *
     * @param key         object key, e.g. {@code products/2026/10/3f2c….png}
     * @param localTicket signed token for providers that receive uploads through the admin API
     */
    UploadTicket prepare(String key, String contentType, long size, String localTicket);

    /** Size, type and first bytes of an uploaded object; {@code providerData} carries values such as ImageKit's fileId. */
    StoredObject inspect(String key, Map<String, String> providerData) throws IOException;

    /** Removes an object that failed verification. */
    void delete(String key, Map<String, String> providerData) throws IOException;

    /** Writes a file sent through the admin API. Only providers whose tickets target the API implement this. */
    default void receive(String key, InputStream content, long maxSize) throws IOException {
        throw new UnsupportedOperationException(type() + " storage receives uploads directly");
    }
}
