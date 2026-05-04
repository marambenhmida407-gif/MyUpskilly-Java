package com.yessinmedqa.controllers;

import com.yessinmedqa.Session;
import com.yessinmedqa.services.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

import java.io.IOException;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         emailError;
    @FXML private Label         passwordError;
    @FXML private Label         loginError;

    @FXML
    public void seConnecter(ActionEvent event) {
        // Clear errors
        emailError.setText("");
        passwordError.setText("");
        loginError.setText("");

        boolean valid = true;

        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        // Validate email
        if (email.isEmpty()) {
            emailError.setText("L'email est obligatoire.");
            valid = false;
        } else if (!email.contains("@")) {
            emailError.setText("Email invalide.");
            valid = false;
        }

        // Validate password
        if (password.isEmpty()) {
            passwordError.setText("Le mot de passe est obligatoire.");
            valid = false;
        }

        if (!valid) return;

        // Try login
        UserService us = new UserService();
        String[] user = us.login(email, password);

        if (user == null) {
            loginError.setText("❌ Email ou mot de passe incorrect.");
            return;
        }

        // Save to session
        Session.setUserId(Integer.parseInt(user[0]));
        Session.setNom(user[1]);
        Session.setPrenom(user[2]);
        Session.setEmail(user[3]);
        Session.setRole(user[4]);
        Session.setSpecialite(user[5]);

        System.out.println("✅ Connecté: " + Session.getPrenom() + " | Role: " + Session.getRole());

        // Navigate based on role
        try {
            String fxml;
            if (Session.isDoctor()) {
                fxml = "/com/yessinmedqa/espaceMedecin.fxml";
            } else if (Session.isAdmin()) {
                fxml = "/com/yessinmedqa/admin.fxml";
            } else {
                fxml = "/com/yessinmedqa/poserQuestion.fxml";
            }

            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            emailField.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("Erreur navigation: " + e.getMessage());
        }
    }

    @FXML
    public void initialize() {}
}