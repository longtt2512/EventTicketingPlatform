package com.eventticketing.payment.application;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.util.*;

final class PaymentQr {
    private PaymentQr() {}
    static String dataUri(UUID token) {
        try {
            var matrix = new MultiFormatWriter().encode("etp:payment:" + token,
                    BarcodeFormat.QR_CODE, 320, 320,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 4));
            var out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception ex) { throw new IllegalStateException("Payment QR rendering unavailable", ex); }
    }
}
