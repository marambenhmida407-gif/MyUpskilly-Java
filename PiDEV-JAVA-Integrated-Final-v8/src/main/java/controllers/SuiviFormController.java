package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.SuiviTherapeutique;
import services.ServiceSuiviTherapeutique;
import services.aziz.UserService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class SuiviFormController implements Initializable {

    @FXML private ComboBox<String> cbPatient;
    @FXML private DatePicker dpDateDebut;
    @FXML private DatePicker dpDateFin;
    @FXML private TextField tfTypeSuivi;
    @FXML private TextField tfObjectif;
    @FXML private ComboBox<String> cbStatut;

    @FXML private Label lblErrorPatient;
    @FXML private Label lblErrorDateDebut;
    @FXML private Label lblErrorDateFin;
    @FXML private Label lblErrorTypeSuivi;
    @FXML private Label lblErrorObjectif;
    @FXML private Label lblErrorStatut;

    private ServiceSuiviTherapeutique service = new ServiceSuiviTherapeutique();
    private UserService userService = new UserService();
    private SuiviTherapeutique suivi = null;
    /** Internal list of patients loaded from DB, parallel to cbPatient items */
    private List<Map<String, Object>> patients;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbStatut.getItems().addAll("En cours", "Terminé", "Suspendu", "Planifié");
        loadPatients();
        clearErrors();
        dpDateDebut.valueProperty().addListener((obs, o, newVal) -> validateDateDebut(newVal));
        dpDateFin.valueProperty().addListener((obs, o, newVal) -> validateDateFin(newVal));
        tfTypeSuivi.textProperty().addListener((obs, o, n) -> { if (!n.isEmpty()) lblErrorTypeSuivi.setText(""); });
        tfObjectif.textProperty().addListener((obs, o, n) -> { if (!n.isEmpty()) lblErrorObjectif.setText(""); });
        cbStatut.valueProperty().addListener((obs, o, n) -> { if (n != null) lblErrorStatut.setText(""); });
        cbPatient.valueProperty().addListener((obs, o, n) -> { if (n != null) lblErrorPatient.setText(""); });
    }

    private void loadPatients() {
        patients = userService.getAll();  // returns users with role='patient'
        cbPatient.getItems().clear();
        patients.forEach(u -> cbPatient.getItems().add(
                u.get("id") + " - " + u.get("prenom") + " " + u.get("nom")
        ));
    }

    public void setSuivi(SuiviTherapeutique s) {
        this.suivi = s;
        // Pre-select patient
        patients.stream()
                .filter(u -> ((int) u.get("id")) == s.getPatientId())
                .findFirst()
                .ifPresent(u -> cbPatient.setValue(
                        u.get("id") + " - " + u.get("prenom") + " " + u.get("nom")));
        if (s.getDateDebut() != null)
            dpDateDebut.setValue(s.getDateDebut().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        if (s.getDateFin() != null)
            dpDateFin.setValue(s.getDateFin().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        tfTypeSuivi.setText(s.getTypeSuivi());
        tfObjectif.setText(s.getObjectifTherapeutique());
        cbStatut.setValue(s.getStatut());
    }

    /** Returns the selected patient's DB id, or -1 if none selected */
    private int getSelectedPatientId() {
        String val = cbPatient.getValue();
        if (val == null) return -1;
        try {
            return Integer.parseInt(val.split(" - ")[0].trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean validatePatient() {
        if (getSelectedPatientId() < 0) {
            lblErrorPatient.setText("⚠ Veuillez sélectionner un patient.");
            return false;
        }
        lblErrorPatient.setText(""); return true;
    }

    private boolean validateDateDebut(LocalDate value) {
        if (value == null) {
            lblErrorDateDebut.setText("⚠ La date de début est obligatoire.");
            return false;
        }
        if (dpDateFin.getValue() != null && value.isAfter(dpDateFin.getValue())) {
            lblErrorDateDebut.setText("⚠ La date de début ne peut pas dépasser la date de fin.");
            return false;
        }
        lblErrorDateDebut.setText("");
        if (dpDateFin.getValue() != null) validateDateFin(dpDateFin.getValue());
        return true;
    }

    private boolean validateDateFin(LocalDate value) {
        if (value == null) { lblErrorDateFin.setText(""); return true; }
        if (dpDateDebut.getValue() != null && value.isBefore(dpDateDebut.getValue())) {
            lblErrorDateFin.setText("⚠ La date de fin ne peut pas être avant la date de début.");
            return false;
        }
        lblErrorDateFin.setText("");
        return true;
    }

    private boolean validateTypeSuivi() {
        String val = tfTypeSuivi.getText().trim();
        if (val.isEmpty()) { lblErrorTypeSuivi.setText("⚠ Champ obligatoire."); return false; }
        if (val.length() < 3) { lblErrorTypeSuivi.setText("⚠ Minimum 3 caractères."); return false; }
        lblErrorTypeSuivi.setText(""); return true;
    }

    private boolean validateObjectif() {
        String val = tfObjectif.getText().trim();
        if (val.isEmpty()) { lblErrorObjectif.setText("⚠ Champ obligatoire."); return false; }
        if (val.length() < 5) { lblErrorObjectif.setText("⚠ Minimum 5 caractères."); return false; }
        lblErrorObjectif.setText(""); return true;
    }

    private boolean validateStatut() {
        if (cbStatut.getValue() == null) { lblErrorStatut.setText("⚠ Veuillez sélectionner un statut."); return false; }
        lblErrorStatut.setText(""); return true;
    }

    private boolean validateAll() {
        boolean p  = validatePatient();
        boolean d1 = validateDateDebut(dpDateDebut.getValue());
        boolean d2 = validateDateFin(dpDateFin.getValue());
        boolean t  = validateTypeSuivi();
        boolean o  = validateObjectif();
        boolean s  = validateStatut();
        return p && d1 && d2 && t && o && s;
    }

    private void clearErrors() {
        lblErrorPatient.setText("");
        lblErrorDateDebut.setText("");
        lblErrorDateFin.setText("");
        lblErrorTypeSuivi.setText("");
        lblErrorObjectif.setText("");
        lblErrorStatut.setText("");
    }

    @FXML
    private void handleSave() {
        if (!validateAll()) return;
        int patientId = getSelectedPatientId();
        Date dateDebut = Date.from(dpDateDebut.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date dateFin = dpDateFin.getValue() != null
                ? Date.from(dpDateFin.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
        try {
            if (suivi == null) {
                SuiviTherapeutique s = new SuiviTherapeutique();
                s.setPatientId(patientId);
                s.setDateDebut(dateDebut);
                s.setDateFin(dateFin);
                s.setTypeSuivi(tfTypeSuivi.getText().trim());
                s.setObjectifTherapeutique(tfObjectif.getText().trim());
                s.setStatut(cbStatut.getValue());
                service.ajouter(s);
            } else {
                suivi.setPatientId(patientId);
                suivi.setDateDebut(dateDebut);
                suivi.setDateFin(dateFin);
                suivi.setTypeSuivi(tfTypeSuivi.getText().trim());
                suivi.setObjectifTherapeutique(tfObjectif.getText().trim());
                suivi.setStatut(cbStatut.getValue());
                service.modifier(suivi);
            }
            closeWindow();
        } catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
    }

    @FXML private void handleCancel() { closeWindow(); }
    private void closeWindow() { ((Stage) tfTypeSuivi.getScene().getWindow()).close(); }
    private void showAlert(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(t); a.setContentText(m); a.showAndWait();
    }
}