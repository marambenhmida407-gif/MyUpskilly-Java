package org.example.controller;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.service.UserService;
import org.example.model.User;
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
    private final UserService userService = new UserService();

    public void setUser(User user) {
        this.currentUser = user;
        this.secret = TotpUtil.generateSecret();
        lblSecretKey.setText(secret);
}
