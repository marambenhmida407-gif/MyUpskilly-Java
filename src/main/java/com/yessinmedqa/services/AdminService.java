package com.yessinmedqa.services;

import com.yessinmedqa.config.DBConnection;

import java.sql.*;
import java.util.*;

public class AdminService {

    // ── STATS ────────────────────────────────────────────────
    public Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM users) as users, " +
                "(SELECT COUNT(*) FROM questions) as questions, " +
                "(SELECT COUNT(*) FROM reponses) as reponses, " +
                "(SELECT COUNT(*) FROM reports WHERE statut='en_attente') as reports";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                stats.put("users",     rs.getInt("users"));
                stats.put("questions", rs.getInt("questions"));
                stats.put("reponses",  rs.getInt("reponses"));
                stats.put("reports",   rs.getInt("reports"));
            }
        } catch (SQLException e) {
            System.err.println("❌ getStats error: " + e.getMessage());
        }
        return stats;
    }

    // ── USERS ────────────────────────────────────────────────
    public List<Map<String, String>> getAllUsers() {
        List<Map<String, String>> list = new ArrayList<>();
        String sql = "SELECT id, nom, prenom, email, role FROM users ORDER BY role, nom";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, String> u = new HashMap<>();
                u.put("id",     rs.getString("id"));
                u.put("nom",    rs.getString("nom"));
                u.put("prenom", rs.getString("prenom"));
                u.put("email",  rs.getString("email"));
                u.put("role",   rs.getString("role"));
                list.add(u);
            }
        } catch (SQLException e) {
            System.err.println("❌ getAllUsers error: " + e.getMessage());
        }
        return list;
    }

    public void deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id = ? AND role != 'admin'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ deleteUser error: " + e.getMessage());
        }
    }

    // ── QUESTIONS ────────────────────────────────────────────
    public List<Map<String, String>> getAllQuestions() {
        List<Map<String, String>> list = new ArrayList<>();
        String sql = "SELECT id, titre, description, categorie, user_id " +
                "FROM questions ORDER BY id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, String> q = new HashMap<>();
                q.put("id",          rs.getString("id"));
                q.put("titre",       rs.getString("titre"));
                q.put("description", rs.getString("description"));
                q.put("categorie",   rs.getString("categorie"));
                q.put("patientId",   rs.getString("user_id"));
                q.put("flagged",     "false");
                list.add(q);
            }
        } catch (SQLException e) {
            System.err.println("❌ getAllQuestions error: " + e.getMessage());
        }
        return list;
    }

    public void deleteQuestion(int questionId) {
        // Delete answers first (foreign key), then the question
        String deleteReponses = "DELETE FROM reponses WHERE question_id = ?";
        String deleteReports  = "DELETE FROM reports  WHERE question_id = ?";
        String deleteQuestion = "DELETE FROM questions WHERE id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(deleteReponses)) {
                    ps.setInt(1, questionId); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(deleteReports)) {
                    ps.setInt(1, questionId); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(deleteQuestion)) {
                    ps.setInt(1, questionId); ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("❌ deleteQuestion error: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("❌ connection error: " + e.getMessage());
        }
    }

    // ── REPONSES ─────────────────────────────────────────────
    public List<Map<String, String>> getAllReponses() {
        List<Map<String, String>> list = new ArrayList<>();
        String sql = "SELECT r.id, r.contenu, r.doctor_id as doctorId, " +
                "q.titre as questionTitre " +
                "FROM reponses r " +
                "JOIN questions q ON r.question_id = q.id " +
                "ORDER BY r.id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, String> r = new HashMap<>();
                r.put("id",            rs.getString("id"));
                r.put("contenu",       rs.getString("contenu"));
                r.put("doctorId",      rs.getString("doctorId"));
                r.put("questionTitre", rs.getString("questionTitre"));
                list.add(r);
            }
        } catch (SQLException e) {
            System.err.println("❌ getAllReponses error: " + e.getMessage());
        }
        return list;
    }

    public void deleteReponse(int reponseId) {
        String sql = "DELETE FROM reponses WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reponseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ deleteReponse error: " + e.getMessage());
        }
    }
    public void deleteFeedback(int feedbackId) {
        // Delete reports linked to this feedback first
        String deleteReports = "DELETE FROM reports WHERE feedback_id = ?";
        String deleteFeedback = "DELETE FROM feedback WHERE id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(deleteReports)) {
                ps.setInt(1, feedbackId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(deleteFeedback)) {
                ps.setInt(1, feedbackId);
                ps.executeUpdate();
            }
            System.out.println("✅ Feedback supprimé");
        } catch (SQLException e) {
            System.err.println("❌ deleteFeedback error: " + e.getMessage());
        }
    }
}