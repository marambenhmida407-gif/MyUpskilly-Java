package services.aziz;

import models.aziz.Consultation;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConsultationService {

    public List<Consultation> getAll() {
        List<Consultation> list = new ArrayList<>();
        String sql = "SELECT c.*, p.nom AS pathologie_nom, " +
                "CONCAT(u.prenom, ' ', u.nom) AS patient_nom, u.email AS patient_email " +
                "FROM consultation c " +
                "LEFT JOIN patologie p ON c.patologie_id = p.id " +
                "LEFT JOIN users u ON c.patient_id = u.id";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public Consultation getById(int id) {
        String sql = "SELECT c.*, p.nom AS pathologie_nom, " +
                "CONCAT(u.prenom, ' ', u.nom) AS patient_nom, u.email AS patient_email " +
                "FROM consultation c " +
                "LEFT JOIN patologie p ON c.patologie_id = p.id " +
                "LEFT JOIN users u ON c.patient_id = u.id WHERE c.id = ?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public void save(Consultation c) {
        String sql = "INSERT INTO consultation (patient_id, patologie_id, date_consultation, motif, diagnostic, observations, ordonnance, statut) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, c.getPatientId());
            if (c.getPathologieId() != null) ps.setInt(2, c.getPathologieId());
            else ps.setNull(2, Types.INTEGER);
            ps.setTimestamp(3, Timestamp.valueOf(c.getDateConsultation()));
            ps.setString(4, c.getMotif());
            ps.setString(5, c.getDiagnostic());
            ps.setString(6, c.getObservations());
            ps.setString(7, c.getOrdonnance());
            ps.setString(8, c.getStatut());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void update(Consultation c) {
        String sql = "UPDATE consultation SET patient_id=?, patologie_id=?, date_consultation=?, motif=?, diagnostic=?, observations=?, ordonnance=?, statut=? WHERE id=?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, c.getPatientId());
            if (c.getPathologieId() != null) ps.setInt(2, c.getPathologieId());
            else ps.setNull(2, Types.INTEGER);
            ps.setTimestamp(3, Timestamp.valueOf(c.getDateConsultation()));
            ps.setString(4, c.getMotif());
            ps.setString(5, c.getDiagnostic());
            ps.setString(6, c.getObservations());
            ps.setString(7, c.getOrdonnance());
            ps.setString(8, c.getStatut());
            ps.setInt(9, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void delete(int id) {
        String sql = "DELETE FROM consultation WHERE id = ?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private Consultation mapResultSet(ResultSet rs) throws SQLException {
        Consultation c = new Consultation();
        c.setId(rs.getInt("id"));
        c.setPatientId(rs.getInt("patient_id"));
        c.setPatientNom(rs.getString("patient_nom"));
        c.setPatientEmail(rs.getString("patient_email"));
        int patId = rs.getInt("patologie_id");
        c.setPathologieId(rs.wasNull() ? null : patId);
        Timestamp ts = rs.getTimestamp("date_consultation");
        c.setDateConsultation(ts != null ? ts.toLocalDateTime() : null);
        c.setMotif(rs.getString("motif"));
        c.setDiagnostic(rs.getString("diagnostic"));
        c.setObservations(rs.getString("observations"));
        c.setOrdonnance(rs.getString("ordonnance"));
        c.setStatut(rs.getString("statut"));
        c.setPathologieNom(rs.getString("pathologie_nom"));
        return c;
    }
}