package com.substringtechnologies.jpaadmin.storage;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/** Detects a file's real type from its first bytes, so a renamed script can't pass as an image. */
public final class FileSignatures {

    private static final Map<String, String> EXTENSIONS = Map.ofEntries(
            Map.entry("image/png", ".png"), Map.entry("image/jpeg", ".jpg"), Map.entry("image/gif", ".gif"),
            Map.entry("image/webp", ".webp"), Map.entry("image/avif", ".avif"), Map.entry("image/bmp", ".bmp"),
            Map.entry("image/svg+xml", ".svg"), Map.entry("image/x-icon", ".ico"), Map.entry("application/pdf", ".pdf"),
            Map.entry("application/zip", ".zip"), Map.entry("text/csv", ".csv"), Map.entry("text/plain", ".txt"),
            Map.entry("application/json", ".json"), Map.entry("video/mp4", ".mp4"), Map.entry("audio/mpeg", ".mp3"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx"));

    private FileSignatures() {}

    /** Detected MIME type, or null when the bytes have no recognizable signature (plain text, CSV, ...). */
    public static String detect(byte[] head) {
        if (head == null || head.length < 4) return null;
        if (starts(head, 0x89, 'P', 'N', 'G')) return "image/png";
        if (starts(head, 0xFF, 0xD8, 0xFF)) return "image/jpeg";
        if (starts(head, 'G', 'I', 'F', '8')) return "image/gif";
        if (head.length >= 12 && starts(head, 'R', 'I', 'F', 'F') && ascii(head, 8, 4).equals("WEBP")) return "image/webp";
        if (head.length >= 12 && ascii(head, 4, 4).equals("ftyp")) {
            String brand = ascii(head, 8, 4);
            if (brand.startsWith("avi")) return "image/avif";
            if (brand.startsWith("hei") || brand.startsWith("mif")) return "image/heic";
            return "video/mp4";
        }
        if (starts(head, 'B', 'M')) return "image/bmp";
        if (starts(head, 0x00, 0x00, 0x01, 0x00)) return "image/x-icon";
        if (starts(head, '%', 'P', 'D', 'F')) return "application/pdf";
        if (starts(head, 'P', 'K', 0x03, 0x04)) return "application/zip";
        if (starts(head, 'I', 'D', '3') || starts(head, 0xFF, 0xFB)) return "audio/mpeg";
        if (starts(head, 'M', 'Z')) return "application/x-msdownload";
        if (starts(head, 0x7F, 'E', 'L', 'F')) return "application/x-elf";
        String text = new String(head, StandardCharsets.ISO_8859_1).stripLeading().toLowerCase(Locale.ROOT);
        if (text.startsWith("﻿")) text = text.substring(1);
        if (text.startsWith("<svg") || (text.startsWith("<?xml") && text.contains("<svg"))) return "image/svg+xml";
        if (text.startsWith("<!doctype html") || text.startsWith("<html") || text.startsWith("<script")) return "text/html";
        if (text.startsWith("<?xml")) return "application/xml";
        return null;
    }

    /**
     * Whether detected content is consistent with the declared type. Office documents are ZIP containers, and types
     * without a signature (text, CSV, JSON) are accepted only when the content isn't a recognized binary or markup type.
     */
    public static boolean matches(String declared, String detected) {
        String type = declared.toLowerCase(Locale.ROOT);
        if (detected == null) return !type.startsWith("image/") && !type.equals("application/pdf") && !type.startsWith("video/");
        if (detected.equals(type)) return true;
        if (detected.equals("application/zip") && (type.contains("openxmlformats") || type.equals("application/zip"))) return true;
        return detected.equals("video/mp4") && type.startsWith("video/");
    }

    public static String extensionFor(String contentType, String fileName) {
        String known = EXTENSIONS.get(contentType.toLowerCase(Locale.ROOT));
        if (known != null) return known;
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dot >= 0) {
            String ext = fileName.substring(dot).toLowerCase(Locale.ROOT);
            if (ext.matches("\\.[a-z0-9]{1,8}")) return ext;
        }
        return "";
    }

    private static boolean starts(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if ((bytes[i] & 0xFF) != prefix[i]) return false;
        return true;
    }

    private static String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, Math.min(length, bytes.length - offset), StandardCharsets.US_ASCII);
    }
}
