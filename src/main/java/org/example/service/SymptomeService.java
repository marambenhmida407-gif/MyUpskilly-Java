package org.example.service;

import org.example.model.Symptome;
import org.example.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SymptomeService {

    public List<Symptome> getAll() {
        List<Symptome> list = new ArrayList<>();
        String sql = "SELECT s.*, p.nom AS patologie_nom FROM symptome s LEFT JOIN patologie p ON s.patologie_id = p.id";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Symptome> getByPathologieId(int patologieId) {
        List<Symptome> list = new ArrayList<>();
        String sql = "SELECT s.*, p.nom AS patologie_nom FROM symptome s LEFT JOIN patologie p ON s.patologie_id = p.id WHERE s.patologie_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patologieId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapResultSet(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void save(Symptome s) {
        String sql = "INSERT INTO symptome (nom, description, patologie_id) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getNom());
            ps.setString(2, s.getDescription());
            if (s.getPatologieId() != null) ps.setInt(3, s.getPatologieId());
            else ps.setNull(3, Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void update(Symptome s) {
        String sql = "UPDATE symptome SET nom=?, description=?, patologie_id=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getNom());
            ps.setString(2, s.getDescription());
            if (s.getPatologieId() != null) ps.setInt(3, s.getPatologieId());
            else ps.setNull(3, Types.INTEGER);
            ps.setInt(4, s.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void delete(int id) {
        String sql = "DELETE FROM symptome WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private Symptome mapResultSet(ResultSet rs) throws SQLException {
        Symptome s = new Symptome();
        s.setId(rs.getInt("id"));
        s.setNom(rs.getString("nom"));
        s.setDescription(rs.getString("description"));
        int patId = rs.getInt("patologie_id");
        s.setPatologieId(rs.wasNull() ? null : patId);
        s.setPatologieNom(rs.getString("patologie_nom"));
        return s;
    }
}