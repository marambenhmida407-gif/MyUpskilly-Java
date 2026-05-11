package services;

import models.PrescriptionMedicale;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class ServicePrescriptionMedicale {

    public void ajouter(PrescriptionMedicale p) throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        String req = "INSERT INTO prescription_medicale (date_prescription, medicaments, recommandations, suivi, suivi_therapeutique_id) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setTimestamp(1, new Timestamp(p.getDatePrescription().getTime()));
        ps.setString(2, p.getMedicaments());
        ps.setString(3, p.getRecommandations());
        ps.setString(4, p.getSuivi());
        ps.setInt(5, p.getSuiviTherapeutiqueId());
        ps.executeUpdate();
    }

    public ArrayList<PrescriptionMedicale> afficherAll() throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        ArrayList<PrescriptionMedicale> list = new ArrayList<>();
        ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM prescription_medicale");
        while (rs.next()) {
            list.add(new PrescriptionMedicale(
                rs.getInt("id"),
                rs.getTimestamp("date_prescription"),
                rs.getString("medicaments"),
                rs.getString("recommandations"),
                rs.getString("suivi"),
                rs.getInt("suivi_therapeutique_id")
            ));
        }
        return list;
    }

    // READ BY SUIVI ID — used by PDF export
    public ArrayList<PrescriptionMedicale> afficherParSuivi(int suiviId) throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        ArrayList<PrescriptionMedicale> list = new ArrayList<>();
        String req = "SELECT * FROM prescription_medicale WHERE suivi_therapeutique_id = ?";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setInt(1, suiviId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(new PrescriptionMedicale(
                    rs.getInt("id"),
                    rs.getTimestamp("date_prescription"),
                    rs.getString("medicaments"),
                    rs.getString("recommandations"),
                    rs.getString("suivi"),
                    rs.getInt("suivi_therapeutique_id")
            ));
        }
        return list;
    }

    public void modifier(PrescriptionMedicale p) throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        String req = "UPDATE prescription_medicale SET date_prescription=?, medicaments=?, recommandations=?, suivi=?, suivi_therapeutique_id=? WHERE id=?";
        PreparedStatement ps = conn.prepareStatement(req);
        ps.setTimestamp(1, new Timestamp(p.getDatePrescription().getTime()));
        ps.setString(2, p.getMedicaments());
        ps.setString(3, p.getRecommandations());
        ps.setString(4, p.getSuivi());
        ps.setInt(5, p.getSuiviTherapeutiqueId());
        ps.setInt(6, p.getId());
        ps.executeUpdate();
    }

    public void supprimer(int id) throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        PreparedStatement ps = conn.prepareStatement("DELETE FROM prescription_medicale WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
