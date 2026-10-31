package com.substringtechnologies.jpaadmin.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;

/** Stores files in a directory on the server; the browser uploads through the admin API. */
public class LocalFileStorage implements FileStorage {

    private final Path root;
    private final String publicPath;
    private final String uploadEndpoint;

    /**
     * @param uploadEndpoint admin API URL that receives uploads, for example {@code /admin/api/uploads/local}
     */
    public LocalFileStorage(StorageSettings settings, String uploadEndpoint) {
        this.root = Path.of(settings.getDirectory()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create upload directory " + root, e);
        }
        this.publicPath = StorageSettings.trimSlashes(settings.getPublicPath());
        this.uploadEndpoint = uploadEndpoint;
    }

    public Path root() { return root; }
    public String publicPath() { return publicPath; }

    @Override
    public String type() { return "local"; }

    @Override
    public UploadTicket prepare(String key, String contentType, long size, String localTicket) {
        return UploadTicket.put(uploadEndpoint + "/" + localTicket, Map.of("Content-Type", contentType));
    }

    @Override
    public void receive(String key, InputStream content, long maxSize) throws IOException {
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), ".upload-", ".part");
        try {
            long written = 0;
            byte[] buffer = new byte[64 * 1024];
            try (OutputStream out = Files.newOutputStream(temp)) {
                for (int read; (read = content.read(buffer)) != -1; ) {
                    written += read;
                    if (written > maxSize) throw new UploadException(413, "The file is larger than the allowed size");
                    out.write(buffer, 0, read);
                }
            }
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    @Override
    public StoredObject inspect(String key, Map<String, String> providerData) throws IOException {
        Path file = resolve(key);
        if (!Files.isRegularFile(file)) throw new UploadException(404, "The uploaded file was not found");
        byte[] head;
        try (InputStream in = Files.newInputStream(file)) {
            head = in.readNBytes(64);
        }
        return new StoredObject(Files.size(file), URLConnection.guessContentTypeFromName(file.getFileName().toString()),
                Arrays.copyOf(head, head.length), publicPath + "/" + key);
    }

    @Override
    public void delete(String key, Map<String, String> providerData) throws IOException {
        Files.deleteIfExists(resolve(key));
    }

    private Path resolve(String key) {
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) throw new UploadException(400, "Invalid file path");
        return resolved;
    }
}
