package com.yessinmedqa.services;

import com.yessinmedqa.config.DBConnection;
import com.yessinmedqa.models.Question;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class QuestionService {

    public void insert(Question q) {
        String sql = "INSERT INTO questions (user_id, titre, description, sous_traitement, " +
                "a_des_allergies, fichier_chemin, statut, taille, poids, " +
                "description_traitement, description_allergies, categorie) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'en_attente', ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql,
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, q.getUserId());
            ps.setString(2, q.getTitre());
            ps.setString(3, q.getDescription());
            ps.setBoolean(4, q.isSousTraitement());
            ps.setBoolean(5, q.isaDesAllergies());
            ps.setString(6, q.getFichierChemin());
            ps.setDouble(7, q.getTaille());
            ps.setDouble(8, q.getPoids());
            ps.setString(9, q.getDescriptionTraitement());
            ps.setString(10, q.getDescriptionAllergies());
            ps.setString(11, q.getCategorie());

            ps.executeUpdate();
            System.out.println("✅ Question inserted");

            // Get generated ID for auto-moderation
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int newId = keys.getInt(1);
                // Auto-moderation runs in background thread
                new ModerationService().autoModerate(
                        newId, q.getTitre(), q.getDescription()
                );
            }

        } catch (SQLException e) {
            System.err.println("❌ insert error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void deleteQuestion(int questionId, int userId) {
        // Only deletes if the question belongs to this user
        // The AND user_id = ? is server-side security
        String sql = "DELETE FROM questions WHERE id = ? AND user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, questionId);
            ps.setInt(2, userId);

            int rows = ps.executeUpdate();
            System.out.println(rows > 0
                    ? "✅ Question supprimée avec succès"
                    : "❌ Suppression refusée — question introuvable ou non autorisée");

        } catch (SQLException e) {
            System.err.println("❌ deleteQuestion error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}