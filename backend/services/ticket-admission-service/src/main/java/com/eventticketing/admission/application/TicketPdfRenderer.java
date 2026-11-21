package com.eventticketing.admission.application;
import static com.eventticketing.admission.application.TicketModels.*;
import java.io.*;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;
@Component
public class TicketPdfRenderer {
 public byte[] render(Ticket t){try(var doc=new PDDocument();var fontStream=getClass().getResourceAsStream("/fonts/BeVietnamPro-Regular.ttf");var out=new ByteArrayOutputStream()){
  var font=PDType0Font.load(doc,Objects.requireNonNull(fontStream));var page=new PDPage(PDRectangle.A4);doc.addPage(page);
  try(var c=new PDPageContentStream(doc,page)){
   c.setNonStrokingColor(49/255f,46/255f,129/255f);c.addRect(0,744,595,98);c.fill();
   text(c,font,"EVENT TICKETING",44,798,13,1,1,1);text(c,font,"Vé điện tử",44,765,23,1,1,1);
   float y=712;y=block(c,font,t.eventName(),44,y,507,19,26);y-=12;
   y=block(c,font,"Hạng vé: "+t.ticketTypeName(),44,y,507,12,19);
   y=block(c,font,"Người mua: "+t.buyerName(),44,y,507,12,19);
   var format=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of(t.timezone()));
   y=block(c,font,"Thời gian: "+format.format(t.startAt())+" - "+format.format(t.endAt()),44,y,507,11,18);
   y=block(c,font,"Địa điểm: "+t.venueName(),44,y,507,11,17);
   y=block(c,font,t.venueAddress(),44,y,507,10,16);
   if(y<420){
    text(c,font,"Mã vé: "+t.id()+" - mã QR ở trang tiếp theo",44,44,9,.2f,.23f,.3f);
    var qrPage=new PDPage(PDRectangle.A4);doc.addPage(qrPage);
    try(var next=new PDPageContentStream(doc,qrPage)){text(next,font,"Vé điện tử - mã vào cửa",44,780,20,.19f,.18f,.51f);admission(doc,next,font,t,250);}
   }else admission(doc,c,font,t,0);
  }
  doc.save(out);return out.toByteArray();
 }catch(IOException ex){throw new IllegalStateException("Ticket PDF cannot be rendered",ex);}}
 private void admission(PDDocument doc,PDPageContentStream c,PDFont font,Ticket t,float offset)throws IOException{
  var format=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of(t.timezone()));
   String status=t.eventCancelled()?"SỰ KIỆN ĐÃ HỦY":switch(t.status()){case "CHECKED_IN"->"ĐÃ CHECK-IN";case "VOIDED"->"VÉ ĐÃ VÔ HIỆU";default->t.qrImage()==null?"ĐÃ KẾT THÚC THỜI GIAN SỬ DỤNG":"CHƯA SỬ DỤNG";};
   text(c,font,status,44,378+offset,12,.19f,.18f,.51f);
   if(t.qrImage()!=null){byte[] png=Base64.getDecoder().decode(t.qrImage().substring(t.qrImage().indexOf(',')+1));c.drawImage(PDImageXObject.createFromByteArray(doc,png,"ticket-qr"),187,151+offset,220,220);}
   else{c.setNonStrokingColor(.95f,.95f,.97f);c.addRect(44,171+offset,507,170);c.fill();block(c,font,"Vé này không còn mã QR sử dụng.",70,270+offset,455,15,24);if(t.checkedInAt()!=null)block(c,font,"Đã check-in: "+format.format(t.checkedInAt()),70,238+offset,455,11,18);}
   text(c,font,"Mã vé: "+t.id(),44,129+offset,9,.2f,.23f,.3f);
   text(c,font,"Mã đơn: "+t.orderId(),44,111+offset,9,.2f,.23f,.3f);
   block(c,font,"Giữ riêng mã QR. Vé chỉ được xác thực trực tuyến và sử dụng một lần. Trạng thái hiện tại trên hệ thống quyết định quyền vào cửa.",44,81,507,9,15);
 }

 private float block(PDPageContentStream c,PDFont f,String value,float x,float y,float width,float size,float leading)throws IOException{
  String line="";
  for(String word:clean(f,value).split("\\s+")){
   String candidate=line.isEmpty()?word:line+" "+word;
   if(!line.isEmpty()&&f.getStringWidth(candidate)/1000*size>width){text(c,f,line,x,y,size,.1f,.13f,.2f);y-=leading;line="";}
   for(char ch:(line.isEmpty()?word:" "+word).toCharArray()){
    if(!line.isEmpty()&&f.getStringWidth(line+ch)/1000*size>width){text(c,f,line,x,y,size,.1f,.13f,.2f);y-=leading;line="";}
    line+=ch;
   }
  }
  if(!line.isEmpty()){text(c,f,line,x,y,size,.1f,.13f,.2f);y-=leading;}return y;
 }

 private void text(PDPageContentStream c,PDFont f,String s,float x,float y,float size,float r,float g,float b)throws IOException{c.beginText();c.setFont(f,size);c.setNonStrokingColor(r,g,b);c.newLineAtOffset(x,y);c.showText(clean(f,s));c.endText();}
 private String clean(PDFont f,String s){var b=new StringBuilder();(s==null?"":s).codePoints().forEach(cp->{String ch=new String(Character.toChars(cp));try{if(Character.isISOControl(cp))b.append(' ');else{f.encode(ch);b.append(ch);}}catch(Exception ex){b.append('?');}});return b.toString();}
}
