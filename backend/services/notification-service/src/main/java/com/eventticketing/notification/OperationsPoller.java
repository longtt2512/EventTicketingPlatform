package com.eventticketing.notification;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
@Service
public class OperationsPoller {
 private final OperationsSources sources;private final OperationsInbox inbox;private final ObjectMapper json;private String reminderCursor="";
 public OperationsPoller(OperationsSources sources,OperationsInbox inbox,ObjectMapper json){this.sources=sources;this.inbox=inbox;this.json=json;}
 @Scheduled(fixedDelayString="${app.mail.poll-millis:2000}",initialDelayString="${app.mail.poll-millis:2000}") public void poll(){
  for(String source:List.of("organization","commerce","finance"))try{for(var notice:sources.read(source,"/internal/notifications")){inbox.accept(source,notice);sources.ack(source,UUID.fromString(notice.path("id").asText()));}}catch(RuntimeException ex){/* owner retains unacknowledged obligations */}
  try{var rows=sources.read("commerce","/internal/notifications/reminders?hours="+sources.hours()+"&cursor="+reminderCursor);for(var row:rows){var p=json.createObjectNode().put("kind","REMINDER").put("eventId",row.path("event_id").asText()).put("userId",row.path("user_id").asText()).put("name",row.path("name").asText()).put("startAt",row.path("start_at").asText());inbox.reminder(p);reminderCursor=p.path("eventId").asText()+":"+p.path("userId").asText();}if(rows.size()<100)reminderCursor="";}catch(RuntimeException ex){/* retry after source recovery; never assume policy or eligibility */}
 }
}
