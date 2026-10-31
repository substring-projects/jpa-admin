package com.substringtechnologies.jpaadmin.metadata;

import java.util.List;
import java.util.Locale;

/** Upload rules for a field annotated with {@code @AdminUpload}. */
public class UploadMetadata {

    private String storage;
    private String folder;
    private List<String> accept;
    private long maxSize;

    public UploadMetadata() {}

    public UploadMetadata(String storage, String folder, List<String> accept, long maxSize) {
        this.storage = storage;
        this.folder = folder;
        this.accept = accept;
        this.maxSize = maxSize;
    }

    public String getStorage() { return storage; }
    public void setStorage(String storage) { this.storage = storage; }
    public String getFolder() { return folder; }
    public void setFolder(String folder) { this.folder = folder; }
    public List<String> getAccept() { return accept; }
    public void setAccept(List<String> accept) { this.accept = accept; }
    public long getMaxSize() { return maxSize; }
    public void setMaxSize(long maxSize) { this.maxSize = maxSize; }

    /** True when every accepted type is an image, so the UI shows previews instead of file links. */
    public boolean isImage() {
        return accept != null && !accept.isEmpty() && accept.stream().allMatch(type -> type.toLowerCase(Locale.ROOT).startsWith("image/"));
    }

    /** Exact matches and {@code type/*} wildcards; SVG only when listed explicitly. */
    public boolean accepts(String contentType) {
        if (contentType == null || accept == null) return false;
        String type = contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        for (String allowed : accept) {
            String rule = allowed.toLowerCase(Locale.ROOT).trim();
            if (rule.equals(type)) return true;
            if (rule.endsWith("/*") && type.startsWith(rule.substring(0, rule.length() - 1)) && !type.equals("image/svg+xml")) return true;
        }
        return false;
    }

    /** Parses sizes such as {@code 5MB}, {@code 500 KB}, {@code 1GB} or a plain byte count. */
    public static long parseSize(String value) {
        String text = value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        long multiplier = 1;
        if (text.endsWith("KB")) { multiplier = 1024L; text = text.substring(0, text.length() - 2); }
        else if (text.endsWith("MB")) { multiplier = 1024L * 1024; text = text.substring(0, text.length() - 2); }
        else if (text.endsWith("GB")) { multiplier = 1024L * 1024 * 1024; text = text.substring(0, text.length() - 2); }
        else if (text.endsWith("B")) { text = text.substring(0, text.length() - 1); }
        try {
            long size = Math.round(Double.parseDouble(text) * multiplier);
            if (size <= 0) throw new NumberFormatException();
            return size;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid @AdminUpload maxSize \"" + value + "\"; use a value like \"5MB\"");
        }
    }
}
