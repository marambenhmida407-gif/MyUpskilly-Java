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
import models.SuiviTherapeutique;
import services.PdfExportService;
import services.ServicePrescriptionMedicale;
import services.ServiceSuiviTherapeutique;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.ResourceBundle;

public class SuiviController implements Initializable {

    // ── Table ────────────────────────────────────────────────────────────────
    @FXML private TableView<SuiviTherapeutique>            tableView;
    @FXML private TableColumn<SuiviTherapeutique, Integer> colId;
    @FXML private TableColumn<SuiviTherapeutique, String>  colPatient;
    @FXML private TableColumn<SuiviTherapeutique, String>  colTypeSuivi;
    @FXML private TableColumn<SuiviTherapeutique, String>  colObjectif;
    @FXML private TableColumn<SuiviTherapeutique, String>  colStatut;
    @FXML private TableColumn<SuiviTherapeutique, Date>    colDateDebut;
    @FXML private TableColumn<SuiviTherapeutique, Date>    colDateFin;

    // ── Search ───────────────────────────────────────────────────────────────
    @FXML private TextField tfSearch;

    // ── Filter panel ─────────────────────────────────────────────────────────
    @FXML private VBox            filterPanel;
    @FXML private Button          btnToggleFilters;
    @FXML private ComboBox<String> cbFilterStatut;
    @FXML private TextField       tfFilterType;
    @FXML private DatePicker      dpFilterDebutFrom;
    @FXML private DatePicker      dpFilterDebutTo;
    @FXML private Label           lblActiveFilters;

