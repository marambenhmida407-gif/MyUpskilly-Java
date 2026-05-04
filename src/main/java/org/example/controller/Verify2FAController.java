package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import org.example.model.User;
import org.example.util.TotpUtil;

public class Verify2FAController {

    @FXML private TextField tfCode;
    @FXML private Label lblError;

    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
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

        if (TotpUtil.verifyCode(currentUser.getGoogleAuthenticatorSecret(), code)) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/user.fxml"));
                Pane root = loader.load();

                Stage stage = (Stage) tfCode.getScene().getWindow();
                stage.setScene(new Scene(root, 1100, 650));
                stage.setTitle("MyUpskilly - Dashboard");
                stage.setResizable(true);

            } catch (Exception e) {
                e.printStackTrace();
                lblError.setText("Erreur lors du chargement du dashboard.");
            }
        } else {
            lblError.setText("Code incorrect. Réessayez.");
            tfCode.clear();
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Pane root = loader.load();

            Stage stage = (Stage) tfCode.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 550));
            stage.setTitle("MyUpskilly - Connexion");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}