package com.substringtechnologies.jpaadmin.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * HMAC-signed upload tickets. A ticket binds the storage, object key, type and size the server approved, so a client
 * can't complete or upload to a key it wasn't given.
 */
public class UploadTokens {

    private final byte[] secret;
    private final ObjectMapper json = new ObjectMapper();

    /** @param secret shared secret; when blank a random one is generated, so tickets don't survive a restart or span instances */
    public UploadTokens(String secret) {
        if (secret == null || secret.isBlank()) {
            this.secret = new byte[32];
            new SecureRandom().nextBytes(this.secret);
        } else {
            this.secret = secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public record Claims(String storage, String key, String contentType, long size, long maxSize, String entity, String field, long expires) {}

    public String sign(Claims claims) {
        try {
            String body = Base64.getUrlEncoder().withoutPadding().encodeToString(json.writeValueAsBytes(Map.of(
                    "s", claims.storage(), "k", claims.key(), "t", claims.contentType(), "n", claims.size(), "m", claims.maxSize(),
                    "e", claims.entity(), "f", claims.field(), "x", claims.expires())));
            return body + "." + mac(body);
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign upload ticket", e);
        }
    }

    public Claims verify(String token) {
        if (token == null) throw new UploadException(400, "Missing upload ticket");
        int dot = token.indexOf('.');
        if (dot <= 0 || !MessageDigest.isEqual(mac(token.substring(0, dot)).getBytes(StandardCharsets.US_ASCII),
                token.substring(dot + 1).getBytes(StandardCharsets.US_ASCII))) {
            throw new UploadException(403, "Invalid upload ticket");
        }
        Map<String, Object> map;
        try {
            map = json.readValue(Base64.getUrlDecoder().decode(token.substring(0, dot)), new TypeReference<>() {});
        } catch (Exception e) {
            throw new UploadException(400, "Invalid upload ticket");
        }
        Claims claims = new Claims((String) map.get("s"), (String) map.get("k"), (String) map.get("t"), ((Number) map.get("n")).longValue(),
                ((Number) map.get("m")).longValue(), (String) map.get("e"), (String) map.get("f"), ((Number) map.get("x")).longValue());
        if (Instant.now().getEpochSecond() > claims.expires()) throw new UploadException(410, "The upload took too long. Please try again.");
        return claims;
    }

    private String mac(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(body.getBytes(StandardCharsets.US_ASCII)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", e);
        }
    }
}
