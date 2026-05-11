package controllers.yessine;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.User;
import services.yessine.UserService;

import java.io.IOException;

public class RegisterController {

    @FXML private TextField     tfNom;
    @FXML private TextField     tfPrenom;
    @FXML private TextField     tfEmail;
    @FXML private TextField     tfUsername;
    @FXML private PasswordField pfPassword;
    @FXML private PasswordField pfConfirm;
    @FXML private TextField     tfSpecialite;
    @FXML private TextField     tfAdresse;
    @FXML private ComboBox<String> cbRole;
    @FXML private Label         lblError;
    @FXML private Label         lblSuccess;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        lblError.setText("");
        lblSuccess.setText("");
        // Rôles disponibles à l'inscription — adapter selon les besoins
        cbRole.getItems().addAll("patient", "doctor");
        cbRole.setValue("patient");
    }

    @FXML
    public void handleRegister(ActionEvent event) {
        lblError.setText("");
        lblSuccess.setText("");
        clearFieldErrors();

        StringBuilder errors = new StringBuilder();

        // ── Validations ───────────────────────────────────────────────────────
        String nom = tfNom.getText() == null ? "" : tfNom.getText().trim();
        if (nom.isEmpty()) {
            errors.append("• Nom obligatoire\n");
            tfNom.getStyleClass().add("field-error");
        } else if (nom.length() < 2) {
            errors.append("• Nom minimum 2 caractères\n");
            tfNom.getStyleClass().add("field-error");
        }

        String prenom = tfPrenom.getText() == null ? "" : tfPrenom.getText().trim();
        if (prenom.isEmpty()) {
            errors.append("• Prénom obligatoire\n");
            tfPrenom.getStyleClass().add("field-error");
        } else if (prenom.length() < 2) {
            errors.append("• Prénom minimum 2 caractères\n");
            tfPrenom.getStyleClass().add("field-error");
        }

        String email = tfEmail.getText() == null ? "" : tfEmail.getText().trim();
        if (email.isEmpty()) {
            errors.append("• Email obligatoire\n");
            tfEmail.getStyleClass().add("field-error");
        } else if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            errors.append("• Format email invalide\n");
            tfEmail.getStyleClass().add("field-error");
        } else if (userService.emailExists(email, 0)) {
            errors.append("• Cet email est déjà utilisé\n");
            tfEmail.getStyleClass().add("field-error");
        }

        String password = pfPassword.getText() == null ? "" : pfPassword.getText();
        if (password.isEmpty()) {
            errors.append("• Mot de passe obligatoire\n");
            pfPassword.getStyleClass().add("field-error");
        } else if (password.length() < 6) {
            errors.append("• Mot de passe minimum 6 caractères\n");
            pfPassword.getStyleClass().add("field-error");
        }

        String confirm = pfConfirm.getText() == null ? "" : pfConfirm.getText();
        if (!confirm.equals(password)) {
            errors.append("• Les mots de passe ne correspondent pas\n");
            pfConfirm.getStyleClass().add("field-error");
        }

        if (cbRole.getValue() == null) {
            errors.append("• Rôle obligatoire\n");
        }

        if (errors.length() > 0) {
            lblError.setText(errors.toString());
            return;
        }

        // ── Création utilisateur ──────────────────────────────────────────────
        User user = new User();
        user.setNom(nom);
        user.setPrenom(prenom);
        user.setEmail(email);
        user.setPassword(password);       // TODO : hacher avec BCrypt si ajouté
        user.setRole(cbRole.getValue());
        user.setUsername(tfUsername.getText() != null ? tfUsername.getText().trim() : null);
        user.setSpecialite(tfSpecialite.getText() != null ? tfSpecialite.getText().trim() : null);
        user.setAdresse(tfAdresse.getText() != null ? tfAdresse.getText().trim() : null);
        user.setVerified(false);
        user.setEtat(true);

        try {
            userService.save(user);
            lblSuccess.setText("✅ Compte créé avec succès ! Vous pouvez maintenant vous connecter.");
            clearForm();
        } catch (Exception e) {
            lblError.setText("❌ Erreur lors de la création : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleGoToLogin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/yessine/login.fxml"));
            Stage stage = (Stage) tfNom.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 650));
            stage.setTitle("Esprit Médical — Connexion");
        } catch (IOException e) {
            System.err.println("Erreur navigation login: " + e.getMessage());
        }
    }

    private void clearForm() {
        tfNom.clear();
        tfPrenom.clear();
        tfEmail.clear();
        tfUsername.clear();
        pfPassword.clear();
        pfConfirm.clear();
        tfSpecialite.clear();
        tfAdresse.clear();
        cbRole.setValue("patient");
        clearFieldErrors();
    }

    private void clearFieldErrors() {
        tfNom.getStyleClass().remove("field-error");
        tfPrenom.getStyleClass().remove("field-error");
        tfEmail.getStyleClass().remove("field-error");
        pfPassword.getStyleClass().remove("field-error");
        pfConfirm.getStyleClass().remove("field-error");
    }
}
