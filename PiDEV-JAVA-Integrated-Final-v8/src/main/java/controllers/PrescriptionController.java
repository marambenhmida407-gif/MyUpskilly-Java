package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import models.PrescriptionMedicale;
import services.ServicePrescriptionMedicale;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.ResourceBundle;

public class PrescriptionController implements Initializable {

    // ── Table ────────────────────────────────────────────────────────────────
    @FXML private TableView<PrescriptionMedicale>            tableView;
    @FXML private TableColumn<PrescriptionMedicale, Integer> colId;
    @FXML private TableColumn<PrescriptionMedicale, Date>    colDatePrescription;
    @FXML private TableColumn<PrescriptionMedicale, String>  colMedicaments;
    @FXML private TableColumn<PrescriptionMedicale, String>  colRecommandations;
    @FXML private TableColumn<PrescriptionMedicale, String>  colSuivi;
    @FXML private TableColumn<PrescriptionMedicale, Integer> colSuiviId;

    // ── Search ───────────────────────────────────────────────────────────────
    @FXML private TextField tfSearch;

    // ── Filter panel ─────────────────────────────────────────────────────────
    @FXML private VBox              filterPanel;
    @FXML private Button            btnToggleFilters;
    @FXML private DatePicker        dpFilterFrom;
    @FXML private DatePicker        dpFilterTo;
    @FXML private ComboBox<String>  cbFilterRec;
    @FXML private Label             lblActiveFilters;

    private final ServicePrescriptionMedicale service = new ServicePrescriptionMedicale();
    private final ObservableList<PrescriptionMedicale> masterList =
            FXCollections.observableArrayList();
    private FilteredList<PrescriptionMedicale> filteredList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        setupFilterCombo();
        loadData();
        setupSearch();
        setupFilters();
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colMedicaments.setCellValueFactory(new PropertyValueFactory<>("medicaments"));
        colRecommandations.setCellValueFactory(new PropertyValueFactory<>("recommandations"));
        colSuivi.setCellValueFactory(new PropertyValueFactory<>("suivi"));
        colSuiviId.setCellValueFactory(new PropertyValueFactory<>("suiviTherapeutiqueId"));

        colDatePrescription.setCellValueFactory(new PropertyValueFactory<>("datePrescription"));
        colDatePrescription.setCellFactory(col -> formatDateCell());
    }

    private TableCell<PrescriptionMedicale, Date> formatDateCell() {
        return new TableCell<>() {
            private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy");
            @Override
            protected void updateItem(Date date, boolean empty) {
                super.updateItem(date, empty);
                setText((empty || date == null) ? null : fmt.format(date));
            }
        };
    }

    private void setupFilterCombo() {
        cbFilterRec.setItems(FXCollections.observableArrayList(
                "Toutes", "Avec recommandations", "Sans recommandations"
        ));
        cbFilterRec.setValue("Toutes");
    }

    private void loadData() {
        try {
            masterList.setAll(service.afficherAll());
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private void setupSearch() {
        filteredList = new FilteredList<>(masterList, p -> true);
        tfSearch.textProperty().addListener((obs, o, n) -> applyFilters());
        SortedList<PrescriptionMedicale> sorted = new SortedList<>(filteredList);
        sorted.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sorted);
    }

    private void setupFilters() {
        cbFilterRec.valueProperty().addListener((obs, o, n)  -> applyFilters());
        dpFilterFrom.valueProperty().addListener((obs, o, n) -> applyFilters());
        dpFilterTo.valueProperty().addListener((obs, o, n)   -> applyFilters());
    }

    private void applyFilters() {
        String search = tfSearch.getText() == null ? "" : tfSearch.getText().toLowerCase();
        String rec    = cbFilterRec.getValue();
        LocalDate from = dpFilterFrom.getValue();
        LocalDate to   = dpFilterTo.getValue();

        filteredList.setPredicate(p -> {
            // Search bar
            if (!search.isEmpty()) {
                boolean match =
                        p.getMedicaments().toLowerCase().contains(search)
                                || (p.getRecommandations() != null
                                && p.getRecommandations().toLowerCase().contains(search))
                                || (p.getSuivi() != null
                                && p.getSuivi().toLowerCase().contains(search));
                if (!match) return false;
            }

            // Recommandations filter
            if (rec != null && !rec.equals("Toutes")) {
                boolean hasRec = p.getRecommandations() != null
                        && !p.getRecommandations().trim().isEmpty();
                if (rec.equals("Avec recommandations") && !hasRec)  return false;
                if (rec.equals("Sans recommandations") &&  hasRec)  return false;
            }

            // Date range filter
            if ((from != null || to != null) && p.getDatePrescription() != null) {
                LocalDate date = p.getDatePrescription().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                if (from != null && date.isBefore(from)) return false;
                if (to   != null && date.isAfter(to))    return false;
            }

            return true;
        });

        updateActiveFiltersLabel();
    }

    private void updateActiveFiltersLabel() {
        int count = 0;
        if (cbFilterRec.getValue() != null
                && !cbFilterRec.getValue().equals("Toutes")) count++;
        if (dpFilterFrom.getValue() != null) count++;
        if (dpFilterTo.getValue()   != null) count++;

        lblActiveFilters.setText(count == 0 ? ""
                : "✓ " + count + " filtre" + (count > 1 ? "s" : "")
                  + " actif" + (count > 1 ? "s" : ""));
    }

    @FXML
    private void toggleFilters() {
        boolean visible = filterPanel.isVisible();
        filterPanel.setVisible(!visible);
        filterPanel.setManaged(!visible);
        btnToggleFilters.setText(visible ? "⚙ Filtres" : "✕ Fermer filtres");
    }

    @FXML
    private void resetFilters() {
        cbFilterRec.setValue("Toutes");
        dpFilterFrom.setValue(null);
        dpFilterTo.setValue(null);
        tfSearch.clear();
        lblActiveFilters.setText("");
    }

    @FXML
    private void handleAjouter() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/PrescriptionForm.fxml"));
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setTitle("Ajouter Prescription Médicale");
        stage.setScene(new Scene(root, 500, 530));
        stage.showAndWait();
        loadData();
    }

    @FXML
    private void handleModifier() throws IOException {
        PrescriptionMedicale selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Attention", "Veuillez sélectionner un enregistrement."); return; }
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/PrescriptionForm.fxml"));
        Parent root = loader.load();
        PrescriptionFormController controller = loader.getController();
        controller.setPrescription(selected);
        Stage stage = new Stage();
        stage.setTitle("Modifier Prescription Médicale");
        stage.setScene(new Scene(root, 500, 530));
        stage.showAndWait();
        loadData();
    }

    @FXML
    private void handleSupprimer() {
        PrescriptionMedicale selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Attention", "Veuillez sélectionner un enregistrement."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Confirmer la suppression ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                try { service.supprimer(selected.getId()); loadData(); }
                catch (SQLException e) { showAlert("Erreur", e.getMessage()); }
            }
        });
    }

    private void showAlert(String title, String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}