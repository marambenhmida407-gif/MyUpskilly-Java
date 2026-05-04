package org.example.repository;

import org.example.entity.User;
import org.example.util.DatabaseConnection;
import java.sql.*;
import java.util.*;

public class UserRepositoryImpl implements UserRepository {

    @Override
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM user";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public User findById(int id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    @Override
    public boolean emailExists(String email, int excludeId) {
        String sql = "SELECT COUNT(*) FROM user WHERE email=? AND id!=?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, excludeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    @Override
    public void save(User u) {
        String sql = "INSERT INTO user (nom, prenom, email, username, password, roles, specialite, description_specialite, adresse, grade, is_verified, etat) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1,  u.getNom());
            ps.setString(2,  u.getPrenom());
            ps.setString(3,  u.getEmail());
            ps.setString(4,  u.getUserName());
            ps.setString(5,  u.getPassword());
            ps.setString(6,  u.getRoles());
            ps.setString(7,  u.getSpecialite());
            ps.setString(8,  u.getDescriptionSpecialite());
            ps.setString(9,  u.getAdresse());
            ps.setString(10, u.getGrade());
            ps.setBoolean(11, u.isVerified());
            ps.setObject(12, u.getEtat());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void update(User u) {
        boolean changePass = u.getPassword() != null && !u.getPassword().isEmpty();
        String sql = String.format(
                "UPDATE user SET nom=?, prenom=?, email=?, username=?, roles=?, specialite=?, description_specialite=?, adresse=?, grade=?, is_verified=?, etat=? %s WHERE id=?",
                changePass ? ", password=?" : ""
        );
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, u.getNom());
            ps.setString(i++, u.getPrenom());
            ps.setString(i++, u.getEmail());
            ps.setString(i++, u.getUserName());
            ps.setString(i++, u.getRoles());
            ps.setString(i++, u.getSpecialite());
            ps.setString(i++, u.getDescriptionSpecialite());
            ps.setString(i++, u.getAdresse());
            ps.setString(i++, u.getGrade());
            ps.setBoolean(i++, u.isVerified());
            ps.setObject(i++, u.getEtat());
            if (changePass) ps.setString(i++, u.getPassword());
            ps.setInt(i, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM user WHERE id = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setUserName(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setRoles(rs.getString("roles"));
        u.setSpecialite(rs.getString("specialite"));
        u.setDescriptionSpecialite(rs.getString("description_specialite"));
        u.setAdresse(rs.getString("adresse"));
        u.setGrade(rs.getString("grade"));
        u.setVerified(rs.getBoolean("is_verified"));
        Object etat = rs.getObject("etat");
        u.setEtat(etat != null ? (Boolean) etat : null);
        return u;
    }
}