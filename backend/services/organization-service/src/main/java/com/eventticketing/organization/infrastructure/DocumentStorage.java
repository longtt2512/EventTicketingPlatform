package com.eventticketing.organization.infrastructure;

import com.eventticketing.organization.api.ApiException;
import com.eventticketing.platform.SecretCipher;
import io.minio.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Private, encrypted objects; public/presigned document URLs are never returned. */
@Component
public class DocumentStorage {
    private final MinioClient client;
    private final String bucket;
    private final SecretCipher cipher;
    public DocumentStorage(@Value("${app.storage.endpoint}") String endpoint,
            @Value("${app.storage.access-key}") String access, @Value("${app.storage.secret-key}") String secret,
            @Value("${app.storage.bucket}") String bucket, @Value("${app.data-encryption-key}") String key) {
        this.client = MinioClient.builder().endpoint(endpoint).credentials(access, secret).build();
        this.bucket = bucket; this.cipher = new SecretCipher(key);
    }
    public void put(String key, byte[] data) {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                try { client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build()); }
                catch (io.minio.errors.ErrorResponseException ex) {
                    if (!"BucketAlreadyOwnedByYou".equals(ex.errorResponse().code())) throw ex;
                }
            }
            byte[] encrypted = cipher.encrypt(Base64.getEncoder().encodeToString(data), key).getBytes(StandardCharsets.UTF_8);
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                    .stream(new ByteArrayInputStream(encrypted), encrypted.length, -1).contentType("application/octet-stream").build());
        } catch (Exception ex) { throw unavailable(); }
    }
    public byte[] get(String key) {
        try (var stream = client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            return Base64.getDecoder().decode(cipher.decrypt(new String(stream.readAllBytes(), StandardCharsets.UTF_8), key));
        } catch (Exception ex) { throw unavailable(); }
    }
    private ApiException unavailable() { return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "DOCUMENT_STORAGE_UNAVAILABLE", "Document storage is temporarily unavailable. Please retry."); }
}
