package com.eventticketing.commerce.application;

import com.eventticketing.commerce.api.ApiException;
import com.eventticketing.commerce.infrastructure.EventMediaStorage;
import jakarta.validation.constraints.*;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventMediaService {
    private final EventRepository repo;private final EventService events;private final EventMediaStorage storage;
    public EventMediaService(EventRepository repo,EventService events,EventMediaStorage storage){this.repo=repo;this.events=events;this.storage=storage;}
    @Transactional public UploadIntent create(UUID actor,UploadInput input) {
        var e=events.managed(input.organizationId(),input.eventId(),actor);EventService.editable(e);
        if(!Set.of("BANNER","GALLERY").contains(input.kind())||!Set.of("image/png","image/jpeg").contains(input.contentType()))throw invalid();
        UUID id=UUID.randomUUID();String key=e.id()+"/"+id;
        String url=storage.upload(key);
        repo.db().update("insert into event_commerce.media_uploads(id,event_id,actor_id,kind,content_type,byte_size,sha256,object_key,expires_at) values (?,?,?,?,?,?,?,?,?)",id,e.id(),actor,input.kind(),input.contentType(),input.byteSize(),input.sha256().toLowerCase(Locale.ROOT),key,EventService.stamp(Instant.now().plusSeconds(600)));
        return new UploadIntent(id,url,600);
    }
    @Transactional public UUID complete(UUID id,UUID actor) {
        var upload=owned(id,actor);var e=repo.get((UUID)upload.get("event_id"));events.managed(e.organizationId(),e.id(),actor);EventService.editable(e);
        repo.db().queryForList("select id from event_commerce.media_uploads where id=? for update",id);upload=owned(id,actor);
        if("AVAILABLE".equals(upload.get("status")))return id;
        if(!"PENDING".equals(upload.get("status"))||!Instant.now().isBefore(((java.sql.Timestamp)upload.get("expires_at")).toInstant()))throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"MEDIA_EXPIRED","Lượt tải ảnh đã hết hạn. Chọn lại ảnh để tải lên.");
        String key=(String)upload.get("object_key"),mime=(String)upload.get("content_type");
        byte[] bytes=storage.staged(key,((Number)upload.get("byte_size")).longValue());
        validate(bytes,mime,(String)upload.get("sha256"));storage.commit(key,bytes,mime);
        repo.db().update("update event_commerce.media_uploads set status='AVAILABLE' where id=?",id);storage.removeStaged(key);return id;
    }
    public String download(UUID id,UUID actor,boolean admin) {
        var rows=repo.db().queryForList("select * from event_commerce.media_uploads where id=? and status='AVAILABLE'",id);
        if(rows.isEmpty())throw EventRepository.missing();var upload=rows.getFirst();var e=repo.get((UUID)upload.get("event_id"));
        boolean attached=id.equals(e.bannerMediaId())||e.galleryMediaIds().contains(id);
        if(e.publishedAt()==null||!attached) {if(actor==null)throw EventRepository.missing();events.read(e.organizationId(),e.id(),actor,admin);}
        return storage.download((String)upload.get("object_key"));
    }
    @Transactional public void abandon(UUID id,UUID actor) {
        var upload=owned(id,actor);var e=repo.get((UUID)upload.get("event_id"));events.managed(e.organizationId(),e.id(),actor);
        if(id.equals(e.bannerMediaId())||e.galleryMediaIds().contains(id))throw new ApiException(HttpStatus.CONFLICT,"MEDIA_IN_USE","Ảnh đang được dùng trong sự kiện.");
        repo.db().update("update event_commerce.media_uploads set status='ABANDONED' where id=?",id);storage.removeStaged((String)upload.get("object_key"));
    }
    private Map<String,Object> owned(UUID id,UUID actor) {return repo.db().queryForList("select * from event_commerce.media_uploads where id=? and actor_id=?",id,actor).stream().findFirst().orElseThrow(EventRepository::missing);}
    public static void validate(byte[] bytes,String mime,String checksum) {
        try {
            if(bytes.length==0||bytes.length>5242880||!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(checksum))throw invalid();
            try(var image=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers=ImageIO.getImageReaders(image);if(!readers.hasNext())throw invalid();var reader=readers.next();
                try {
                    reader.setInput(image,true,true);String format=reader.getFormatName().toLowerCase(Locale.ROOT);
                    if(!(mime.equals("image/png")&&format.equals("png")||mime.equals("image/jpeg")&&(format.equals("jpeg")||format.equals("jpg"))))throw invalid();
                    var params=reader.getDefaultReadParam();params.setSourceSubsampling(Math.max(1,reader.getWidth(0)/1024),Math.max(1,reader.getHeight(0)/1024),0,0);
                    if(reader.read(0,params)==null)throw invalid();
                }finally{reader.dispose();}
            }
        }catch(ApiException ex){throw ex;}catch(Exception ex){throw invalid();}
    }
    private static ApiException invalid(){return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"MEDIA_INVALID","Chọn ảnh PNG hoặc JPEG hợp lệ, tối đa 5 MB. Nội dung và mã kiểm tra ảnh phải khớp.");}
    public record UploadInput(@NotNull UUID organizationId,@NotNull UUID eventId,@NotBlank String kind,@NotBlank String contentType,@Min(1) @Max(5242880) long byteSize,@NotNull @Pattern(regexp="[a-fA-F0-9]{64}") String sha256) {}
    public record UploadIntent(UUID id,String uploadUrl,int expiresInSeconds) {}
}
