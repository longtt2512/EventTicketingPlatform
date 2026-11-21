package com.eventticketing.commerce.infrastructure;

import com.eventticketing.commerce.api.ApiException;
import io.minio.*;
import io.minio.http.Method;
import java.io.*;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class EventMediaStorage {
    private final MinioClient client;private final String bucket;
    public EventMediaStorage(@Value("${app.storage.endpoint:http://localhost:9000}") String endpoint,@Value("${app.storage.access-key:localminio}") String access,@Value("${app.storage.secret-key:localminio123}") String secret,@Value("${app.storage.bucket:event-media}") String bucket) {
        client=MinioClient.builder().endpoint(endpoint).credentials(access,secret).build();this.bucket=bucket;
    }
    private void ensureBucket() throws Exception {
        if(!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))try {client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());}catch(io.minio.errors.ErrorResponseException ex){if(!"BucketAlreadyOwnedByYou".equals(ex.errorResponse().code()))throw ex;}
    }
    public String upload(String key) {
        try {ensureBucket();return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Method.PUT).bucket(bucket).object("pending/"+key).expiry(10,TimeUnit.MINUTES).build());}
        catch(Exception ex){throw unavailable();}
    }
    public byte[] staged(String key,long expected) {
        try {
            var stat=client.statObject(StatObjectArgs.builder().bucket(bucket).object("pending/"+key).build());
            if(stat.size()!=expected)throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"MEDIA_INVALID","Kích thước ảnh không khớp. Hãy chọn lại ảnh.");
            try(var stream=client.getObject(GetObjectArgs.builder().bucket(bucket).object("pending/"+key).build())) {return stream.readNBytes(5242881);}
        }catch(ApiException ex){throw ex;}catch(Exception ex){throw unavailable();}
    }
    public void commit(String key,byte[] bytes,String mime) {
        try {client.putObject(PutObjectArgs.builder().bucket(bucket).object("available/"+key).stream(new ByteArrayInputStream(bytes),bytes.length,-1).contentType(mime).build());}
        catch(Exception ex){throw unavailable();}
    }
    public String download(String key) {
        try {return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Method.GET).bucket(bucket).object("available/"+key).expiry(5,TimeUnit.MINUTES).build());}
        catch(Exception ex){throw unavailable();}
    }
    public void removeStaged(String key) {
        try{client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object("pending/"+key).build());}catch(Exception ignored){/* Unreferenced staging objects can be swept; never undo a committed image. */}
    }
    private ApiException unavailable(){return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"MEDIA_STORAGE_UNAVAILABLE","Kho ảnh tạm thời không phản hồi. Giữ nguyên ảnh và thử lại.");}
}
