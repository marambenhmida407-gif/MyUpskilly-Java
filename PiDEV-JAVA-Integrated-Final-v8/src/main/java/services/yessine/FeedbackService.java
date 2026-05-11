package services.yessine;

import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class FeedbackService {

    public void insertFeedback(int reponseId, int rating, String commentaire, String userNom) {
        String sql = "INSERT INTO feedback (reponse_id, rating, commentaire, user_nom) VALUES (?, ?, ?, ?)";

        try (Connection conn = MyDatabase.getInstance().getConnection();
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
    public void reportFeedback(int feedbackId, int reporterId, String raison) {
        System.out.println("=== REPORT FEEDBACK CALLED ===");
        System.out.println("feedbackId: " + feedbackId);
        System.out.println("reporterId: " + reporterId);
        System.out.println("raison: " + raison);

        String sql = "INSERT INTO reports (question_id, feedback_id, reporter_id, raison, type) " +
                "VALUES (NULL, ?, ?, ?, 'feedback')";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, feedbackId);
            ps.setInt(2, reporterId);
            ps.setString(3, raison);
            int rows = ps.executeUpdate();
            System.out.println("✅ Rows inserted: " + rows);
        } catch (SQLException e) {
            System.err.println("❌ reportFeedback error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}