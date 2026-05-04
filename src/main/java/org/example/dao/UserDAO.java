package org.example.dao;

import org.example.entity.User;
import org.example.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // CREATE
    public void save(User user) {
        String sql = "INSERT INTO user (email, roles, password, user_name, nom, prenom, photo, is_verified, specialite, description_specialite, adresse, grade, google_authenticator_secret, etat) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getRoles());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getUserName());
            ps.setString(5, user.getNom());
            ps.setString(6, user.getPrenom());
            ps.setString(7, user.getPhoto());
            ps.setBoolean(8, user.isVerified());
            ps.setString(9, user.getSpecialite());
            ps.setString(10, user.getDescriptionSpecialite());
            ps.setString(11, user.getAdresse());
            ps.setString(12, user.getGrade());
            ps.setString(13, user.getGoogleAuthenticatorSecret());
            ps.setObject(14, user.getEtat());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // READ by ID
    public User findById(int id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // READ by Email
    public User findByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // READ all
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // Alias for controllers
    public List<User> getAll() {
        return findAll();
    }

    // UPDATE
    public void update(User user) {
        String sql = "UPDATE user SET email=?, roles=?, password=?, user_name=?, nom=?, prenom=?, photo=?, is_verified=?, specialite=?, description_specialite=?, adresse=?, grade=?, google_authenticator_secret=?, etat=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getRoles());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getUserName());
            ps.setString(5, user.getNom());
            ps.setString(6, user.getPrenom());
            ps.setString(7, user.getPhoto());
            ps.setBoolean(8, user.isVerified());
            ps.setString(9, user.getSpecialite());
            ps.setString(10, user.getDescriptionSpecialite());
            ps.setString(11, user.getAdresse());
            ps.setString(12, user.getGrade());
            ps.setString(13, user.getGoogleAuthenticatorSecret());
            ps.setObject(14, user.getEtat());
            ps.setInt(15, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // DELETE
    public void delete(int id) {
        String sql = "DELETE FROM user WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // CHECK email exists (excluding current user)
    public boolean emailExists(String email, int excludeId) {
        String sql = "SELECT COUNT(*) FROM user WHERE email = ? AND id != ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, excludeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // MAP ResultSet → User
    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setEmail(rs.getString("email"));
        u.setRoles(rs.getString("roles"));
        u.setPassword(rs.getString("password"));
        u.setUserName(rs.getString("user_name"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setPhoto(rs.getString("photo"));
        u.setVerified(rs.getBoolean("is_verified"));
        u.setSpecialite(rs.getString("specialite"));
        u.setDescriptionSpecialite(rs.getString("description_specialite"));
        u.setAdresse(rs.getString("adresse"));
        u.setGrade(rs.getString("grade"));
        u.setGoogleAuthenticatorSecret(rs.getString("google_authenticator_secret"));
        u.setEtat((Boolean) rs.getObject("etat"));
        return u;
    }
}