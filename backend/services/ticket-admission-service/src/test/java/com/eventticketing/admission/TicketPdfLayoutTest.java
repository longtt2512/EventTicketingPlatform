package com.eventticketing.admission;
import static org.assertj.core.api.Assertions.*;
import com.eventticketing.admission.application.*;
import com.eventticketing.admission.application.TicketModels.Ticket;
import java.time.Instant;
import java.util.UUID;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
class TicketPdfLayoutTest {
 @Test void longVietnameseFieldsAndUnbrokenTextDoNotOverlapQr()throws Exception{var t=new Ticket(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"Sự kiện âm nhạc và nghệ thuật ".repeat(7),"Nhà hát thử nghiệm ".repeat(10),"Địa chỉ cần giữ đầy đủ thông tin ".repeat(16),"Asia/Ho_Chi_Minh",Instant.now().plusSeconds(3600),Instant.now().plusSeconds(7200),"V".repeat(160),"Nguyễn Thị Người mua có tên dài ".repeat(5),"ISSUED",false,Instant.now(),null,1,TicketQr.dataUri("layout-fixture-not-a-live-credential"));byte[] bytes=new TicketPdfRenderer().render(t);try(var doc=Loader.loadPDF(bytes)){assertThat(doc.getNumberOfPages()).isEqualTo(2);String text=new PDFTextStripper().getText(doc);assertThat(text).contains(t.id().toString(),"Nguyễn", "trang tiếp theo");assertThat(text.replaceAll("\\s+","")).contains("V".repeat(160));}Path file=Path.of("build/qa/ticket-long.pdf");Files.createDirectories(file.getParent());Files.write(file,bytes);}
}
