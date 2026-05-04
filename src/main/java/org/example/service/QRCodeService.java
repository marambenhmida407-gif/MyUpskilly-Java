package org.example.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.example.model.Consultation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public class QRCodeService {

    public javafx.scene.image.Image generateQRCodeImage(Consultation c, int size) {
        String url = ConsultationWebServer.getInstance().getBaseUrl() + "/consultation/" + c.getId();
        return generateQR(url, size);
    }

    public void saveQRCodeToFile(Consultation c, String filePath, int size) {
        String url = ConsultationWebServer.getInstance().getBaseUrl() + "/consultation/" + c.getId();
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, size, size);
            MatrixToImageWriter.writeToPath(matrix, "PNG", Path.of(filePath));
        } catch (WriterException | IOException e) {
            e.printStackTrace();
        }
    }

    private javafx.scene.image.Image generateQR(String content, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return new javafx.scene.image.Image(new ByteArrayInputStream(out.toByteArray()));
        } catch (WriterException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}