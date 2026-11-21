package com.eventticketing.notification;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class TicketMailWorker {
 private final JdbcTemplate db;private final IdentityContact identity;private final JavaMailSender mail;private final String web;
 public TicketMailWorker(JdbcTemplate db,IdentityContact identity,JavaMailSender mail,@Value("${app.web-base-url}")String web){this.db=db;this.identity=identity;this.mail=mail;this.web=web;}
 @Scheduled(fixedDelayString="${app.mail.poll-millis:2000}",initialDelayString="${app.mail.poll-millis:2000}") @Transactional public void deliver(){for(var r:db.queryForList("select * from notification.ticket_deliveries where delivered_at is null and available_at<=clock_timestamp() order by created_at limit 20 for update skip locked"))try{boolean cancelled="CANCELLED".equals(r.get("kind"));var contact=identity.read((UUID)r.get("customer_id"));var message=new SimpleMailMessage();message.setFrom("no-reply@event-ticketing.local");message.setTo(contact.email());message.setSubject(cancelled?"Sự kiện đã hủy - Event Ticketing":"Vé điện tử của bạn đã sẵn sàng");message.setText((cancelled?"Sự kiện đã hủy. Vé không còn quyền vào cửa. Các khoản thanh toán được theo dõi theo quy trình ngoại lệ của đơn hàng.":"Vé điện tử của bạn đã được phát hành. Đăng nhập để xem mã QR và tải PDF. Giữ riêng mã QR của bạn.")+"\nSự kiện: "+r.get("event_name")+"\nMã đơn: "+r.get("order_id")+"\nXem trạng thái hiện tại: "+web+"/account/orders/"+r.get("order_id"));mail.send(message);db.update("update notification.ticket_deliveries set delivered_at=clock_timestamp(),attempts=attempts+1 where order_id=? and kind=?",r.get("order_id"),r.get("kind"));}catch(RuntimeException ex){db.update("update notification.ticket_deliveries set attempts=attempts+1,available_at=clock_timestamp()+interval '30 seconds' where order_id=? and kind=?",r.get("order_id"),r.get("kind"));}}
}
