package org.example.service;

import org.example.model.Pathologie;
import org.example.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PathologieService {

    public List<Pathologie> getAll() {
        List<Pathologie> list = new ArrayList<>();
        String sql = "SELECT * FROM patologie";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Pathologie getById(int id) {
        String sql = "SELECT * FROM patologie WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void save(Pathologie p) {
        String sql = "INSERT INTO patologie (user_id, nom, description, type, gravite) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getUserId());
            ps.setString(2, p.getNom());
            ps.setString(3, p.getDescription());
            ps.setString(4, p.getType());
            ps.setString(5, p.getGravite());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(Pathologie p) {
        String sql = "UPDATE patologie SET user_id=?, nom=?, description=?, type=?, gravite=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getUserId());
            ps.setString(2, p.getNom());
            ps.setString(3, p.getDescription());
            ps.setString(4, p.getType());
            ps.setString(5, p.getGravite());
            ps.setInt(6, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM patologie WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Pathologie mapResultSet(ResultSet rs) throws SQLException {
        Pathologie p = new Pathologie();
        p.setId(rs.getInt("id"));
        p.setUserId(rs.getInt("user_id"));
        p.setNom(rs.getString("nom"));
        p.setDescription(rs.getString("description"));
        p.setType(rs.getString("type"));
        p.setGravite(rs.getString("gravite"));
        return p;
    }
}