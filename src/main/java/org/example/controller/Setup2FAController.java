package org.example.controller;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.dao.UserDAO;
import org.example.entity.User;
import org.example.util.TotpUtil;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ResourceBundle;

public class Setup2FAController implements Initializable {

    @FXML private ImageView qrImageView;
    @FXML private Label lblSecretKey;
    @FXML private TextField tfCode;
    @FXML private Label lblError;

    private User currentUser;
    private String secret;
    private final UserDAO userDAO = new UserDAO();

    public void setUser(User user) {
        this.currentUser = user;
        this.secret = TotpUtil.generateSecret();
        lblSecretKey.setText(secret);
        generateQrCode();
    }

    private void generateQrCode() {
        try {
            String otpUrl = TotpUtil.getQrUrl(currentUser.getEmail(), secret);
            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix matrix = qrWriter.encode(otpUrl, BarcodeFormat.QR_CODE, 200, 200);
            BufferedImage buffered = MatrixToImageWriter.toBufferedImage(matrix);
            qrImageView.setImage(SwingFXUtils.toFXImage(buffered, null));
        } catch (Exception e) {
            lblError.setText("Erreur génération QR: " + e.getMessage());
        }
    }

    @FXML
    private void handleVerify() {
        lblError.setText("");
        String codeText = tfCode.getText().trim();
        if (codeText.isEmpty() || !codeText.matches("\\d{6}")) {
            lblError.setText("Entrez un code à 6 chiffres.");
            return;
        }
        int code = Integer.parseInt(codeText);
        if (TotpUtil.verifyCode(secret, code)) {
            // Save secret to DB
            currentUser.setGoogleAuthenticatorSecret(secret);
            userDAO.update(currentUser);

            // Go to main app
            navigateToMain();
        } else {
            lblError.setText("Code incorrect. Réessayez.");
            tfCode.clear();
        }
    }

    private void navigateToMain() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/user.fxml"));
            javafx.scene.layout.Pane root = loader.load();
            Stage stage = (Stage) tfCode.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 650));
            stage.setTitle("MyUpskilly - Dashboard");
            stage.setResizable(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {}
}