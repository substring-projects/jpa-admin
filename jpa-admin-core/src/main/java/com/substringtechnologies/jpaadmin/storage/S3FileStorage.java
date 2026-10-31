package com.substringtechnologies.jpaadmin.storage;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Amazon S3 and S3-compatible services (MinIO, Cloudflare R2, DigitalOcean Spaces). The browser PUTs to a presigned
 * URL; the bucket needs a CORS rule allowing {@code PUT} from the admin's origin.
 * <p>
 * Requires {@code software.amazon.awssdk:s3} on the classpath. Without keys, the default AWS credential chain is used.
 */
public class S3FileStorage implements FileStorage {

    private static final Duration PRESIGN_TTL = Duration.ofMinutes(15);

    private final StorageSettings settings;
    private final S3Client client;
    private final S3Presigner presigner;

    public S3FileStorage(String name, StorageSettings settings) {
        settings.require(name, "bucket", settings.getBucket());
        this.settings = settings;
        AwsCredentialsProvider credentials = settings.getAccessKey() != null && !settings.getAccessKey().isBlank()
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(settings.getAccessKey(), settings.getSecretKey()))
                : DefaultCredentialsProvider.builder().build();
        S3Configuration s3Config = S3Configuration.builder().pathStyleAccessEnabled(settings.isPathStyle()).build();
        Region region = Region.of(settings.getRegion());

        var clientBuilder = S3Client.builder().region(region).credentialsProvider(credentials).serviceConfiguration(s3Config);
        var presignerBuilder = S3Presigner.builder().region(region).credentialsProvider(credentials).serviceConfiguration(s3Config);
        if (settings.getEndpoint() != null && !settings.getEndpoint().isBlank()) {
            clientBuilder.endpointOverride(URI.create(settings.getEndpoint()));
            presignerBuilder.endpointOverride(URI.create(settings.getEndpoint()));
        }
        this.client = clientBuilder.build();
        this.presigner = presignerBuilder.build();
    }

    @Override
    public String type() { return "s3"; }

    @Override
    public UploadTicket prepare(String key, String contentType, long size, String localTicket) {
        var presigned = presigner.presignPutObject(request -> request
                .signatureDuration(PRESIGN_TTL)
                .putObjectRequest(put -> put.bucket(settings.getBucket()).key(key).contentType(contentType).contentLength(size)));
        Map<String, String> headers = new LinkedHashMap<>();
        presigned.signedHeaders().forEach((header, values) -> {
            // The browser sets Host and Content-Length itself and refuses to send them from script.
            if (!header.equalsIgnoreCase("host") && !header.equalsIgnoreCase("content-length") && !values.isEmpty()) {
                headers.put(header, values.get(0));
            }
        });
        headers.putIfAbsent("Content-Type", contentType);
        return UploadTicket.put(presigned.url().toString(), headers);
    }

    @Override
    public StoredObject inspect(String key, Map<String, String> providerData) throws IOException {
        try {
            HeadObjectResponse head = client.headObject(request -> request.bucket(settings.getBucket()).key(key));
            ResponseBytes<GetObjectResponse> bytes = client.getObjectAsBytes(request -> request.bucket(settings.getBucket()).key(key).range("bytes=0-63"));
            return new StoredObject(head.contentLength(), head.contentType(), bytes.asByteArray(), publicUrl(key));
        } catch (NoSuchKeyException e) {
            throw new UploadException(404, "The uploaded file was not found in the bucket", e);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) throw new UploadException(404, "The uploaded file was not found in the bucket", e);
            throw new IOException("S3 request failed: " + e.awsErrorDetails().errorMessage(), e);
        } catch (SdkException e) {
            throw new IOException("S3 request failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String key, Map<String, String> providerData) throws IOException {
        try {
            client.deleteObject(request -> request.bucket(settings.getBucket()).key(key));
        } catch (SdkException e) {
            throw new IOException("S3 delete failed: " + e.getMessage(), e);
        }
    }

    String publicUrl(String key) {
        if (settings.getPublicUrl() != null && !settings.getPublicUrl().isBlank()) {
            return StorageSettings.trimSlashes(settings.getPublicUrl()) + "/" + key;
        }
        if (settings.getEndpoint() != null && !settings.getEndpoint().isBlank()) {
            String endpoint = StorageSettings.trimSlashes(settings.getEndpoint());
            if (settings.isPathStyle()) return endpoint + "/" + settings.getBucket() + "/" + key;
            URI uri = URI.create(endpoint);
            return uri.getScheme() + "://" + settings.getBucket() + "." + uri.getAuthority() + "/" + key;
        }
        return "https://" + settings.getBucket() + ".s3." + settings.getRegion() + ".amazonaws.com/" + key;
    }
}
