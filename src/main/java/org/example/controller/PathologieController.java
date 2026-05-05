package org.example.controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import org.example.model.Pathologie;
import org.example.service.PathologieService;
import org.example.service.UserService;
import org.example.service.ExcelExportService;
import org.example.service.OpenFDAService;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PathologieController implements Initializable {

    @FXML private TableView<Pathologie> tablePathologies;
    @FXML private TableColumn<Pathologie, Integer> colId;
    @FXML private TableColumn<Pathologie, String> colNom;
    @FXML private TableColumn<Pathologie, String> colDescription;
    @FXML private TableColumn<Pathologie, String> colType;
    @FXML private TableColumn<Pathologie, String> colGravite;

    @FXML private Label lblTotal, lblFaible, lblModeree, lblCritique;
    @FXML private TextField tfRecherche;
    @FXML private ComboBox<String> cbFiltreType;
    @FXML private ComboBox<String> cbFiltreGravite;
    @FXML private PieChart pieGravite;
    @FXML private BarChart<String, Number> barType;

    private final PathologieService pathologieService = new PathologieService();
    private final UserService userService = new UserService();
    private ObservableList<Pathologie> pathologieList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        colGravite.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGravite()));

        cbFiltreType.setItems(FXCollections.observableArrayList("Tous", "chronique", "infectieuse", "hereditaire", "degenerative", "aigue"));
        cbFiltreType.setValue("Tous");
        cbFiltreGravite.setItems(FXCollections.observableArrayList("Tous", "faible", "moderee", "critique"));
        cbFiltreGravite.setValue("Tous");

        tfRecherche.textProperty().addListener((obs, o, n) -> applyFilters());
        cbFiltreType.valueProperty().addListener((obs, o, n) -> applyFilters());
        cbFiltreGravite.valueProperty().addListener((obs, o, n) -> applyFilters());

        tablePathologies.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && tablePathologies.getSelectionModel().getSelectedItem() != null)
                showFormDialog(tablePathologies.getSelectionModel().getSelectedItem());
        });

        loadData();
    }

    private void loadData() {
        pathologieList = FXCollections.observableArrayList(pathologieService.getAll());
        tablePathologies.setItems(pathologieList);
        updateStats();
        updateCharts();
    }

    private void updateStats() {
        lblTotal.setText(String.valueOf(pathologieList.size()));
        lblFaible.setText(String.valueOf(pathologieList.stream().filter(p -> "faible".equals(p.getGravite())).count()));
        lblModeree.setText(String.valueOf(pathologieList.stream().filter(p -> "moderee".equals(p.getGravite())).count()));
        lblCritique.setText(String.valueOf(pathologieList.stream().filter(p -> "critique".equals(p.getGravite())).count()));
    }

    private void showFormDialog(Pathologie existing) {
        Dialog<Pathologie> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Nouvelle pathologie" : "Modifier pathologie #" + existing.getId());
        dialog.setHeaderText(existing == null ? "Remplissez les informations" : "Modifiez les informations");

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(14);
        grid.setPadding(new Insets(24)); grid.setPrefWidth(580);

        Label lUser = new Label("Utilisateur *"); lUser.setMinWidth(120);
        Label lNom = new Label("Nom *"); lNom.setMinWidth(120);
        Label lDesc = new Label("Description"); lDesc.setMinWidth(120);
        Label lType = new Label("Type *"); lType.setMinWidth(120);
        Label lGrav = new Label("Gravité *"); lGrav.setMinWidth(120);

        ComboBox<String> cbUser = new ComboBox<>();
        cbUser.setPrefWidth(380);

        // ✅ Fixed: use getAllAsMap() instead of getAll()
        List<Map<String, Object>> users = userService.getAllAsMap();
        users.forEach(u -> cbUser.getItems().add(u.get("id") + " - " + u.get("prenom") + " " + u.get("nom")));

        TextField tfNom = new TextField(); tfNom.setPromptText("Nom de la pathologie"); tfNom.setPrefWidth(380);
        TextArea taDescription = new TextArea(); taDescription.setPrefRowCount(3); taDescription.setPrefWidth(380);
        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList("chronique", "infectieuse", "hereditaire", "degenerative", "aigue"));
        cbType.setPrefWidth(380);
        ComboBox<String> cbGravite = new ComboBox<>(FXCollections.observableArrayList("faible", "moderee", "critique"));
        cbGravite.setPrefWidth(380);

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11; -fx-font-weight: bold;");
        lblError.setWrapText(true);

        if (existing != null) {
            users.stream().filter(u -> (int) u.get("id") == existing.getUserId()).findFirst()
                    .ifPresent(u -> cbUser.setValue(u.get("id") + " - " + u.get("prenom") + " " + u.get("nom")));
            tfNom.setText(existing.getNom());
            taDescription.setText(existing.getDescription());
            cbType.setValue(existing.getType());
            cbGravite.setValue(existing.getGravite());
        }

        grid.add(lUser, 0, 0); grid.add(cbUser, 1, 0);
        grid.add(lNom, 0, 1); grid.add(tfNom, 1, 1);
        grid.add(lDesc, 0, 2); grid.add(taDescription, 1, 2);
        grid.add(lType, 0, 3); grid.add(cbType, 1, 3);
        grid.add(lGrav, 0, 4); grid.add(cbGravite, 1, 4);
        grid.add(lblError, 0, 5, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 13;");
        dialog.getDialogPane().setPrefWidth(650);

        final javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveBtn);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            StringBuilder errors = new StringBuilder();
            if (cbUser.getValue() == null) errors.append("• Utilisateur obligatoire\n");
            if (tfNom.getText().trim().length() < 2) errors.append("• Nom min 2 caractères\n");
            if (cbType.getValue() == null) errors.append("• Type obligatoire\n");
            if (cbGravite.getValue() == null) errors.append("• Gravité obligatoire\n");
            if (errors.length() > 0) { lblError.setText(errors.toString()); event.consume(); }
        });

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                Pathologie p = existing != null ? existing : new Pathologie();
                p.setUserId(Integer.parseInt(cbUser.getValue().split(" - ")[0]));
                p.setNom(tfNom.getText().trim());
                p.setDescription(taDescription.getText() != null ? taDescription.getText().trim() : null);
                p.setType(cbType.getValue());
                p.setGravite(cbGravite.getValue());
                return p;
            }
            return null;
        });

        Optional<Pathologie> result = dialog.showAndWait();
        result.ifPresent(p -> {
            if (existing == null) { pathologieService.save(p); showSuccess("Pathologie ajoutée !"); }
            else { pathologieService.update(p); showSuccess("Pathologie modifiée !"); }
            loadData();
        });
    }

    @FXML private void handleAdd() { showFormDialog(null); }

    @FXML private void handleUpdate() {
        Pathologie s = tablePathologies.getSelectionModel().getSelectedItem();
        if (s == null) { showAlert("Sélectionnez une pathologie."); return; }
        showFormDialog(s);
    }

    @FXML private void handleDelete() {
        Pathologie s = tablePathologies.getSelectionModel().getSelectedItem();
        if (s == null) { showAlert("Sélectionnez une pathologie."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer cette pathologie ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) { pathologieService.delete(s.getId()); loadData(); showSuccess("Supprimée !"); }
        });
    }

    @FXML private void handleResetFilters() {
        tfRecherche.clear(); cbFiltreType.setValue("Tous"); cbFiltreGravite.setValue("Tous");
        tablePathologies.setItems(pathologieList);
    }

    @FXML private void handleExportExcel() {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("pathologies.xlsx");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        java.io.File file = fc.showSaveDialog(tablePathologies.getScene().getWindow());
        if (file != null) {
            try { new ExcelExportService().exportPathologies(new java.util.ArrayList<>(tablePathologies.getItems()), file.getAbsolutePath()); showSuccess("Export réussi !"); }
            catch (Exception e) { showAlert("Erreur : " + e.getMessage()); }
        }
    }

    private void applyFilters() {
        String kw = tfRecherche.getText();
        String tp = cbFiltreType.getValue();
        String gr = cbFiltreGravite.getValue();
        ObservableList<Pathologie> filtered = pathologieList.filtered(p -> {
            boolean matchKw = (kw == null || kw.isEmpty()) || (p.getNom() != null && p.getNom().toLowerCase().contains(kw.toLowerCase()));
            boolean matchTp = (tp == null || tp.equals("Tous")) || tp.equals(p.getType());
            boolean matchGr = (gr == null || gr.equals("Tous")) || gr.equals(p.getGravite());
            return matchKw && matchTp && matchGr;
        });
        tablePathologies.setItems(filtered);
    }

    private void updateCharts() {
        Map<String, Long> gravites = pathologieList.stream()
                .collect(Collectors.groupingBy(p -> p.getGravite() != null ? p.getGravite() : "Inconnu", Collectors.counting()));
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        gravites.forEach((g, c) -> pieData.add(new PieChart.Data(g + " (" + c + ")", c)));
        pieGravite.setData(pieData); pieGravite.setTitle("Par Gravité");

        Map<String, Long> types = pathologieList.stream()
                .collect(Collectors.groupingBy(p -> p.getType() != null ? p.getType() : "Inconnu", Collectors.counting()));
        XYChart.Series<String, Number> series = new XYChart.Series<>(); series.setName("Pathologies");
        types.forEach((t, c) -> series.getData().add(new XYChart.Data<>(t, c)));
        barType.getData().clear(); barType.getData().add(series); barType.setTitle("Par Type");
    }

    @FXML
    private void handleSearchFDA() {
        Pathologie selected = tablePathologies.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Sélectionnez une pathologie.");
            return;
        }

        OpenFDAService fdaService = new OpenFDAService();
        List<String> medicaments = fdaService.searchMedicaments(selected.getNom());

        if (medicaments.isEmpty()) {
            showAlert("Aucun médicament trouvé pour \"" + selected.getNom() + "\" dans OpenFDA.");
            return;
        }

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Médicaments FDA — " + selected.getNom());
        dialog.setHeaderText("Médicaments trouvés pour : " + selected.getNom());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);
        dialog.getDialogPane().setPrefWidth(600);

        ListView<String> listView = new ListView<>();
        listView.getItems().addAll(medicaments);
        listView.setPrefHeight(300);

        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && listView.getSelectionModel().getSelectedItem() != null) {
                String medName = listView.getSelectionModel().getSelectedItem();
                String details = fdaService.getMedicamentDetails(medName);

                Alert detailAlert = new Alert(Alert.AlertType.INFORMATION);
                detailAlert.setTitle("Détails — " + medName);
                detailAlert.setHeaderText(medName);
                TextArea ta = new TextArea(details);
                ta.setWrapText(true);
                ta.setEditable(false);
                ta.setPrefHeight(400);
                ta.setPrefWidth(500);
                detailAlert.getDialogPane().setContent(ta);
                detailAlert.showAndWait();
            }
        });

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10);
        content.getChildren().addAll(
                new Label("Double-cliquez sur un médicament pour voir les détails"),
                listView
        );
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void showAlert(String msg) { new Alert(Alert.AlertType.WARNING, msg).showAndWait(); }
    private void showSuccess(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
}