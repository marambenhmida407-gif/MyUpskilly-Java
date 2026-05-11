package controllers.yessine;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.User;
import utils.Session;
import utils.TotpUtil;

import java.io.IOException;

/**
 * Vérification du code 2FA lors du login.
 * CORRIGÉ par rapport à la version Maraam :
 *  - Redirige vers la bonne page selon le rôle (MainLayout / admin / patient)
 *    au lieu de toujours aller vers user.fxml
 */
public class Verify2FAController {

    @FXML private TextField tfCode;
    @FXML private Label     lblError;
    @FXML private Label     lblEmail;

    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
        if (lblEmail != null) lblEmail.setText("Code pour : " + user.getEmail());
    }

    @FXML
    public void initialize() {
        if (lblError != null) lblError.setText("");
    }

    @FXML
    public void handleVerify() {
        if (lblError != null) lblError.setText("");

        String codeText = tfCode.getText().trim();
        if (codeText.isEmpty() || !codeText.matches("\\d{6}")) {
            lblError.setText("Entrez un code à 6 chiffres.");
            return;
        }

        int code = Integer.parseInt(codeText);

        if (TotpUtil.verifyCode(currentUser.getGoogleAuthenticatorSecret(), code)) {
            // ✅ Code correct → naviguer vers la page par rôle
            System.out.println("✅ 2FA validée pour " + currentUser.getEmail());
            LoginController.navigateByRole(tfCode);
        } else {
            lblError.setText("❌ Code incorrect. Réessayez.");
            tfCode.clear();
        }
    }

    @FXML
    public void handleBack() {
        try {
            // Retour au login + clear session
            Session.clear();
            Parent root = FXMLLoader.load(getClass().getResource("/yessine/login.fxml"));
            Stage stage = (Stage) tfCode.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 650));
            stage.setTitle("Esprit Médical — Connexion");
        } catch (IOException e) {
            System.err.println("Erreur retour login: " + e.getMessage());
        }
    }
}
