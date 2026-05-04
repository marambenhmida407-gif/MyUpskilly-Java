package com.yessinmedqa.services;

import com.yessinmedqa.config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class FeedbackService {

    public void insertFeedback(int reponseId, int rating, String commentaire, String userNom) {
        String sql = "INSERT INTO feedback (reponse_id, rating, commentaire, user_nom) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, reponseId);
            ps.setInt(2, rating);
            ps.setString(3, commentaire);
            ps.setString(4, userNom != null ? userNom : "Anonyme");
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erreur feedback: " + e.getMessage());
        }
    }
}