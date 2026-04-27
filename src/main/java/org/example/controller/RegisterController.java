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
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    @FXML private TextField tfNom;
    @FXML private TextField tfPrenom;
    @FXML private TextField tfEmail;
    @FXML private TextField tfUserName;
    @FXML private PasswordField pfPassword;
    @FXML private PasswordField pfConfirmPassword;
    @FXML private TextField tfSpecialite;
    @FXML private TextField tfAdresse;
    @FXML private ComboBox<String> cbRole;
    @FXML private Label lblError;
    @FXML private Label lblSuccess;

    private final UserDAO userDAO = new UserDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblError.setText("");
        lblSuccess.setText("");
        cbRole.getItems().addAll("ROLE_PATIENT", "ROLE_MEDECIN", "ROLE_USER");
        cbRole.setValue("ROLE_PATIENT");
    }

    @FXML
    private void handleRegister() {
        lblError.setText("");
        lblSuccess.setText("");
        clearErrors();

        StringBuilder errors = new StringBuilder();

        // Nom
        if (tfNom.getText() == null || tfNom.getText().trim().isEmpty()) {
            errors.append("• Nom obligatoire\n");
            tfNom.getStyleClass().add("field-error");
        } else if (tfNom.getText().trim().length() < 2) {
            errors.append("• Nom minimum 2 caractères\n");
            tfNom.getStyleClass().add("field-error");
        }

        // Prénom
        if (tfPrenom.getText() == null || tfPrenom.getText().trim().isEmpty()) {
            errors.append("• Prénom obligatoire\n");
            tfPrenom.getStyleClass().add("field-error");
        } else if (tfPrenom.getText().trim().length() < 2) {
            errors.append("• Prénom minimum 2 caractères\n");
            tfPrenom.getStyleClass().add("field-error");
        }

        // Email
        String email = tfEmail.getText();
        if (email == null || email.trim().isEmpty()) {
            errors.append("• Email obligatoire\n");
            tfEmail.getStyleClass().add("field-error");
        } else if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            errors.append("• Format email invalide\n");
            tfEmail.getStyleClass().add("field-error");
        } else if (userDAO.emailExists(email.trim(), 0)) {
            errors.append("• Cet email est déjà utilisé\n");
            tfEmail.getStyleClass().add("field-error");
        }

        // Mot de passe
        String password = pfPassword.getText();
        if (password == null || password.trim().isEmpty()) {
            errors.append("• Mot de passe obligatoire\n");
            pfPassword.getStyleClass().add("field-error");
        } else if (password.length() < 6) {
            errors.append("• Mot de passe minimum 6 caractères\n");
            pfPassword.getStyleClass().add("field-error");
        }

        // Confirmation mot de passe
        String confirmPassword = pfConfirmPassword.getText();
        if (confirmPassword == null || !confirmPassword.equals(password)) {
            errors.append("• Les mots de passe ne correspondent pas\n");
            pfConfirmPassword.getStyleClass().add("field-error");
        }

        // Rôle
        if (cbRole.getValue() == null) {
            errors.append("• Rôle obligatoire\n");
            cbRole.getStyleClass().add("field-error");
        }

        if (errors.length() > 0) {
            lblError.setText(errors.toString());
            return;
        }

        // Créer l'utilisateur
        User user = new User();
        user.setNom(tfNom.getText().trim());
        user.setPrenom(tfPrenom.getText().trim());
        user.setEmail(tfEmail.getText().trim());
        user.setUserName(tfUserName.getText() != null ? tfUserName.getText().trim() : null);
        user.setPassword(password);
        user.setSpecialite(tfSpecialite.getText() != null ? tfSpecialite.getText().trim() : null);
        user.setAdresse(tfAdresse.getText() != null ? tfAdresse.getText().trim() : null);
        user.setRoles("[\"" + cbRole.getValue() + "\"]");
        user.setVerified(false);
        user.setEtat(true);

        try {
            userDAO.save(user);
            lblSuccess.setText("Compte créé avec succès ! Vous pouvez maintenant vous connecter.");
            clearForm();
        } catch (Exception e) {
            lblError.setText("Erreur lors de la création : " + e.getMessage());
        }
    }

    @FXML
    private void handleGoToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Pane root = loader.load();
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            Scene scene = new Scene(root, 900, 550);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setTitle("MyUpskilly - Connexion");
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearForm() {
        tfNom.clear();
        tfPrenom.clear();
        tfEmail.clear();
        tfUserName.clear();
        pfPassword.clear();
        pfConfirmPassword.clear();
        tfSpecialite.clear();
        tfAdresse.clear();
        cbRole.setValue("ROLE_PATIENT");
        clearErrors();
    }

    private void clearErrors() {
        tfNom.getStyleClass().remove("field-error");
        tfPrenom.getStyleClass().remove("field-error");
        tfEmail.getStyleClass().remove("field-error");
        pfPassword.getStyleClass().remove("field-error");
        pfConfirmPassword.getStyleClass().remove("field-error");
        cbRole.getStyleClass().remove("field-error");
    }
}