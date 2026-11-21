package com.eventticketing.notification;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class OperationsInbox {
 private final JdbcTemplate db;private final OperationsSources sources;
 public OperationsInbox(JdbcTemplate db,OperationsSources sources){this.db=db;this.sources=sources;}
 @Transactional public void accept(String source,JsonNode notice){
  var recipients=new LinkedHashSet<UUID>();String kind=notice.path("kind").asText();
  if(notice.hasNonNull("userId"))recipients.add(UUID.fromString(notice.path("userId").asText()));
  else {String path="/internal/notifications/recipients/"+notice.path("organizationId").asText()+"?kind="+kind;if(notice.hasNonNull("eventId"))path+="&eventId="+notice.path("eventId").asText();for(var id:sources.read("organization",path))recipients.add(UUID.fromString(id.asText()));}
  if(kind.equals("CANCELLED"))for(var id:sources.read("commerce","/internal/notifications/cancelled-customers/"+notice.path("eventId").asText()))recipients.add(UUID.fromString(id.asText()));
  for(UUID user:recipients)save(source+":"+notice.path("id").asText(),user,notice);
 }
 @Transactional public void reminder(JsonNode notice){save("reminder:"+notice.path("eventId").asText(),UUID.fromString(notice.path("userId").asText()),notice);}
 private void save(String key,UUID user,JsonNode notice){db.update("insert into notification.operations_deliveries(obligation_key,user_id,payload) values (?,?,?::jsonb) on conflict do nothing",key,user,notice.toString());}
}
