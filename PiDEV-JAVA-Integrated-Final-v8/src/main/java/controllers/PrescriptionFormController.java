package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.PrescriptionMedicale;
import models.SuiviTherapeutique;
import services.ServicePrescriptionMedicale;
import services.ServiceSuiviTherapeutique;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.ResourceBundle;

public class PrescriptionFormController implements Initializable {

    @FXML private DatePicker dpDatePrescription;
    @FXML private TextField  tfMedicaments;
    @FXML private TextField  tfRecommandations;   // now optional
    @FXML private TextField  tfSuivi;             // always optional
    @FXML private ComboBox<SuiviTherapeutique> cbSuiviTherapeutique; // ✅ FIX 1

    @FXML private Label lblErrorDate;
    @FXML private Label lblErrorMedicaments;
    @FXML private Label lblErrorSuiviId;
    // ✅ FIX 2: lblErrorRecommandations removed — field is now optional

    private final ServicePrescriptionMedicale service     = new ServicePrescriptionMedicale();
    private final ServiceSuiviTherapeutique   suiviService = new ServiceSuiviTherapeutique();
    private PrescriptionMedicale prescription = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        clearErrors();
        loadSuivisIntoCombo();

        // Live validation listeners
        dpDatePrescription.valueProperty().addListener((obs, o, n) -> validateDate(n));
        tfMedicaments.textProperty().addListener((obs, o, n) -> {
            if (!n.trim().isEmpty()) lblErrorMedicaments.setText("");
        });
        cbSuiviTherapeutique.valueProperty().addListener((obs, o, n) -> {
            if (n != null) lblErrorSuiviId.setText("");
            validateDate(dpDatePrescription.getValue()); // ✅ re-check date when suivi changes
        });
    }

    // ✅ FIX 1: Load all suivis from DB into the ComboBox
    private void loadSuivisIntoCombo() {
        try {
            ArrayList<SuiviTherapeutique> list = suiviService.afficherAll();
            cbSuiviTherapeutique.setItems(FXCollections.observableArrayList(list));
        } catch (SQLException e) {
            lblErrorSuiviId.setText("⚠ Impossible de charger les suivis.");
        }
    }

    // Called from PrescriptionController when editing an existing record
    public void setPrescription(PrescriptionMedicale p) {
        this.prescription = p;

        if (p.getDatePrescription() != null)
            dpDatePrescription.setValue(
                    p.getDatePrescription().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate());

        tfMedicaments.setText(p.getMedicaments());
        tfRecommandations.setText(p.getRecommandations() != null ? p.getRecommandations() : "");
        tfSuivi.setText(p.getSuivi() != null ? p.getSuivi() : "");

        // ✅ FIX 1: Pre-select the matching suivi in the ComboBox
        cbSuiviTherapeutique.getItems().stream()
                .filter(s -> s.getId() == p.getSuiviTherapeutiqueId())
                .findFirst()
                .ifPresent(s -> cbSuiviTherapeutique.setValue(s));
    }

    // ── Validators ──────────────────────────────────────────────────────────

    private boolean validateDate(LocalDate value) {
        if (value == null) {
            lblErrorDate.setText("⚠ La date est obligatoire.");
            return false;
        }

        // ✅ Validate against the selected suivi's date range
        SuiviTherapeutique suivi = cbSuiviTherapeutique.getValue();
        if (suivi != null) {
            LocalDate debut = suivi.getDateDebut().toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();

            if (value.isBefore(debut)) {
                lblErrorDate.setText("⚠ La date doit être après le début du suivi ("
                        + debut.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ").");
                return false;
            }

            if (suivi.getDateFin() != null) {
                LocalDate fin = suivi.getDateFin().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                if (value.isAfter(fin)) {
                    lblErrorDate.setText("⚠ La date doit être avant la fin du suivi ("
                            + fin.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ").");
                    return false;
                }
            }
        }

        lblErrorDate.setText("");
        return true;
    }

    private boolean validateMedicaments() {
        String val = tfMedicaments.getText().trim();
        if (val.isEmpty())  { lblErrorMedicaments.setText("⚠ Champ obligatoire."); return false; }
        if (val.length() < 3) { lblErrorMedicaments.setText("⚠ Minimum 3 caractères."); return false; }
        lblErrorMedicaments.setText("");
        return true;
    }

    private boolean validateSuivi() {
        if (cbSuiviTherapeutique.getValue() == null) {
            lblErrorSuiviId.setText("⚠ Veuillez sélectionner un suivi thérapeutique.");
            return false;
        }
        lblErrorSuiviId.setText("");
        return true;
    }

    // ✅ FIX 2: recommandations removed from validateAll() — it's optional now
    private boolean validateAll() {
        boolean d = validateDate(dpDatePrescription.getValue());
        boolean m = validateMedicaments();
        boolean s = validateSuivi();
        return d && m && s;
    }

    @FXML
    private void handleSave() {
        if (!validateAll()) return;

        Date date   = Date.from(dpDatePrescription.getValue()
                .atStartOfDay(ZoneId.systemDefault()).toInstant());
        int suiviId = cbSuiviTherapeutique.getValue().getId(); // ✅ FIX 1

        // Optional fields — use null if blank
        String recommandations = tfRecommandations.getText().trim().isEmpty()
                ? null : tfRecommandations.getText().trim();
        String suiviNotes      = tfSuivi.getText().trim().isEmpty()
                ? null : tfSuivi.getText().trim();

        try {
            if (prescription == null) {
                service.ajouter(new PrescriptionMedicale(
                        0, date,
                        tfMedicaments.getText().trim(),
                        recommandations,
                        suiviNotes,
                        suiviId
                ));
            } else {
                prescription.setDatePrescription(date);
                prescription.setMedicaments(tfMedicaments.getText().trim());
                prescription.setRecommandations(recommandations);
                prescription.setSuivi(suiviNotes);
                prescription.setSuiviTherapeutiqueId(suiviId);
                service.modifier(prescription);
            }
            closeWindow();
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    @FXML private void handleCancel() { closeWindow(); }

    private void closeWindow() {
        ((Stage) tfMedicaments.getScene().getWindow()).close();
    }

    private void clearErrors() {
        lblErrorDate.setText("");
        lblErrorMedicaments.setText("");
        lblErrorSuiviId.setText("");
    }

    private void showAlert(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t);
        a.setContentText(m);
        a.showAndWait();
    }
}