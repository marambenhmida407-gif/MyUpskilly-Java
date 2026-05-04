package com.yessinmedqa.services;

import com.yessinmedqa.config.DBConnection;

import java.sql.*;
import java.util.*;

public class ModerationService {

    // ── PATIENT MANUAL REPORT ────────────────────────────────
    public void reportQuestion(int questionId, int reporterId, String raison) {
        insertReport(questionId, reporterId, raison, "manuel");
    }

    // ── AI AUTO-MODERATION ───────────────────────────────────
    public void autoModerate(int questionId, String titre, String description) {
        new Thread(() -> {
            AIService ai = new AIService();
            boolean toxic = ai.isToxic(titre + " " + description);
            if (toxic) {
                insertReport(questionId, 0,
                        "Contenu inapproprié détecté automatiquement par IA", "ai");
                System.out.println("🚨 Auto-flagged question: " + questionId);
            }
        }).start();
    }

    // ── INSERT REPORT ────────────────────────────────────────
    private void insertReport(int questionId, int reporterId,
                              String raison, String type) {
        String sql = "INSERT INTO reports (question_id, reporter_id, raison, type) " +
                "VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            if (reporterId == 0) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, reporterId);
            ps.setString(3, raison);
            ps.setString(4, type);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ insertReport error: " + e.getMessage());
        }
    }

    // ── GET ALL REPORTS — used by AdminController ────────────
    public List<Map<String, String>> getAllReports() {
        List<Map<String, String>> list = new ArrayList<>();

        String sql =
                "SELECT r.id, r.raison, r.type, r.statut, r.created_at, " +
                        "r.question_id, r.feedback_id, " +
                        "q.titre as question_titre, " +
                        "f.commentaire as feedback_commentaire " +
                        "FROM reports r " +
                        "LEFT JOIN questions q ON r.question_id = q.id " +
                        "LEFT JOIN feedback f ON r.feedback_id = f.id " +
                        "ORDER BY r.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, String> map = new HashMap<>();
                map.put("id",                  rs.getString("id"));
                map.put("raison",              rs.getString("raison"));
                map.put("type",                rs.getString("type") != null
                        ? rs.getString("type") : "manuel");
                map.put("status",              rs.getString("statut"));
                map.put("date",                rs.getString("created_at"));
                map.put("questionId",          rs.getString("question_id"));
                map.put("feedbackId",          rs.getString("feedback_id"));
                map.put("questionTitre",       rs.getString("question_titre"));
                map.put("feedbackCommentaire", rs.getString("feedback_commentaire"));
                list.add(map);
            }
        } catch (SQLException e) {
            System.err.println("❌ getAllReports error: " + e.getMessage());
        }
        return list;
    }

    // ── UPDATE REPORT STATUS — used by AdminController ───────
    public void updateReportStatus(int reportId, String status) {
        String sql = "UPDATE reports SET statut = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, reportId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ updateReportStatus error: " + e.getMessage());
        }
    }

    // ── DELETE QUESTION + MARK REPORT REVIEWED ───────────────
    public void supprimerQuestion(int questionId, int reportId) {
        String sqlQ = "DELETE FROM questions WHERE id = ?";
        String sqlR = "UPDATE reports SET statut = 'supprime' WHERE id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlQ)) {
                ps.setInt(1, questionId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlR)) {
                ps.setInt(1, reportId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("❌ supprimerQuestion error: " + e.getMessage());
        }
    }

    // ── APPROVE REPORT ───────────────────────────────────────
    public void approuver(int reportId) {
        updateReportStatus(reportId, "approuve");
    }
}