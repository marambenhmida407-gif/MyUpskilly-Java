package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.example.dao.UserDAO;
import org.example.entity.User;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField tfEmail;
    @FXML private PasswordField pfPassword;
    @FXML private Label lblError;
    @FXML private CheckBox chkRemember;

    private final UserDAO userDAO = new UserDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblError.setText("");
    }

    @FXML
    private void handleLogin() {
        lblError.setText("");
        String email = tfEmail.getText().trim();
        String password = pfPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            lblError.setText("Email et mot de passe obligatoires.");
            return;
        }

        User user = userDAO.findByEmail(email);

        if (user == null || !user.getPassword().equals(password)) {
            lblError.setText("Email ou mot de passe incorrect.");
            return;
        }

        try {
            // Has 2FA already set up?
            if (user.getGoogleAuthenticatorSecret() != null
                    && !user.getGoogleAuthenticatorSecret().isEmpty()) {
                // → Go to verification screen
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/verify_2fa.fxml"));
                Pane root = loader.load();
                Verify2FAController ctrl = loader.getController();
                ctrl.setUser(user);
                Stage stage = (Stage) tfEmail.getScene().getWindow();
                stage.setScene(new Scene(root, 400, 400));
                stage.setTitle("MyUpskilly - Vérification 2FA");
            } else {
                // → First login: set up 2FA
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/setup_2fa.fxml"));
                Pane root = loader.load();
                Setup2FAController ctrl = loader.getController();
                ctrl.setUser(user);
                Stage stage = (Stage) tfEmail.getScene().getWindow();
                stage.setScene(new Scene(root, 450, 550));
                stage.setTitle("MyUpskilly - Configuration 2FA");
            }
        } catch (Exception e) {
            e.printStackTrace();
            lblError.setText("Erreur navigation: " + e.getMessage());
        }
    }

    @FXML
    private void handleClear() {
        tfEmail.clear();
        pfPassword.clear();
        lblError.setText("");
        tfEmail.getStyleClass().remove("field-error");
        pfPassword.getStyleClass().remove("field-error");
    }

    @FXML
    private void handleGoToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/register.fxml"));
            Pane root = loader.load();
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.setScene(new Scene(root, 950, 650));
            stage.setTitle("MyUpskilly - Inscription");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}