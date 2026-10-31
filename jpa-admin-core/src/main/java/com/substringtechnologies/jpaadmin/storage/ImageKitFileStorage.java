package com.substringtechnologies.jpaadmin.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ImageKit client-side uploads: the browser posts straight to ImageKit with a short-lived signature, and the server
 * confirms the result through ImageKit's API (which also reports the type ImageKit detected from the content).
 */
public class ImageKitFileStorage implements FileStorage {

    private static final Duration SIGNATURE_TTL = Duration.ofMinutes(30);

    private final StorageSettings settings;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();

    public ImageKitFileStorage(String name, StorageSettings settings) {
        settings.require(name, "public-key", settings.getPublicKey());
        settings.require(name, "private-key", settings.getPrivateKey());
        settings.require(name, "url-endpoint", settings.getUrlEndpoint());
        this.settings = settings;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    @Override
    public String type() { return "imagekit"; }

    @Override
    public UploadTicket prepare(String key, String contentType, long size, String localTicket) {
        String token = UUID.randomUUID().toString();
        String expire = String.valueOf(Instant.now().plus(SIGNATURE_TTL).getEpochSecond());
        int slash = key.lastIndexOf('/');
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("publicKey", settings.getPublicKey());
        fields.put("signature", sign(token + expire));
        fields.put("expire", expire);
        fields.put("token", token);
        fields.put("fileName", key.substring(slash + 1));
        fields.put("folder", "/" + (slash > 0 ? key.substring(0, slash) : ""));
        fields.put("useUniqueFileName", "false");
        return UploadTicket.multipart(settings.getUploadUrl(), fields, "file");
    }

    @Override
    public StoredObject inspect(String key, Map<String, String> providerData) throws IOException {
        JsonNode details = details(providerData);
        String filePath = details.path("filePath").asText("");
        if (!filePath.replaceAll("^/+", "").equals(key)) {
            throw new UploadException(400, "The uploaded file doesn't match the requested upload");
        }
        String url = details.path("url").asText(StorageSettings.trimSlashes(settings.getUrlEndpoint()) + "/" + key);
        if (settings.getPublicUrl() != null && !settings.getPublicUrl().isBlank()) {
            url = StorageSettings.trimSlashes(settings.getPublicUrl()) + "/" + key;
        }
        return new StoredObject(details.path("size").asLong(-1), details.path("mime").asText(null), readHead(details.path("url").asText(null)), url);
    }

    @Override
    public void delete(String key, Map<String, String> providerData) throws IOException {
        String fileId = fileId(providerData);
        send(HttpRequest.newBuilder(URI.create(StorageSettings.trimSlashes(settings.getApiUrl()) + "/files/" + fileId)).DELETE());
    }

    String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(settings.getPrivateKey().getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA1 is unavailable", e);
        }
    }

    private JsonNode details(Map<String, String> providerData) throws IOException {
        String body = send(HttpRequest.newBuilder(URI.create(StorageSettings.trimSlashes(settings.getApiUrl()) + "/files/" + fileId(providerData) + "/details")).GET());
        return json.readTree(body);
    }

    private static String fileId(Map<String, String> providerData) {
        String fileId = providerData == null ? null : providerData.get("fileId");
        if (fileId == null || !fileId.matches("[A-Za-z0-9_-]{1,64}")) throw new UploadException(400, "Missing ImageKit fileId");
        return fileId;
    }

    private String send(HttpRequest.Builder request) throws IOException {
        String auth = Base64.getEncoder().encodeToString((settings.getPrivateKey() + ":").getBytes(StandardCharsets.UTF_8));
        try {
            HttpResponse<String> response = http.send(request.header("Authorization", "Basic " + auth).timeout(Duration.ofSeconds(20)).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) throw new UploadException(404, "The uploaded file was not found in ImageKit");
            if (response.statusCode() >= 300) throw new UploadException(502, "ImageKit returned HTTP " + response.statusCode());
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while contacting ImageKit", e);
        }
    }

    /** Reads leading bytes for type detection; private files may refuse this, in which case ImageKit's detected type is used. */
    private byte[] readHead(String url) {
        if (url == null || !url.startsWith("https://") && !url.startsWith("http://")) return null;
        try {
            HttpResponse<byte[]> response = http.send(HttpRequest.newBuilder(URI.create(url)).header("Range", "bytes=0-63")
                    .timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 && response.statusCode() != 206) return null;
            byte[] body = response.body();
            return body.length > 64 ? java.util.Arrays.copyOf(body, 64) : body;
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
