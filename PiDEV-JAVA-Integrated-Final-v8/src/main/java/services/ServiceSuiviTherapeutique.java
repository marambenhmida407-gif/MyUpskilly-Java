package services;

import models.SuiviTherapeutique;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;

public class ServiceSuiviTherapeutique {

    public void ajouter(SuiviTherapeutique s) throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        String req = "INSERT INTO suivi_therapeutique (patient_id, date_debut, date_fin, type_suivi, objectif_therapeutique, statut) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, s.getPatientId());
        ps.setTimestamp(2, new Timestamp(s.getDateDebut().getTime()));
        ps.setTimestamp(3, s.getDateFin() != null ? new Timestamp(s.getDateFin().getTime()) : null);
        ps.setString(4, s.getTypeSuivi());
        ps.setString(5, s.getObjectifTherapeutique());
        ps.setString(6, s.getStatut());
        ps.executeUpdate();
    }

    public ArrayList<SuiviTherapeutique> afficherAll() throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        ArrayList<SuiviTherapeutique> list = new ArrayList<>();
        String sql = "SELECT s.*, CONCAT(u.prenom, ' ', u.nom) AS patient_nom " +
                     "FROM suivi_therapeutique s " +
                     "LEFT JOIN users u ON s.patient_id = u.id";
        ResultSet rs = conn.createStatement().executeQuery(sql);
        while (rs.next()) {
            list.add(new SuiviTherapeutique(
                rs.getInt("id"),
                rs.getInt("patient_id"),
                rs.getString("patient_nom"),
                rs.getTimestamp("date_debut"),
                rs.getTimestamp("date_fin"),
                rs.getString("type_suivi"),
                rs.getString("objectif_therapeutique"),
                rs.getString("statut")
            ));
        }
        return list;
    }

    public void modifier(SuiviTherapeutique s) throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        String req = "UPDATE suivi_therapeutique SET patient_id=?, date_debut=?, date_fin=?, type_suivi=?, objectif_therapeutique=?, statut=? WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, s.getPatientId());
        ps.setTimestamp(2, new Timestamp(s.getDateDebut().getTime()));
        ps.setTimestamp(3, s.getDateFin() != null ? new Timestamp(s.getDateFin().getTime()) : null);
        ps.setString(4, s.getTypeSuivi());
        ps.setString(5, s.getObjectifTherapeutique());
        ps.setString(6, s.getStatut());
        ps.setInt(7, s.getId());
        ps.executeUpdate();
    }

    public void supprimer(int id) throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        PreparedStatement ps = conn.prepareStatement("DELETE FROM suivi_therapeutique WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
