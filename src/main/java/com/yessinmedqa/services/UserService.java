package com.yessinmedqa.services;

import com.yessinmedqa.config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserService {

    // Returns user data if login success, null if fail
    public String[] login(String email, String password) {
        String sql = "SELECT id, nom, prenom, email, role, specialite " +
                "FROM users WHERE email = ? AND mot_de_passe = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("specialite")
                };
            }

        } catch (SQLException e) {
            System.err.println("Erreur login: " + e.getMessage());
        }
        return null;
    }
}