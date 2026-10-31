package com.substringtechnologies.jpaadmin.storage;

/**
 * One named storage under {@code spring.jpa.admin.storage.<name>}. Only the properties of the chosen {@code type} are used.
 */
public class StorageSettings {

    /** {@code local}, {@code s3} or {@code imagekit}. */
    private String type = "local";

    // local
    /** Directory files are written to. */
    private String directory = "./uploads";
    /** URL path the directory is served from, for example {@code /uploads}. */
    private String publicPath = "/uploads";

    // s3 and S3-compatible services
    private String bucket;
    private String region = "us-east-1";
    private String accessKey;
    private String secretKey;
    /** Custom endpoint for MinIO, Cloudflare R2, DigitalOcean Spaces and similar services. */
    private String endpoint;
    /** Path-style URLs ({@code endpoint/bucket/key}); usually required by MinIO. */
    private boolean pathStyle;

    // imagekit
    private String publicKey;
    private String privateKey;
    /** For example {@code https://ik.imagekit.io/your_id}. */
    private String urlEndpoint;
    private String uploadUrl = "https://upload.imagekit.io/api/v1/files/upload";
    private String apiUrl = "https://api.imagekit.io/v1";

    /** Base URL saved for uploaded files (a CDN or custom domain). Defaults to the provider's own URL. */
    private String publicUrl;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDirectory() { return directory; }
    public void setDirectory(String directory) { this.directory = directory; }
    public String getPublicPath() { return publicPath; }
    public void setPublicPath(String publicPath) { this.publicPath = publicPath; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public boolean isPathStyle() { return pathStyle; }
    public void setPathStyle(boolean pathStyle) { this.pathStyle = pathStyle; }
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
    public String getUrlEndpoint() { return urlEndpoint; }
    public void setUrlEndpoint(String urlEndpoint) { this.urlEndpoint = urlEndpoint; }
    public String getUploadUrl() { return uploadUrl; }
    public void setUploadUrl(String uploadUrl) { this.uploadUrl = uploadUrl; }
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }

    static String trimSlashes(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }

    void require(String name, String property, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("spring.jpa.admin.storage." + name + "." + property + " is required for type " + type);
        }
    }
}
