package com.substringtechnologies.jpaadmin.web;

import com.substringtechnologies.jpaadmin.storage.UploadException;
import com.substringtechnologies.jpaadmin.storage.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("${spring.jpa.admin.path:/admin}/api/uploads")
public class AdminUploadController {

    private final UploadService uploads;

    public AdminUploadController(UploadService uploads) {
        this.uploads = uploads;
    }

    public record PrepareRequest(String entity, String field, String fileName, String contentType, long size) {}

    public record CompleteRequest(String token, Map<String, String> providerData) {}

    /** Step 1: checks type and size against the field's {@code @AdminUpload} rules and returns where to send the file. */
    @PostMapping("/prepare")
    public Map<String, Object> prepare(@RequestBody PrepareRequest request) {
        return UploadService.describe(uploads.prepare(request.entity(), request.field(), request.fileName(), request.contentType(), request.size()));
    }

    /** Step 2 for local storage: the raw file body, streamed to disk with the size limit enforced. */
    @PutMapping("/local/{token}")
    public ResponseEntity<Void> receive(@PathVariable String token, HttpServletRequest request) throws IOException {
        uploads.receive(token, request.getInputStream());
        return ResponseEntity.noContent().build();
    }

    /** Step 3: verifies the stored file and returns the URL to save on the record. */
    @PostMapping("/complete")
    public Map<String, Object> complete(@RequestBody CompleteRequest request) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("url", uploads.complete(request.token(), request.providerData() == null ? Map.of() : request.providerData()));
        return body;
    }

    @ExceptionHandler(UploadException.class)
    public ResponseEntity<Map<String, String>> handleUpload(UploadException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleStorage(IOException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "The file storage couldn't be reached. Please try again."));
    }
}