    private final ServiceSuiviTherapeutique service    = new ServiceSuiviTherapeutique();
    private final ObservableList<SuiviTherapeutique> masterList =
            FXCollections.observableArrayList();
    private FilteredList<SuiviTherapeutique> filteredList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        setupFilterCombo();
        loadData();
        setupSearch();
        setupFilters();
    }

    // ── Column setup ─────────────────────────────────────────────────────────
    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPatient.setCellValueFactory(new PropertyValueFactory<>("patientNom"));
        colTypeSuivi.setCellValueFactory(new PropertyValueFactory<>("typeSuivi"));
        colObjectif.setCellValueFactory(new PropertyValueFactory<>("objectifTherapeutique"));

        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(statut);
                badge.getStyleClass().add("badge");
                switch (statut) {
                    case "En cours" -> badge.getStyleClass().add("badge-green");
                    case "Terminé"  -> badge.getStyleClass().add("badge-dark");
                    case "Suspendu" -> badge.getStyleClass().add("badge-orange");
                    case "Planifié" -> badge.getStyleClass().add("badge-blue");
                    default         -> badge.getStyleClass().add("badge-gray");
                }
                setGraphic(badge);
                setText(null);
            }
        });

        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));
        colDateFin.setCellValueFactory(new PropertyValueFactory<>("dateFin"));
        colDateDebut.setCellFactory(col -> formatDateCell());
        colDateFin.setCellFactory(col -> formatDateCell());
    }

    private TableCell<SuiviTherapeutique, Date> formatDateCell() {
        return new TableCell<>() {
            private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy");
            @Override
            protected void updateItem(Date date, boolean empty) {
                super.updateItem(date, empty);
                setText((empty || date == null) ? null : fmt.format(date));
            }
        };
    }

    // ── Filter combo setup ───────────────────────────────────────────────────
    private void setupFilterCombo() {
        cbFilterStatut.setItems(FXCollections.observableArrayList(
                "Tous", "En cours", "Terminé", "Suspendu", "Planifié"
        ));
        cbFilterStatut.setValue("Tous");
    }

    // ── Load data ────────────────────────────────────────────────────────────
    private void loadData() {
        try {
            masterList.setAll(service.afficherAll());
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    // ── Search + filter combined predicate ───────────────────────────────────
    private void setupSearch() {
        filteredList = new FilteredList<>(masterList, p -> true);

        // Search listener
        tfSearch.textProperty().addListener((obs, o, n) -> applyFilters());

        SortedList<SuiviTherapeutique> sorted = new SortedList<>(filteredList);
        sorted.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sorted);
    }

    private void setupFilters() {
        // All filter controls trigger applyFilters on any change
        cbFilterStatut.valueProperty().addListener((obs, o, n) -> applyFilters());
        tfFilterType.textProperty().addListener((obs, o, n)    -> applyFilters());
        dpFilterDebutFrom.valueProperty().addListener((obs, o, n) -> applyFilters());
        dpFilterDebutTo.valueProperty().addListener((obs, o, n)   -> applyFilters());
    }

    private void applyFilters() {
        String search    = tfSearch.getText() == null ? "" : tfSearch.getText().toLowerCase();
        String statut    = cbFilterStatut.getValue();
        String type      = tfFilterType.getText() == null ? "" : tfFilterType.getText().toLowerCase();
        LocalDate from   = dpFilterDebutFrom.getValue();
        LocalDate to     = dpFilterDebutTo.getValue();

        filteredList.setPredicate(s -> {
            // ── Search bar ──────────────────────────────────────────────────
            if (!search.isEmpty()) {
                boolean matchSearch =
                        s.getTypeSuivi().toLowerCase().contains(search)
                                || s.getObjectifTherapeutique().toLowerCase().contains(search)
                                || s.getStatut().toLowerCase().contains(search);
                if (!matchSearch) return false;
            }

            // ── Statut filter ───────────────────────────────────────────────
            if (statut != null && !statut.equals("Tous")) {
                if (!s.getStatut().equals(statut)) return false;
            }

            // ── Type filter ─────────────────────────────────────────────────
            if (!type.isEmpty()) {
                if (!s.getTypeSuivi().toLowerCase().contains(type)) return false;
            }

            // ── Date range filter ───────────────────────────────────────────
            if (from != null || to != null) {
                if (s.getDateDebut() == null) return false;
                LocalDate dateDebut = s.getDateDebut().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                if (from != null && dateDebut.isBefore(from)) return false;
                if (to   != null && dateDebut.isAfter(to))    return false;
            }

            return true;
        });

        updateActiveFiltersLabel();
    }

    private void updateActiveFiltersLabel() {
        int count = 0;
        if (cbFilterStatut.getValue() != null
                && !cbFilterStatut.getValue().equals("Tous")) count++;
        if (tfFilterType.getText() != null
                && !tfFilterType.getText().isEmpty()) count++;
        if (dpFilterDebutFrom.getValue() != null) count++;
        if (dpFilterDebutTo.getValue()   != null) count++;

        if (count == 0) {
            lblActiveFilters.setText("");
        } else {
            lblActiveFilters.setText("✓ " + count + " filtre"
                    + (count > 1 ? "s" : "") + " actif"
                    + (count > 1 ? "s" : ""));
        }
    }

    // ── Toggle filter panel ──────────────────────────────────────────────────
    @FXML
    private void toggleFilters() {
        boolean visible = filterPanel.isVisible();
        filterPanel.setVisible(!visible);
        filterPanel.setManaged(!visible);
        btnToggleFilters.setText(visible ? "⚙ Filtres" : "✕ Fermer filtres");
    }

    // ── Reset all filters ────────────────────────────────────────────────────
    @FXML
    private void resetFilters() {
        cbFilterStatut.setValue("Tous");
        tfFilterType.clear();
        dpFilterDebutFrom.setValue(null);
        dpFilterDebutTo.setValue(null);
        tfSearch.clear();
        lblActiveFilters.setText("");
    }

    // ── CRUD handlers ────────────────────────────────────────────────────────
    @FXML
    private void handleAjouter() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/SuiviForm.fxml"));
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setTitle("Ajouter Suivi Thérapeutique");
        stage.setScene(new Scene(root, 500, 530));
        stage.showAndWait();
        loadData();
    }

    @FXML
    private void handleModifier() throws IOException {
        SuiviTherapeutique selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Attention", "Veuillez sélectionner un enregistrement."); return; }
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/SuiviForm.fxml"));
        Parent root = loader.load();
        SuiviFormController controller = loader.getController();
        controller.setSuivi(selected);
        Stage stage = new Stage();
        stage.setTitle("Modifier Suivi Thérapeutique");
        stage.setScene(new Scene(root, 500, 530));
        stage.showAndWait();
        loadData();
    }

    @FXML
    private void handleSupprimer() {
        SuiviTherapeutique selected = tableView.getSelectionModel().getSelectedItem();
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

    @FXML
    private void handleExportPdf() {
        SuiviTherapeutique selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Attention", "Veuillez sélectionner un suivi à exporter."); return; }
        try {
            ServicePrescriptionMedicale prescService = new ServicePrescriptionMedicale();
            ArrayList<PrescriptionMedicale> prescriptions =
                    prescService.afficherParSuivi(selected.getId());
            PdfExportService pdfService = new PdfExportService();
            File pdf = pdfService.export(selected, prescriptions);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(pdf);
            showAlert("Succès", "PDF exporté : " + pdf.getName());
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de générer le PDF : " + e.getMessage());
        }
    }

    private void showAlert(String title, String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}