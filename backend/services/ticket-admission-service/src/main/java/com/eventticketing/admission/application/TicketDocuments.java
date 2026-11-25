package com.eventticketing.admission.application;
import com.eventticketing.admission.infrastructure.TicketArtifactStore;
import com.eventticketing.admission.api.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class TicketDocuments {
 private final TicketQueries tickets;private final TicketPdfRenderer renderer;private final TicketArtifactStore store;
 public TicketDocuments(TicketQueries tickets,TicketPdfRenderer renderer,TicketArtifactStore store){this.tickets=tickets;this.renderer=renderer;this.store=store;}
 @Transactional public byte[] pdf(UUID id,UUID actor){var ticket=tickets.detail(id,actor);try{var bytes=renderer.render(ticket);store.put("tickets/"+id+"/"+ticket.version()+"-"+(ticket.qrImage()!=null?"qr":"status")+".pdf",bytes);return bytes;}catch(RuntimeException ex){throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"PDF_UNAVAILABLE","Chưa thể tạo bản PDF. Vé vẫn được giữ nguyên. Hãy thử tải lại.");}}
}
