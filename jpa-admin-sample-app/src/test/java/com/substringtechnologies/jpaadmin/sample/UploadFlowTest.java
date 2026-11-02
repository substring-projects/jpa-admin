package com.substringtechnologies.jpaadmin.sample;

import com.substringtechnologies.jpaadmin.metadata.JpaMetamodelScanner;
import com.substringtechnologies.jpaadmin.storage.StorageRegistry;
import com.substringtechnologies.jpaadmin.storage.StorageSettings;
import com.substringtechnologies.jpaadmin.storage.UploadException;
import com.substringtechnologies.jpaadmin.storage.UploadService;
import com.substringtechnologies.jpaadmin.storage.UploadTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "JPA_ADMIN")
@SpringBootTest(properties = "spring.jpa.admin.security.mode=configured")
class UploadFlowTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    @Autowired
    private UploadService uploads;

    @Autowired
    private JpaMetamodelScanner scanner;

    private String tokenFromUrl(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    @Test
    void imageUploadsToLocalStorageAndReturnsAServedUrl() throws Exception {
        var prepared = uploads.prepare("Product", "imageUrl", "Espresso Front Ünïcode.PNG", "image/png", PNG.length);
        assertEquals("PUT", prepared.ticket().method());
        assertTrue(prepared.ticket().url().startsWith("/admin/api/uploads/local/"));

        uploads.receive(tokenFromUrl(prepared.ticket().url()), new ByteArrayInputStream(PNG));
        String url = uploads.complete(prepared.token(), Map.of());
        assertTrue(url.matches("/uploads/products/\\d{4}/\\d{2}/[0-9a-f]{8}-espresso-front-unicode\\.png"), url);
    }

    @Test
    void rulesFromTheAnnotationAreEnforcedBeforeUploading() {
        assertEquals(415, assertThrows(UploadException.class, () -> uploads.prepare("Product", "imageUrl", "a.pdf", "application/pdf", 10)).getStatus());
        assertEquals(415, assertThrows(UploadException.class, () -> uploads.prepare("Product", "imageUrl", "a.svg", "image/svg+xml", 10)).getStatus());
        var tooBig = assertThrows(UploadException.class, () -> uploads.prepare("Product", "imageUrl", "a.png", "image/png", 6L * 1024 * 1024));
        assertEquals(413, tooBig.getStatus());
        assertTrue(tooBig.getMessage().contains("the limit is 5 MB"), tooBig.getMessage());
        assertEquals(404, assertThrows(UploadException.class, () -> uploads.prepare("Product", "name", "a.png", "image/png", 10)).getStatus());
        assertDoesNotThrow(() -> uploads.prepare("Product", "datasheetUrl", "spec.pdf", "application/pdf", 9L * 1024 * 1024));
    }

    @Test
    void disguisedContentIsRejectedAndRemoved() throws Exception {
        byte[] html = "<!DOCTYPE html><script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);
        var prepared = uploads.prepare("Product", "imageUrl", "cat.png", "image/png", html.length);
        uploads.receive(tokenFromUrl(prepared.ticket().url()), new ByteArrayInputStream(html));
        var rejected = assertThrows(UploadException.class, () -> uploads.complete(prepared.token(), Map.of()));
        assertEquals(422, rejected.getStatus());
        // The stored file was deleted, so completing again finds nothing.
        assertEquals(404, assertThrows(UploadException.class, () -> uploads.complete(prepared.token(), Map.of())).getStatus());
    }

    @Test
    void ticketsCannotBeForged() throws Exception {
        var prepared = uploads.prepare("Product", "imageUrl", "photo.png", "image/png", PNG.length);
        String forged = prepared.token().substring(0, prepared.token().length() - 2) + "xx";
        assertEquals(403, assertThrows(UploadException.class, () -> uploads.complete(forged, Map.of())).getStatus());
        assertEquals(403, assertThrows(UploadException.class, () -> uploads.receive(forged, new ByteArrayInputStream(PNG))).getStatus());
    }

    @Test
    void fieldsNamingAnUnconfiguredStorageAreReported() {
        StorageRegistry onlyArchive = new StorageRegistry(Map.of("archive", new StorageSettings()), "/admin/api/uploads/local", List.of());
        List<String> problems = UploadService.misconfiguredFields(scanner, onlyArchive);
        assertTrue(problems.stream().anyMatch(p -> p.contains("Product.imageUrl") && p.contains("\"default\"")), problems.toString());
        assertTrue(UploadService.misconfiguredFields(scanner, new StorageRegistry(Map.of("default", new StorageSettings()), "/x", List.of())).isEmpty());
        assertNotNull(new UploadTokens(""));
    }
}
