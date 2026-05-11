package services.yessine;

import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles doctor recommendations for out-of-speciality questions.
 * Plain JDBC — no DAO — same pattern as other services.
 */
public class RecommandationService {

    // Get all doctors except the current one
    // Get doctors filtered by speciality — case insensitive
    public List<String[]> getDoctorsBySpecialite(String specialite, int excludeId) {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT id, nom, prenom, specialite " +
                "FROM users " +
                "WHERE role = 'doctor' AND id != ? " +
                "AND LOWER(specialite) = LOWER(?) " +
                "ORDER BY nom";

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, excludeId);
            ps.setString(2, specialite);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                        rs.getString("id"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("specialite") != null
                                ? rs.getString("specialite") : "Médecin Généraliste"
                });
            }
        } catch (SQLException e) {
            System.err.println("❌ getDoctorsBySpecialite error: " + e.getMessage());
        }
        return list;
    }



    // Save recommendation
    public void recommander(int questionId, int fromDoctorId,
                            int toDoctorId, String message) {
        String sql = "INSERT INTO recommandations " +
                "(question_id, from_doctor_id, to_doctor_id, message) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ps.setInt(2, fromDoctorId);
            ps.setInt(3, toDoctorId);
            ps.setString(4, message);
            ps.executeUpdate();
            System.out.println("✅ Recommandation envoyée");
        } catch (SQLException e) {
            System.err.println("❌ recommander error: " + e.getMessage());
        }
    }

    // Check if question was already recommended
    public boolean dejaRecommande(int questionId, int fromDoctorId) {
        String sql = "SELECT COUNT(*) FROM recommandations " +
                "WHERE question_id = ? AND from_doctor_id = ?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ps.setInt(2, fromDoctorId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("❌ dejaRecommande error: " + e.getMessage());
        }
        return false;
    }
    // Get all recommendations sent TO a specific doctor
    public List<String[]> getRecommandationsForDoctor(int doctorId) {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT r.id, r.message, r.created_at, " +
                "q.id as qid, q.titre, q.description, q.categorie, " +
                "u.nom as from_nom, u.prenom as from_prenom " +
                "FROM recommandations r " +
                "JOIN questions q ON r.question_id = q.id " +
                "JOIN users u ON r.from_doctor_id = u.id " +
                "WHERE r.to_doctor_id = ? " +
                "AND q.statut = 'en_attente' " +
                "ORDER BY r.created_at DESC";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                        rs.getString("id"),
                        rs.getString("qid"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getString("categorie"),
                        rs.getString("message"),
                        rs.getString("from_nom"),
                        rs.getString("from_prenom"),
                        rs.getString("created_at")
                });
            }
        } catch (SQLException e) {
            System.err.println("❌ getRecommandationsForDoctor error: " + e.getMessage());
        }
        return list;
    }
}