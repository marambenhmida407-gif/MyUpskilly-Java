package services.yessine;

import utils.MyDatabase;
import models.yessine.Question;
import models.yessine.Reponse;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
// Session management will be handled by the main project

public class ReponseService {

    public List<Reponse> getRepondues(String keyword, String categorie) {
        List<Reponse> list = new ArrayList<>();

        // ✅ GROUP BY question — only return ONE row per question
        // (the most recent response for display purposes)
        String sql = "SELECT r.id, r.contenu, r.question_id, r.doctor_id, " +
                "r.updated_at, " +
                "q.titre, q.description, q.fichier_chemin, q.categorie, q.user_id, " +
                "u.nom, u.prenom, u.specialite " +
                "FROM reponses r " +
                "JOIN questions q ON r.question_id = q.id " +
                "JOIN users u ON r.doctor_id = u.id " +
                "WHERE r.id = (" +
                "  SELECT MIN(id) FROM reponses r2 WHERE r2.question_id = q.id" +
                ") " +
                "AND (q.titre LIKE ? OR q.description LIKE ?) " +
                (categorie != null ? "AND q.categorie = ? " : "") +
                "ORDER BY q.id DESC";

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String search = "%" + (keyword == null ? "" : keyword) + "%";
            ps.setString(1, search);
            ps.setString(2, search);
            if (categorie != null) ps.setString(3, categorie);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Reponse rep = new Reponse();
                rep.setId(rs.getInt("id"));
                rep.setContenu(rs.getString("contenu"));
                rep.setQuestionId(rs.getInt("question_id"));
                rep.setDoctorId(rs.getInt("doctor_id"));
                rep.setQuestionTitre(rs.getString("titre"));
                rep.setQuestionDescription(rs.getString("description"));
                rep.setDoctorNom(rs.getString("nom"));
                rep.setDoctorPrenom(rs.getString("prenom"));
                rep.setDoctorSpecialite(rs.getString("specialite"));
                rep.setFichierChemin(rs.getString("fichier_chemin"));
                rep.setCategorie(rs.getString("categorie"));
                rep.setQuestionUserId(rs.getInt("user_id"));
                rep.setUpdatedAt(rs.getString("updated_at"));
                list.add(rep);
            }

        } catch (SQLException e) {
            System.err.println("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    public List<String[]> getFeedbacks(int reponseId) {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT id, rating, commentaire, user_nom, created_at " +
                "FROM feedback WHERE reponse_id = ? ORDER BY created_at DESC";

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, reponseId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String[] row = new String[5];
                row[0] = String.valueOf(rs.getInt("rating"));
                row[1] = rs.getString("commentaire");
                row[2] = rs.getString("user_nom") != null
                        ? rs.getString("user_nom") : "Anonyme";
                row[3] = rs.getString("created_at");
                row[4] = rs.getString("id"); // ✅ feedback ID for reporting
                list.add(row);
            }

        } catch (SQLException e) {
            System.err.println("Erreur feedbacks: " + e.getMessage());
        }
        return list;
    }


    public void insert(Reponse r) {
        // ✅ Check if this doctor already answered this question
        String checkSql = "SELECT id FROM reponses WHERE question_id = ? AND doctor_id = ?";
        String insertSql = "INSERT INTO reponses (question_id, doctor_id, contenu) VALUES (?, ?, ?)";
        String updateSql = "UPDATE reponses SET contenu = ?, updated_at = NOW() " +
                "WHERE question_id = ? AND doctor_id = ?";
        String statusSql = "UPDATE questions SET statut = 'repondu' WHERE id = ?";

        try (Connection conn = MyDatabase.getInstance().getConnection()) {

            // Check if already answered by this doctor
            PreparedStatement check = conn.prepareStatement(checkSql);
            check.setInt(1, r.getQuestionId());
            check.setInt(2, r.getDoctorId());
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                // ✅ Already answered — UPDATE instead of INSERT
                PreparedStatement update = conn.prepareStatement(updateSql);
                update.setString(1, r.getContenu());
                update.setInt(2, r.getQuestionId());
                update.setInt(3, r.getDoctorId());
                update.executeUpdate();
                System.out.println("✅ Réponse mise à jour");
            } else {
                // ✅ New answer — INSERT
                PreparedStatement insert = conn.prepareStatement(insertSql);
                insert.setInt(1, r.getQuestionId());
                insert.setInt(2, r.getDoctorId());
                insert.setString(3, r.getContenu());
                insert.executeUpdate();
                System.out.println("✅ Nouvelle réponse insérée");

                // Mark question as answered
                PreparedStatement status = conn.prepareStatement(statusSql);
                status.setInt(1, r.getQuestionId());
                status.executeUpdate();
            }

        } catch (SQLException e) {
            System.err.println("❌ insert error: " + e.getMessage());
        }
    }

    public List<Question> getEnAttente() {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM questions WHERE statut = 'en_attente'";

        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Question q = new Question();
                q.setId(rs.getInt("id"));
                q.setTitre(rs.getString("titre"));
                q.setDescription(rs.getString("description"));
                q.setFichierChemin(rs.getString("fichier_chemin"));
                q.setCategorie(rs.getString("categorie"));
                q.setSousTraitement(rs.getBoolean("sous_traitement"));
                q.setaDesAllergies(rs.getBoolean("a_des_allergies"));
                q.setTaille(rs.getDouble("taille"));
                q.setPoids(rs.getDouble("poids"));
                q.setDescriptionTraitement(rs.getString("description_traitement"));
                q.setDescriptionAllergies(rs.getString("description_allergies"));
                list.add(q);
                System.out.println("✅ Question: " + q.getTitre()
                        + " | taille=" + q.getTaille()
                        + " | poids=" + q.getPoids());
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }
    public void deleteQuestion(int questionId) {
        String deleteReponses = "DELETE FROM reponses WHERE question_id = ?";
        String deleteQuestion = "DELETE FROM questions WHERE id = ?";

        try (Connection conn = MyDatabase.getInstance().getConnection()) {

            // 1. delete linked responses first (important for FK)
            PreparedStatement ps1 = conn.prepareStatement(deleteReponses);
            ps1.setInt(1, questionId);
            ps1.executeUpdate();

            // 2. delete question
            PreparedStatement ps2 = conn.prepareStatement(deleteQuestion);
            ps2.setInt(1, questionId);
            ps2.executeUpdate();

            System.out.println("✅ Question supprimée");

        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression: " + e.getMessage());
        }
    }
    // Update existing answer — only if same doctor
    public void updateReponse(int reponseId, int doctorId, String newContenu) {
        String sql = "UPDATE reponses SET contenu = ?, updated_at = NOW() " +
                "WHERE id = ? AND doctor_id = ?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newContenu);
            ps.setInt(2, reponseId);
            ps.setInt(3, doctorId);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0
                    ? "✅ Réponse modifiée"
                    : "❌ Non autorisé ou réponse introuvable");
        } catch (SQLException e) {
            System.err.println("❌ updateReponse error: " + e.getMessage());
        }
    }

    // Add a new answer on already answered question
    public void addAnotherReponse(int questionId, int doctorId, String contenu) {
        // Just insert — don't change question statut (already repondu)
        String sql = "INSERT INTO reponses (question_id, doctor_id, contenu) VALUES (?, ?, ?)";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ps.setInt(2, doctorId);
            ps.setString(3, contenu);
            ps.executeUpdate();
            System.out.println("✅ Nouvelle réponse ajoutée");
        } catch (SQLException e) {
            System.err.println("❌ addAnotherReponse error: " + e.getMessage());
        }
    }
    // Get all responses for a specific question
    public List<Reponse> getReponsesByQuestion(int questionId) {
        List<Reponse> list = new ArrayList<>();
        String sql = "SELECT r.id, r.contenu, r.question_id, r.doctor_id, " +
                "r.updated_at, " +
                "u.nom, u.prenom, u.specialite, " +
                "q.titre, q.description, q.fichier_chemin, " +
                "q.categorie, q.user_id " +
                "FROM reponses r " +
                "JOIN users u ON r.doctor_id = u.id " +
                "JOIN questions q ON r.question_id = q.id " +
                "WHERE r.question_id = ? " +
                "ORDER BY r.id ASC";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Reponse rep = new Reponse();
                rep.setId(rs.getInt("id"));
                rep.setContenu(rs.getString("contenu"));
                rep.setQuestionId(rs.getInt("question_id"));
                rep.setDoctorId(rs.getInt("doctor_id"));
                rep.setUpdatedAt(rs.getString("updated_at"));
                rep.setDoctorNom(rs.getString("nom"));
                rep.setDoctorPrenom(rs.getString("prenom"));
                rep.setDoctorSpecialite(rs.getString("specialite"));
                // ✅ These were missing — needed by VoirReponseController
                rep.setQuestionTitre(rs.getString("titre"));
                rep.setQuestionDescription(rs.getString("description"));
                rep.setFichierChemin(rs.getString("fichier_chemin"));
                rep.setCategorie(rs.getString("categorie"));
                rep.setQuestionUserId(rs.getInt("user_id"));
                list.add(rep);
            }
        } catch (SQLException e) {
            System.err.println("❌ getReponsesByQuestion error: " + e.getMessage());
        }
        return list;
    }

}
