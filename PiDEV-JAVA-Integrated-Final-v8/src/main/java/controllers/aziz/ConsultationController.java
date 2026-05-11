package controllers.aziz;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import models.aziz.Consultation;
import models.aziz.Pathologie;
import services.aziz.*;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ConsultationController implements Initializable {

    @FXML private TableView<Consultation> tableConsultations;
    @FXML private TableColumn<Consultation, Integer> colId;
    @FXML private TableColumn<Consultation, String> colPatient;
    @FXML private TableColumn<Consultation, String> colDate;
    @FXML private TableColumn<Consultation, String> colMotif;
    @FXML private TableColumn<Consultation, String> colDiagnostic;
    @FXML private TableColumn<Consultation, String> colStatut;
    @FXML private TableColumn<Consultation, String> colPathologie;

    @FXML private Label lblTotal, lblAujourdhui, lblProgrammees, lblEnCours, lblAnnulees;
    @FXML private TextField tfRecherche;
    @FXML private ComboBox<String> cbFiltreStatut;
    @FXML private DatePicker dpFiltreDate;
    @FXML private PieChart pieStatut;
    @FXML private BarChart<String, Number> barMensuel;

    private final ConsultationService consultationService = new ConsultationService();
    private final PathologieService pathologieService = new PathologieService();
    private final UserService userService = new UserService();
    private ObservableList<Consultation> consultationList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colPatient.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPatientNom() != null ? c.getValue().getPatientNom() : "#" + c.getValue().getPatientId()));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateConsultation() != null ? c.getValue().getDateConsultation().toString().replace("T", " ") : ""));
        colMotif.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMotif()));
        colDiagnostic.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDiagnostic()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));
        colPathologie.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPathologieNom() != null ? c.getValue().getPathologieNom() : ""));

        cbFiltreStatut.setItems(FXCollections.observableArrayList("Tous", "en_cours", "terminee", "annulee", "programmee"));
        cbFiltreStatut.setValue("Tous");

        tfRecherche.textProperty().addListener((obs, o, n) -> applyFilters());
        cbFiltreStatut.valueProperty().addListener((obs, o, n) -> applyFilters());
        dpFiltreDate.valueProperty().addListener((obs, o, n) -> applyFilters());

        // Simple clic → ouvre page détails
        tableConsultations.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && tableConsultations.getSelectionModel().getSelectedItem() != null) {
                openDetail(tableConsultations.getSelectionModel().getSelectedItem());
            }
        });

        loadData();
    }

    private void openDetail(Consultation c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/aziz/detail_consultation.fxml"));
            javafx.scene.Node page = loader.load();
            DetailConsultationController ctrl = loader.getController();
            ctrl.setConsultation(c);
            BorderPane root = (BorderPane) tableConsultations.getScene().getRoot();
            root.setCenter(page);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        consultationList = FXCollections.observableArrayList(consultationService.getAll());
        tableConsultations.setItems(consultationList);
        updateStats();
        updateCharts();
    }

    private void updateStats() {
        lblTotal.setText(String.valueOf(consultationList.size()));
        lblAujourdhui.setText(String.valueOf(consultationList.stream()
                .filter(c -> c.getDateConsultation() != null && c.getDateConsultation().toLocalDate().equals(LocalDate.now())).count()));
        lblProgrammees.setText(String.valueOf(consultationList.stream().filter(c -> "programmee".equals(c.getStatut())).count()));
        lblEnCours.setText(String.valueOf(consultationList.stream().filter(c -> "en_cours".equals(c.getStatut())).count()));
        lblAnnulees.setText(String.valueOf(consultationList.stream().filter(c -> "annulee".equals(c.getStatut())).count()));
    }

    private void showFormDialog(Consultation existing) {
        Dialog<Consultation> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Nouvelle consultation" : "Modifier consultation #" + existing.getId());
        dialog.setHeaderText(existing == null ? "Remplissez les informations" : "Modifiez les informations");

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(14);
        grid.setPadding(new Insets(24)); grid.setPrefWidth(620);

        // Labels avec style
        Label lPatient = new Label("Patient *"); lPatient.setMinWidth(120);
        Label lDate = new Label("Date *"); lDate.setMinWidth(120);
        Label lMotif = new Label("Motif *"); lMotif.setMinWidth(120);
        Label lDiag = new Label("Diagnostic *"); lDiag.setMinWidth(120);
        Label lObs = new Label("Observations"); lObs.setMinWidth(120);
        Label lOrd = new Label("Ordonnance"); lOrd.setMinWidth(120);
        Label lStatut = new Label("Statut *"); lStatut.setMinWidth(120);
        Label lPath = new Label("Pathologie"); lPath.setMinWidth(120);

        ComboBox<String> cbPatient = new ComboBox<>();
        cbPatient.setPrefWidth(400);
        List<Map<String, Object>> users = userService.getAll();
        users.forEach(u -> cbPatient.getItems().add(u.get("id") + " - " + u.get("prenom") + " " + u.get("nom") + " (" + u.get("email") + ")"));

        DatePicker dpDate = new DatePicker(); dpDate.setPrefWidth(400);
        TextField tfMotif = new TextField(); tfMotif.setPromptText("Motif de consultation"); tfMotif.setPrefWidth(400);
        TextArea taDiagnostic = new TextArea(); taDiagnostic.setPrefRowCount(2); taDiagnostic.setPrefWidth(400);
        TextArea taObservations = new TextArea(); taObservations.setPrefRowCount(2); taObservations.setPrefWidth(400);
        TextArea taOrdonnance = new TextArea(); taOrdonnance.setPrefRowCount(2); taOrdonnance.setPrefWidth(400);
        ComboBox<String> cbStatut = new ComboBox<>(FXCollections.observableArrayList("en_cours", "terminee", "annulee", "programmee"));
        cbStatut.setPrefWidth(400);

        ComboBox<String> cbPathologie = new ComboBox<>();
        cbPathologie.setPrefWidth(400);
        cbPathologie.getItems().add("Aucune");
        List<Pathologie> pathologies = pathologieService.getAll();
        pathologies.forEach(p -> cbPathologie.getItems().add(p.getId() + " - " + p.getNom()));
        cbPathologie.setValue("Aucune");

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11; -fx-font-weight: bold;");
        lblError.setWrapText(true);

        if (existing != null) {
            users.stream().filter(u -> (int) u.get("id") == existing.getPatientId()).findFirst()
                    .ifPresent(u -> cbPatient.setValue(u.get("id") + " - " + u.get("prenom") + " " + u.get("nom") + " (" + u.get("email") + ")"));
            dpDate.setValue(existing.getDateConsultation() != null ? existing.getDateConsultation().toLocalDate() : null);
            tfMotif.setText(existing.getMotif());
            taDiagnostic.setText(existing.getDiagnostic());
            taObservations.setText(existing.getObservations());
            taOrdonnance.setText(existing.getOrdonnance());
            cbStatut.setValue(existing.getStatut());
            if (existing.getPathologieId() != null)
                pathologies.stream().filter(p -> p.getId() == existing.getPathologieId()).findFirst()
                        .ifPresent(p -> cbPathologie.setValue(p.getId() + " - " + p.getNom()));
        }

        grid.add(lPatient, 0, 0); grid.add(cbPatient, 1, 0);
        grid.add(lDate, 0, 1); grid.add(dpDate, 1, 1);
        grid.add(lMotif, 0, 2); grid.add(tfMotif, 1, 2);
        grid.add(lDiag, 0, 3); grid.add(taDiagnostic, 1, 3);
        grid.add(lObs, 0, 4); grid.add(taObservations, 1, 4);
        grid.add(lOrd, 0, 5); grid.add(taOrdonnance, 1, 5);
        grid.add(lStatut, 0, 6); grid.add(cbStatut, 1, 6);
        grid.add(lPath, 0, 7); grid.add(cbPathologie, 1, 7);
        grid.add(lblError, 0, 8, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13;");
        dialog.getDialogPane().setPrefWidth(650);

        final javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveBtn);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            StringBuilder errors = new StringBuilder();
            if (cbPatient.getValue() == null) errors.append("• Patient obligatoire\n");
            if (dpDate.getValue() == null) errors.append("• Date obligatoire\n");
            if (tfMotif.getText().trim().length() < 3) errors.append("• Motif min 3 caractères\n");
            if (taDiagnostic.getText().trim().isEmpty()) errors.append("• Diagnostic obligatoire\n");
            if (cbStatut.getValue() == null) errors.append("• Statut obligatoire\n");
            if (errors.length() > 0) { lblError.setText(errors.toString()); event.consume(); }
        });

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                Consultation c = existing != null ? existing : new Consultation();
                String patientVal = cbPatient.getValue();
                int patientId = Integer.parseInt(patientVal.split(" - ")[0]);
                c.setPatientId(patientId);
                // Extraire email du patient
                String emailPart = patientVal.substring(patientVal.indexOf("(") + 1, patientVal.indexOf(")"));
                c.setPatientEmail(emailPart);
                c.setDateConsultation(dpDate.getValue().atStartOfDay());
                c.setMotif(tfMotif.getText().trim());
                c.setDiagnostic(taDiagnostic.getText().trim());
                c.setObservations(taObservations.getText() != null ? taObservations.getText().trim() : null);
                c.setOrdonnance(taOrdonnance.getText() != null ? taOrdonnance.getText().trim() : null);
                c.setStatut(cbStatut.getValue());
                String pathVal = cbPathologie.getValue();
                if (pathVal != null && !pathVal.equals("Aucune"))
                    c.setPathologieId(Integer.parseInt(pathVal.split(" - ")[0]));
                else c.setPathologieId(null);
                return c;
            }
            return null;
        });

        Optional<Consultation> result = dialog.showAndWait();
        result.ifPresent(c -> {
            if (existing == null) {
                consultationService.save(c);
                // Email automatique au patient
                if (c.getPatientEmail() != null && !c.getPatientEmail().isEmpty()) {
                    try {
                        new EmailService().sendConfirmation(c, c.getPatientEmail());
                    } catch (Exception e) {
                        System.err.println("Erreur email auto: " + e.getMessage());
                    }
                }
                showSuccess("Consultation ajoutée ! Email envoyé au patient.");
            } else {
                consultationService.update(c);
                showSuccess("Consultation modifiée !");
            }
            loadData();
        });
    }

    @FXML private void handleAdd() { showFormDialog(null); }

    @FXML private void handleUpdate() {
        Consultation s = tableConsultations.getSelectionModel().getSelectedItem();
        if (s == null) { showAlert("Sélectionnez une consultation."); return; }
        showFormDialog(s);
    }

    @FXML private void handleDelete() {
        Consultation s = tableConsultations.getSelectionModel().getSelectedItem();
        if (s == null) { showAlert("Sélectionnez une consultation."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette consultation ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) { consultationService.delete(s.getId()); loadData(); showSuccess("Supprimée !"); }
        });
    }

    @FXML private void handleResetFilters() {
        tfRecherche.clear(); cbFiltreStatut.setValue("Tous"); dpFiltreDate.setValue(null);
        tableConsultations.setItems(consultationList);
    }

    @FXML private void handleExportExcel() {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("consultations.xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        java.io.File file = fc.showSaveDialog(tableConsultations.getScene().getWindow());
        if (file != null) {
            try { new ExcelExportService().exportConsultations(new java.util.ArrayList<>(tableConsultations.getItems()), file.getAbsolutePath()); showSuccess("Export réussi !"); }
            catch (Exception e) { showAlert("Erreur : " + e.getMessage()); }
        }
    }

    @FXML private void handleSendEmail() {
        Consultation s = tableConsultations.getSelectionModel().getSelectedItem();
        if (s == null) { showAlert("Sélectionnez une consultation."); return; }
        if (s.getPatientEmail() != null && !s.getPatientEmail().isEmpty()) {
            try { new EmailService().sendConfirmation(s, s.getPatientEmail()); showSuccess("Email envoyé à " + s.getPatientEmail()); }
            catch (Exception e) { showAlert("Erreur : " + e.getMessage()); }
        } else showAlert("Ce patient n'a pas d'email.");
    }

    @FXML private void handleSendRappel() {
        Consultation s = tableConsultations.getSelectionModel().getSelectedItem();
        if (s == null) { showAlert("Sélectionnez une consultation."); return; }
        if (s.getPatientEmail() != null && !s.getPatientEmail().isEmpty()) {
            try { new EmailService().sendRappel(s, s.getPatientEmail()); showSuccess("Rappel envoyé à " + s.getPatientEmail()); }
            catch (Exception e) { showAlert("Erreur : " + e.getMessage()); }
        } else showAlert("Ce patient n'a pas d'email.");
    }

    private void applyFilters() {
        String kw = tfRecherche.getText();
        String st = cbFiltreStatut.getValue();
        LocalDate dt = dpFiltreDate.getValue();
        ObservableList<Consultation> filtered = consultationList.filtered(c -> {
            boolean matchKw = (kw == null || kw.isEmpty()) ||
                    (c.getMotif() != null && c.getMotif().toLowerCase().contains(kw.toLowerCase())) ||
                    (c.getDiagnostic() != null && c.getDiagnostic().toLowerCase().contains(kw.toLowerCase())) ||
                    (c.getPatientNom() != null && c.getPatientNom().toLowerCase().contains(kw.toLowerCase()));
            boolean matchSt = (st == null || st.equals("Tous")) || st.equals(c.getStatut());
            boolean matchDt = (dt == null) || (c.getDateConsultation() != null && c.getDateConsultation().toLocalDate().equals(dt));
            return matchKw && matchSt && matchDt;
        });
        tableConsultations.setItems(filtered);
    }

    private void updateCharts() {
        Map<String, Long> statuts = consultationList.stream()
                .collect(Collectors.groupingBy(c -> c.getStatut() != null ? c.getStatut() : "Inconnu", Collectors.counting()));
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        statuts.forEach((s, count) -> pieData.add(new PieChart.Data(s + " (" + count + ")", count)));
        pieStatut.setData(pieData); pieStatut.setTitle("Répartition par statut");

        Map<String, Long> mois = consultationList.stream().filter(c -> c.getDateConsultation() != null)
                .collect(Collectors.groupingBy(c -> c.getDateConsultation().getYear() + "-" + String.format("%02d", c.getDateConsultation().getMonthValue()), Collectors.counting()));
        XYChart.Series<String, Number> series = new XYChart.Series<>(); series.setName("Consultations");
        mois.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));
        barMensuel.getData().clear(); barMensuel.getData().add(series); barMensuel.setTitle("Par mois");
    }

    private void showAlert(String msg) { new Alert(Alert.AlertType.WARNING, msg).showAndWait(); }
    private void showSuccess(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
}