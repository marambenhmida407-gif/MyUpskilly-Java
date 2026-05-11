package controllers.yessine;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.User;
import services.yessine.UserService;
import utils.Session;

import java.io.IOException;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         emailError;
    @FXML private Label         passwordError;
    @FXML private Label         loginError;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        if (emailError != null)    emailError.setText("");
        if (passwordError != null) passwordError.setText("");
        if (loginError != null)    loginError.setText("");
    }

    @FXML
    public void seConnecter(ActionEvent event) {
        clearErrors();

        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        // ── Validation champs ────────────────────────────────────────────────
        boolean valid = true;
        if (email.isEmpty()) {
            emailError.setText("L'email est obligatoire.");
            valid = false;
        } else if (!email.contains("@")) {
            emailError.setText("Email invalide.");
            valid = false;
        }
        if (password.isEmpty()) {
            passwordError.setText("Le mot de passe est obligatoire.");
            valid = false;
        }
        if (!valid) return;

        // ── Authentification ─────────────────────────────────────────────────
        User user = userService.login(email, password);

        if (user == null) {
            loginError.setText("❌ Email ou mot de passe incorrect.");
            return;
        }

        // ── Vérification compte actif ────────────────────────────────────────
        if (!user.isActif()) {
            loginError.setText("⛔ Votre compte a été désactivé. Contactez l'administrateur.");
            return;
        }

        // ── Remplir la session ───────────────────────────────────────────────
        Session.setUserId(user.getId());
        Session.setNom(user.getNom());
        Session.setPrenom(user.getPrenom());
        Session.setEmail(user.getEmail());
        Session.setRole(user.getRole());
        Session.setSpecialite(user.getSpecialite());
        Session.setPhoto(user.getPhoto());
        Session.setEtat(user.getEtat());
        Session.setVerified(user.isVerified());

        System.out.println("✅ Connecté: " + Session.getPrenom() + " | Rôle: " + Session.getRole());

        // ── Vérification 2FA ─────────────────────────────────────────────────
        if (user.has2FA()) {
            navigateTo2FAVerify(user);
        } else {
            navigateByRole();  // forcer setup pour test
        }
    }

    /** Navigation vers la page de vérification 2FA */
    private void navigateTo2FAVerify(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/yessine/verify_2fa.fxml"));
            Parent root = loader.load();
            Verify2FAController ctrl = loader.getController();
            ctrl.setUser(user);
            emailField.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur navigation verify_2fa: " + e.getMessage());
            loginError.setText("Erreur lors du chargement de la 2FA.");
        }
    }

    /** Routing par rôle après authentification réussie */
    public static void navigateByRole(Parent anyNode) {
        String fxml;
        if (Session.isDoctor()) {
            fxml = "/MainLayout.fxml";
        } else if (Session.isAdmin()) {
            fxml = "/yessine/admin.fxml";
        } else {
            fxml = "/yessine/poserQuestion.fxml";
        }
        try {
            Parent root = FXMLLoader.load(LoginController.class.getResource(fxml));
            anyNode.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur navigation par rôle: " + e.getMessage());
        }
    }

    private void navigateByRole() {
        navigateByRole(emailField);
    }

    // ── Lien vers Register ────────────────────────────────────────────────────

    @FXML
    public void handleGoToRegister(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/yessine/register.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 680));
            stage.setTitle("Esprit Médical — Inscription");
        } catch (IOException e) {
            System.err.println("Erreur navigation register: " + e.getMessage());
        }
    }

    private void clearErrors() {
        emailError.setText("");
        passwordError.setText("");
        loginError.setText("");
    }

    private void navigateTo2FASetup(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/yessine/setup_2fa.fxml"));
            Parent root = loader.load();
            Setup2FAController ctrl = loader.getController();
            ctrl.setUser(user);
            emailField.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur navigation setup_2fa: " + e.getMessage());
            loginError.setText("Erreur chargement 2FA.");
        }
    }

}
