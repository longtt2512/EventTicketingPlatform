package com.eventticketing.commerce.application;

import com.eventticketing.commerce.api.ApiException;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Categories {
    private final JdbcTemplate db;
    public Categories(JdbcTemplate db){this.db=db;}
    public List<String> active(){return db.queryForList("select name from event_commerce.categories where active order by name",String.class);}
    public void requireNewSelection(String value,String current){
        if(value==null||value.isBlank()||value.equals(current))return;
        if(!Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from event_commerce.categories where name=? and active)",Boolean.class,value)))throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"CATEGORY_UNAVAILABLE","Danh mục đã ngừng sử dụng hoặc không tồn tại. Chọn danh mục đang hoạt động.");
    }
    public Map<String,Object> list(UUID cursor,int limit){
        if(limit<1||limit>100)throw EventRepository.invalid("Giới hạn trang không hợp lệ.");
        var rows=db.queryForList("select id,name,active,version from event_commerce.categories where (?::uuid is null or id>?) order by id limit ?",cursor,cursor,limit+1);
        boolean more=rows.size()>limit;var result=new LinkedHashMap<String,Object>();result.put("data",rows.stream().limit(limit).toList());result.put("hasMore",more);result.put("nextCursor",more?rows.get(limit-1).get("id"):null);return result;
    }
    @Transactional public Map<String,Object> create(String name,UUID actor){
        UUID id=UUID.randomUUID();db.update("insert into event_commerce.categories(id,name) values (?,?)",id,name.strip());audit(id,actor,"CATEGORY_CREATED","Tạo danh mục");return get(id);
    }
    @Transactional public Map<String,Object> change(UUID id,long expected,boolean active,UUID actor,String reason){
        if(db.update("update event_commerce.categories set active=?,version=version+1 where id=? and version=?",active,id,expected)!=1)throw new ApiException(HttpStatus.CONFLICT,"STALE_VERSION","Danh mục đã thay đổi. Tải lại trước khi lưu.");
        audit(id,actor,active?"CATEGORY_ACTIVATED":"CATEGORY_DEACTIVATED",reason);return get(id);
    }
    private Map<String,Object> get(UUID id){return db.queryForList("select id,name,active,version from event_commerce.categories where id=?",id).getFirst();}
    private void audit(UUID id,UUID actor,String action,String reason){db.update("insert into event_commerce.category_audit(id,category_id,actor_id,action,reason) values (?,?,?,?,?)",UUID.randomUUID(),id,actor,action,reason);}
}
