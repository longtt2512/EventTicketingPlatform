package com.eventticketing.admission.infrastructure;
import io.minio.*;
import java.io.ByteArrayInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class TicketArtifactStore {
 private final MinioClient client;private final String bucket="private-ticket-artifacts";
 public TicketArtifactStore(@Value("${app.storage.endpoint}")String endpoint,@Value("${app.storage.access-key}")String access,@Value("${app.storage.secret-key}")String secret){client=MinioClient.builder().endpoint(endpoint).credentials(access,secret).build();}
 public void put(String key,byte[] bytes){try{if(!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())){try{client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());}catch(io.minio.errors.ErrorResponseException ex){if(!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))throw ex;}}client.putObject(PutObjectArgs.builder().bucket(bucket).object(key).contentType("application/pdf").stream(new ByteArrayInputStream(bytes),bytes.length,-1).build());}catch(Exception ex){throw new IllegalStateException("Private ticket artifact unavailable",ex);}}
}
