package com.substringtechnologies.jpaadmin.storage;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class StorageProvidersTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    private HttpServer server;
    private final List<String> requests = new CopyOnWriteArrayList<>();

    @AfterEach
    void stop() { if (server != null) server.stop(0); }

    private String startServer(Map<String, byte[]> responses, Map<String, String> contentTypes) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            requests.add(exchange.getRequestMethod() + " " + path + " " + exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = responses.get(path);
            if (body == null) { exchange.sendResponseHeaders(exchange.getRequestMethod().equals("DELETE") ? 204 : 404, -1); exchange.close(); return; }
            exchange.getResponseHeaders().set("Content-Type", contentTypes.getOrDefault(path, "application/octet-stream"));
            if (exchange.getRequestMethod().equals("HEAD")) {
                exchange.getResponseHeaders().set("Content-Length", String.valueOf(body.length));
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(exchange.getRequestHeaders().containsKey("Range") ? 206 : 200, body.length);
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void localStorageWritesWithinItsDirectoryAndEnforcesTheLimit(@TempDir Path dir) throws Exception {
        StorageSettings settings = new StorageSettings();
        settings.setDirectory(dir.toString());
        LocalFileStorage local = new LocalFileStorage(settings, "/admin/api/uploads/local");

        UploadTicket ticket = local.prepare("products/2026/10/a.png", "image/png", PNG.length, "TOKEN");
        assertEquals("PUT", ticket.method());
        assertEquals("/admin/api/uploads/local/TOKEN", ticket.url());

        local.receive("products/2026/10/a.png", new ByteArrayInputStream(PNG), 1024);
        StoredObject stored = local.inspect("products/2026/10/a.png", Map.of());
        assertEquals(PNG.length, stored.size());
        assertEquals("/uploads/products/2026/10/a.png", stored.url());
        assertEquals("image/png", FileSignatures.detect(stored.head()));

        var tooBig = assertThrows(UploadException.class, () -> local.receive("b.png", new ByteArrayInputStream(new byte[2048]), 1024));
        assertEquals(413, tooBig.getStatus());
        assertFalse(Files.exists(dir.resolve("b.png")));
        try (var leftovers = Files.list(dir)) {
            assertTrue(leftovers.noneMatch(p -> p.getFileName().toString().endsWith(".part")), "temporary file removed");
        }
        assertThrows(UploadException.class, () -> local.receive("../escape.png", new ByteArrayInputStream(PNG), 1024));
    }

    @Test
    void imageKitSignsClientUploadsAndVerifiesThroughItsApi() throws Exception {
        String key = "avatars/2026/10/x.png";
        String base = startServer(Map.of(
                "/v1/files/abc123/details", ("{\"filePath\":\"/" + key + "\",\"size\":16,\"mime\":\"image/png\",\"url\":\"URL\"}").getBytes(StandardCharsets.UTF_8)),
                Map.of());
        StorageSettings settings = new StorageSettings();
        settings.setType("imagekit");
        settings.setPublicKey("public_test");
        settings.setPrivateKey("private_test");
        settings.setUrlEndpoint("https://ik.imagekit.io/demo");
        settings.setApiUrl(base + "/v1");
        ImageKitFileStorage imageKit = new ImageKitFileStorage("avatars", settings);

        UploadTicket ticket = imageKit.prepare(key, "image/png", 16, null);
        assertEquals("POST", ticket.method());
        assertEquals("https://upload.imagekit.io/api/v1/files/upload", ticket.url());
        assertEquals("file", ticket.fileField());
        assertEquals("x.png", ticket.fields().get("fileName"));
        assertEquals("/avatars/2026/10", ticket.fields().get("folder"));
        assertEquals("false", ticket.fields().get("useUniqueFileName"));
        assertEquals(imageKit.sign(ticket.fields().get("token") + ticket.fields().get("expire")), ticket.fields().get("signature"));
        assertFalse(ticket.fields().containsValue("private_test"), "the private key never reaches the browser");

        StoredObject stored = imageKit.inspect(key, Map.of("fileId", "abc123"));
        assertEquals(16, stored.size());
        assertEquals("image/png", stored.contentType());
        assertTrue(requests.get(0).contains("Basic "), "API calls authenticate with the private key");

        assertThrows(UploadException.class, () -> imageKit.inspect("someone/else.png", Map.of("fileId", "abc123")));
        assertThrows(UploadException.class, () -> imageKit.inspect(key, Map.of("fileId", "../../etc")));
    }

    @Test
    void s3PresignsSizeAndTypeAndInspectsTheStoredObject() throws Exception {
        String base = startServer(Map.of("/bucket/products/p.png", PNG), Map.of("/bucket/products/p.png", "image/png"));
        StorageSettings settings = new StorageSettings();
        settings.setType("s3");
        settings.setBucket("bucket");
        settings.setRegion("ap-south-1");
        settings.setAccessKey("AKIATEST");
        settings.setSecretKey("secret");
        settings.setEndpoint(base);
        settings.setPathStyle(true);
        S3FileStorage s3 = new S3FileStorage("default", settings);

        UploadTicket ticket = s3.prepare("products/p.png", "image/png", PNG.length, null);
        assertEquals("PUT", ticket.method());
        assertTrue(ticket.url().startsWith(base + "/bucket/products/p.png?"), ticket.url());
        assertTrue(ticket.url().contains("X-Amz-Signature="));
        assertTrue(ticket.url().toLowerCase().contains("content-length%3bcontent-type") || ticket.url().toLowerCase().contains("content-length;content-type"),
                "size and type are part of the signature: " + ticket.url());
        assertEquals("image/png", ticket.headers().get("Content-Type"));
        assertFalse(ticket.headers().keySet().stream().anyMatch(h -> h.equalsIgnoreCase("host") || h.equalsIgnoreCase("content-length")));

        StoredObject stored = s3.inspect("products/p.png", Map.of());
        assertEquals(PNG.length, stored.size());
        assertEquals("image/png", FileSignatures.detect(stored.head()));
        assertEquals(base + "/bucket/products/p.png", stored.url());

        settings.setPublicUrl("https://cdn.example.com/");
        assertEquals("https://cdn.example.com/products/p.png", new S3FileStorage("default", settings).publicUrl("products/p.png"));
        assertThrows(UploadException.class, () -> s3.inspect("missing.png", Map.of()));
    }

    @Test
    void contentDetectionCatchesDisguisedFiles() {
        assertEquals("image/png", FileSignatures.detect(PNG));
        assertEquals("text/html", FileSignatures.detect("<!DOCTYPE html><script>".getBytes(StandardCharsets.UTF_8)));
        assertEquals("image/svg+xml", FileSignatures.detect("<?xml version=\"1.0\"?><svg>".getBytes(StandardCharsets.UTF_8)));
        assertEquals("application/pdf", FileSignatures.detect("%PDF-1.7".getBytes(StandardCharsets.UTF_8)));
        assertTrue(FileSignatures.matches("image/png", "image/png"));
        assertFalse(FileSignatures.matches("image/png", "text/html"));
        assertFalse(FileSignatures.matches("image/png", null));
        assertTrue(FileSignatures.matches("text/csv", null));
        assertFalse(FileSignatures.matches("text/csv", "text/html"));
        assertTrue(FileSignatures.matches("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/zip"));
        assertEquals(".jpg", FileSignatures.extensionFor("image/jpeg", "Photo.JPEG"));
        assertEquals("", FileSignatures.extensionFor("application/octet-stream", "evil.sh;rm"));
    }

    @Test
    void unknownProvidersAndMissingSettingsFailClearly() {
        StorageSettings bad = new StorageSettings();
        bad.setType("dropbox");
        var unknown = assertThrows(IllegalStateException.class, () -> new StorageRegistry(Map.of("files", bad), "/admin/api/uploads/local", List.of()));
        assertTrue(unknown.getMessage().contains("spring.jpa.admin.storage.files.type"));

        StorageSettings imageKit = new StorageSettings();
        imageKit.setType("imagekit");
        var missing = assertThrows(IllegalStateException.class, () -> new StorageRegistry(Map.of("avatars", imageKit), "/x", List.of()));
        assertTrue(missing.getMessage().contains("spring.jpa.admin.storage.avatars.public-key"));
    }
}
