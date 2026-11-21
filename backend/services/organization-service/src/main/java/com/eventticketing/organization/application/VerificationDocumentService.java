package com.eventticketing.organization.application;

import com.eventticketing.organization.api.ApiException;
import com.eventticketing.organization.infrastructure.DocumentStorage;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VerificationDocumentService {
    private final JdbcTemplate db; private final DocumentStorage storage;
    public VerificationDocumentService(JdbcTemplate db,DocumentStorage storage) {this.db=db;this.storage=storage;}
    public UUID upload(UUID owner, MultipartFile file) {
        if(file.isEmpty() || file.getSize()>5242880) throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_DOCUMENT","Choose a PDF, PNG or JPEG document up to 5 MB.");
        byte[] data;
        try {data=file.getBytes();} catch(java.io.IOException ex) {throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_DOCUMENT","Could not read the document.");}
        String type=detect(data);
        if(!type.equals(file.getContentType())) throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_DOCUMENT","The file content must match its PDF, PNG or JPEG type.");
        UUID id=UUID.randomUUID(); String key=owner+"/"+id;
        storage.put(key,data);
        db.update("insert into organization.verification_documents(id,owner_user_id,object_key,content_type,size_bytes) values (?,?,?,?,?)",id,owner,key,type,data.length);
        return id;
    }
    public void requireOwned(UUID id,UUID owner) {
        if(id==null || db.queryForList("select id from organization.verification_documents where id=? and owner_user_id=?",id,owner).isEmpty()) throw OrganizationAccess.missing();
    }
    public Document download(UUID id,UUID actor,boolean admin) {
        var rows=db.queryForList("select * from organization.verification_documents where id=?",id);
        if(rows.isEmpty()) throw OrganizationAccess.missing();
        var r=rows.getFirst();
        boolean submitted=!db.queryForList("select id from organization.organizer_applications where document_id=?",id).isEmpty();
        if(!actor.equals(r.get("owner_user_id")) && !(admin && submitted)) throw OrganizationAccess.missing();
        return new Document(storage.get((String)r.get("object_key")),(String)r.get("content_type"));
    }
    private String detect(byte[] b) {
        if(b.length>=5 && new String(b,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) return "application/pdf";
        if(b.length>=8 && Arrays.equals(Arrays.copyOf(b,8),new byte[]{(byte)137,80,78,71,13,10,26,10})) return "image/png";
        if(b.length>=3 && b[0]==(byte)255 && b[1]==(byte)216 && b[2]==(byte)255) return "image/jpeg";
        throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_DOCUMENT","Only PDF, PNG and JPEG documents are accepted.");
    }
    public record Document(byte[] bytes,String type) {}
}
