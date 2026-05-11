package services.yessine;

import models.User;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service utilisateurs — couche d'accès à la table `users`.
 * Remplace l'ancienne version minimaliste (login uniquement).
 * Compatible avec les colonnes originales + nouvelles colonnes Maraam.
 */
public class UserService {

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    /**
     * Tente un login email+password.
     * Vérifie aussi que le compte est actif (etat = TRUE ou NULL).
     * Retourne l'objet User complet, ou null si échec.
     */
    public User login(String email, String password) {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("Erreur login: " + e.getMessage());
        }
        return null;
    }

    /**
     * Méthode de compatibilité avec l'ancien LoginController.
     * Retourne String[] { id, nom, prenom, email, role, specialite }
     * @deprecated Utiliser login(email, password) qui retourne un User complet.
     */
    @Deprecated
    public String[] loginLegacy(String email, String password) {
        User u = login(email, password);
        if (u == null) return null;
        return new String[]{
                String.valueOf(u.getId()),
                u.getNom(),
                u.getPrenom(),
                u.getEmail(),
                u.getRole(),
                u.getSpecialite()
        };
    }

    // ── FIND ──────────────────────────────────────────────────────────────────

    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("Erreur findByEmail: " + e.getMessage());
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("Erreur findById: " + e.getMessage());
        }
        return null;
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id DESC";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("Erreur findAll: " + e.getMessage());
        }
        return list;
    }

    public boolean emailExists(String email, int excludeId) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ? AND id != ?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setInt(2, excludeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("Erreur emailExists: " + e.getMessage());
        }
        return false;
    }

    // ── SAVE (Register) ───────────────────────────────────────────────────────

    public void save(User u) {
        String sql = "INSERT INTO users " +
                "(nom, prenom, email, password, role, specialite, " +
                " username, photo, is_verified, etat, grade, adresse, description_specialite) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,  u.getNom());
            ps.setString(2,  u.getPrenom());
            ps.setString(3,  u.getEmail());
            ps.setString(4,  u.getPassword());
            ps.setString(5,  u.getRole());
            ps.setString(6,  u.getSpecialite());
            ps.setString(7,  u.getUsername());
            ps.setString(8,  u.getPhoto());
            ps.setBoolean(9, u.isVerified());
            ps.setObject(10, u.getEtat());
            ps.setString(11, u.getGrade());
            ps.setString(12, u.getAdresse());
            ps.setString(13, u.getDescriptionSpecialite());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur save user: " + e.getMessage(), e);
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    public void update(User u) {
        boolean changePass = u.getPassword() != null && !u.getPassword().isEmpty();
        String sql = "UPDATE users SET nom=?, prenom=?, email=?, role=?, specialite=?, " +
                "username=?, is_verified=?, etat=?, grade=?, adresse=?, description_specialite=?" +
                (changePass ? ", password=?" : "") +
                " WHERE id=?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, u.getNom());
            ps.setString(i++, u.getPrenom());
            ps.setString(i++, u.getEmail());
            ps.setString(i++, u.getRole());
            ps.setString(i++, u.getSpecialite());
            ps.setString(i++, u.getUsername());
            ps.setBoolean(i++, u.isVerified());
            ps.setObject(i++, u.getEtat());
            ps.setString(i++, u.getGrade());
            ps.setString(i++, u.getAdresse());
            ps.setString(i++, u.getDescriptionSpecialite());
            if (changePass) ps.setString(i++, u.getPassword());
            ps.setInt(i, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur update user: " + e.getMessage(), e);
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    public void delete(int id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur delete user: " + e.getMessage(), e);
        }
    }

    // ── 2FA ───────────────────────────────────────────────────────────────────

    /** Sauvegarde le secret TOTP dans la BDD après configuration 2FA */
    public void saveGoogleAuthSecret(int userId, String secret) {
        String sql = "UPDATE users SET google_authenticator_secret = ? WHERE id = ?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, secret);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur saveGoogleAuthSecret: " + e.getMessage(), e);
        }
    }

    /** Supprime le secret TOTP (désactiver 2FA) */
    public void remove2FA(int userId) {
        String sql = "UPDATE users SET google_authenticator_secret = NULL WHERE id = ?";
        try (Connection conn = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur remove2FA: " + e.getMessage(), e);
        }
    }

    // ── MAPPER ────────────────────────────────────────────────────────────────

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setRole(rs.getString("role"));
        u.setSpecialite(rs.getString("specialite"));

        // Colonnes ajoutées — entourées de try/catch car la migration
        // peut ne pas encore être faite sur certains environnements
        try { u.setUsername(rs.getString("username")); } catch (SQLException ignored) {}
        try { u.setPhoto(rs.getString("photo")); } catch (SQLException ignored) {}
        try { u.setVerified(rs.getBoolean("is_verified")); } catch (SQLException ignored) {}
        try { u.setEtat((Boolean) rs.getObject("etat")); } catch (SQLException ignored) {}
        try { u.setGrade(rs.getString("grade")); } catch (SQLException ignored) {}
        try { u.setAdresse(rs.getString("adresse")); } catch (SQLException ignored) {}
        try { u.setDescriptionSpecialite(rs.getString("description_specialite")); } catch (SQLException ignored) {}
        try { u.setGoogleAuthenticatorSecret(rs.getString("google_authenticator_secret")); } catch (SQLException ignored) {}

        return u;
    }
}
