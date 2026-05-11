package services.aziz;

import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserService {

    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT id, nom, prenom, email FROM users WHERE role = 'patient'";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", rs.getInt("id"));
                user.put("nom", rs.getString("nom") != null ? rs.getString("nom") : "");
                user.put("prenom", rs.getString("prenom") != null ? rs.getString("prenom") : "");
                user.put("email", rs.getString("email") != null ? rs.getString("email") : "");
                list.add(user);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public String getEmailById(int id) {
        String sql = "SELECT email FROM users WHERE id = ?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("email");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }
}