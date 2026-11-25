package com.eventticketing.notification;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class OperationsMailWorker {
 private final JdbcTemplate db;private final ObjectMapper json;private final OperationsSources sources;private final IdentityContact identity;private final JavaMailSender mail;private final String web,organizer;
 public OperationsMailWorker(JdbcTemplate db,ObjectMapper json,OperationsSources sources,IdentityContact identity,JavaMailSender mail,@Value("${app.web-base-url}")String web,@Value("${app.organizer-web-base-url}")String organizer){this.db=db;this.json=json;this.sources=sources;this.identity=identity;this.mail=mail;this.web=web;this.organizer=organizer;}
 @Scheduled(fixedDelayString="${app.mail.poll-millis:2000}",initialDelayString="${app.mail.poll-millis:2000}") @Transactional public void deliver(){
  for(var row:db.queryForList("select * from notification.operations_deliveries where delivered_at is null and available_at<=clock_timestamp() order by created_at limit 20 for update skip locked"))try{
   var p=json.readTree(row.get("payload").toString());String kind=p.path("kind").asText();
   if(kind.equals("REMINDER")&&!sources.reminderEligible(p)){retry(row);continue;}
   var contact=identity.read((UUID)row.get("user_id"));boolean en="en".equals(contact.locale());String subject=switch(kind){case "REMINDER"->en?"Your event is coming up":"Sắp đến giờ tham dự sự kiện";case "APPLICATION_APPROVED"->en?"Organizer application approved":"Hồ sơ tổ chức đã được duyệt";case "APPLICATION_REJECTED"->en?"Organizer application needs changes":"Hồ sơ tổ chức cần bổ sung";case "APPROVED"->en?"Event approved":"Sự kiện đã được duyệt";case "REJECTED"->en?"Event needs changes":"Sự kiện cần bổ sung";case "CANCELLED"->en?"Event cancelled":"Sự kiện đã hủy";case "SETTLED"->en?"Local settlement completed":"Đã hoàn tất đối soát local";default->throw new IllegalArgumentException("Unknown notice");};
   String link=kind.equals("REMINDER")||kind.equals("CANCELLED")?web+"/account/orders":kind.startsWith("APPLICATION")?organizer+"/apply":kind.equals("SETTLED")?organizer+"/organizations/"+p.path("organizationId").asText()+"/settlements":organizer+"/organizations/"+p.path("organizationId").asText()+"/events/"+p.path("eventId").asText();
   String details=p.path("name").asText()+"\n"+p.path("reason").asText();
   if(kind.equals("REMINDER"))details+="\n"+(en?"Starts at: ":"Bắt đầu: ")+p.path("startAt").asText()+"\n"+(en?"Sign in to view your tickets and current schedule.":"Đăng nhập để xem vé và lịch hiện tại.");
   if(kind.equals("SETTLED"))details+="\n"+p.path("amount").asText()+" VND\n"+(en?"Local simulated evidence; no bank transfer was made.":"Bằng chứng mô phỏng local. Không chuyển tiền ngân hàng.");
   var message=new SimpleMailMessage();message.setFrom("no-reply@event-ticketing.local");message.setTo(contact.email());message.setSubject(subject+" · Event Ticketing");message.setText(subject+"\n"+details+"\n"+link);mail.send(message);
   db.update("update notification.operations_deliveries set delivered_at=clock_timestamp(),attempts=attempts+1 where obligation_key=? and user_id=?",row.get("obligation_key"),row.get("user_id"));
  }catch(Exception ex){retry(row);}
 }
 private void retry(Map<String,Object> row){db.update("update notification.operations_deliveries set attempts=attempts+1,available_at=clock_timestamp()+interval '30 seconds' where obligation_key=? and user_id=?",row.get("obligation_key"),row.get("user_id"));}
}
