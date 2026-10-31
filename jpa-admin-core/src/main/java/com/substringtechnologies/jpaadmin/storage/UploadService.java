package com.substringtechnologies.jpaadmin.storage;

import com.substringtechnologies.jpaadmin.metadata.EntityMetadata;
import com.substringtechnologies.jpaadmin.metadata.FieldMetadata;
import com.substringtechnologies.jpaadmin.metadata.JpaMetamodelScanner;
import com.substringtechnologies.jpaadmin.metadata.UploadMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Three-step upload: {@link #prepare} checks the field's rules and issues a ticket, the browser sends the file to the
 * storage, and {@link #complete} verifies the stored object (size and real content type) before returning its URL.
 */
public class UploadService {

    private static final Duration TICKET_TTL = Duration.ofMinutes(30);

    private final JpaMetamodelScanner scanner;
    private final StorageRegistry storages;
    private final UploadTokens tokens;

    public UploadService(JpaMetamodelScanner scanner, StorageRegistry storages, UploadTokens tokens) {
        this.scanner = scanner;
        this.storages = storages;
        this.tokens = tokens;
    }

    public record Prepared(String token, UploadTicket ticket) {}

    public Prepared prepare(String entityName, String fieldName, String fileName, String contentType, long size) {
        UploadMetadata rules = rules(entityName, fieldName);
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        if (size <= 0) throw new UploadException(400, "The file is empty");
        if (size > rules.getMaxSize()) {
            throw new UploadException(413, "The file is " + human(size) + "; the limit is " + human(rules.getMaxSize()));
        }
        if (!rules.accepts(type)) throw new UploadException(415, "This file type isn't allowed. Allowed: " + String.join(", ", rules.getAccept()));

        String folder = rules.getFolder().isEmpty() ? "" : rules.getFolder() + "/";
        String key = folder + YearMonth.now().toString().replace('-', '/') + "/" + UUID.randomUUID().toString().substring(0, 8)
                + readableName(fileName) + FileSignatures.extensionFor(type, fileName);
        String token = tokens.sign(new UploadTokens.Claims(rules.getStorage(), key, type, size, rules.getMaxSize(), entityName, fieldName,
                Instant.now().plus(TICKET_TTL).getEpochSecond()));
        return new Prepared(token, storages.get(rules.getStorage()).prepare(key, type, size, token));
    }

    /** Receives a file sent through the admin API (local storage). */
    public void receive(String token, InputStream content) throws IOException {
        UploadTokens.Claims claims = tokens.verify(token);
        storages.get(claims.storage()).receive(claims.key(), content, claims.maxSize());
    }

    public String complete(String token, Map<String, String> providerData) throws IOException {
        UploadTokens.Claims claims = tokens.verify(token);
        FileStorage storage = storages.get(claims.storage());
        StoredObject stored = storage.inspect(claims.key(), providerData);
        String problem = problem(claims, stored);
        if (problem != null) {
            try { storage.delete(claims.key(), providerData); } catch (IOException ignored) { /* the rejection matters more */ }
            throw new UploadException(422, problem);
        }
        return stored.url();
    }

    private String problem(UploadTokens.Claims claims, StoredObject stored) {
        if (stored.size() > claims.maxSize()) return "The file is larger than the allowed " + human(claims.maxSize());
        if (stored.head() != null) {
            String detected = FileSignatures.detect(stored.head());
            if (!FileSignatures.matches(claims.contentType(), detected)) {
                return "The file's content doesn't match its type (" + claims.contentType() + ")";
            }
        } else if (stored.contentType() != null && !stored.contentType().toLowerCase(Locale.ROOT).startsWith(claims.contentType())
                && !(claims.contentType().equals("image/jpeg") && stored.contentType().equalsIgnoreCase("image/jpg"))) {
            return "The file's content doesn't match its type (" + claims.contentType() + ")";
        }
        return null;
    }

    private UploadMetadata rules(String entityName, String fieldName) {
        EntityMetadata entity = scanner.getMetadata(entityName);
        FieldMetadata field = entity == null ? null : entity.getField(fieldName);
        if (field == null || field.getUpload() == null) throw new UploadException(404, "No upload field " + entityName + "." + fieldName);
        if (entity.isReadOnly() || field.isReadOnly()) throw new UploadException(403, field.getLabel() + " can't be changed");
        return field.getUpload();
    }

    /** "-espresso-front" from "Espresso Front.PNG": keeps keys readable while the random prefix keeps them unique. */
    static String readableName(String fileName) {
        if (fileName == null) return "";
        String base = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String slug = java.text.Normalizer.normalize(base, java.text.Normalizer.Form.NFKD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (slug.length() > 60) slug = slug.substring(0, 60).replaceAll("-+$", "");
        return slug.isEmpty() ? "" : "-" + slug;
    }

    /** Fields whose {@code @AdminUpload} names a storage that isn't configured, for a clear startup failure. */
    public static List<String> misconfiguredFields(JpaMetamodelScanner scanner, StorageRegistry storages) {
        List<String> problems = new ArrayList<>();
        for (EntityMetadata entity : scanner.getAllMetadata()) {
            for (FieldMetadata field : entity.getFields()) {
                if (field.getUpload() != null && !storages.contains(field.getUpload().getStorage())) {
                    problems.add(entity.getEntityName() + "." + field.getName() + " uses storage \"" + field.getUpload().getStorage() + "\"");
                }
            }
        }
        return problems;
    }

    static String human(long bytes) {
        if (bytes >= 1024L * 1024 * 1024) return trim(bytes / (1024.0 * 1024 * 1024)) + " GB";
        if (bytes >= 1024L * 1024) return trim(bytes / (1024.0 * 1024)) + " MB";
        if (bytes >= 1024) return trim(bytes / 1024.0) + " KB";
        return bytes + " bytes";
    }

    private static String trim(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.1f", value);
    }

    public static Map<String, Object> describe(Prepared prepared) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", prepared.token());
        body.put("method", prepared.ticket().method());
        body.put("url", prepared.ticket().url());
        body.put("headers", prepared.ticket().headers());
        body.put("fields", prepared.ticket().fields());
        body.put("fileField", prepared.ticket().fileField());
        return body;
    }
}
