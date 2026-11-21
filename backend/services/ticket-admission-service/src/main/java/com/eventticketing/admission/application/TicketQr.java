package com.eventticketing.admission.application;
import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.util.*;
public final class TicketQr {
 private TicketQr(){}
 public static String dataUri(String credential){try{var matrix=new MultiFormatWriter().encode(credential,BarcodeFormat.QR_CODE,384,384,Map.of(EncodeHintType.ERROR_CORRECTION,ErrorCorrectionLevel.M,EncodeHintType.MARGIN,4));var out=new ByteArrayOutputStream();MatrixToImageWriter.writeToStream(matrix,"PNG",out);return "data:image/png;base64,"+Base64.getEncoder().encodeToString(out.toByteArray());}catch(Exception ex){throw new IllegalStateException("QR rendering unavailable",ex);}}
}
