package org.example.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.example.service.UserService;
import org.example.model.User;

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class UserController implements Initializable {

    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colNom;
    @FXML private TableColumn<User, String> colPrenom;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colSpecialite;
    @FXML private TableColumn<User, Boolean> colVerifie;
    @FXML private TableColumn<User, String> colEtat;

    @FXML private TextField tfRecherche;
    @FXML private ComboBox<String> cbFiltreRole;
    @FXML private ComboBox<String> cbFiltreEtat;

    @FXML private Label lblTotal;
    @FXML private Label lblVerifies;
    @FXML private Label lblActifs;
    @FXML private PieChart pieRoles;
    @FXML private BarChart<String, Number> barSpecialites;

    private final UserService userService = new UserService();
    private ObservableList<User> userList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colPrenom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrenom()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(parseRole(c.getValue().getRoles())));
        colSpecialite.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSpecialite()));
        colVerifie.setCellValueFactory(c -> new SimpleBooleanProperty(c.getValue().isVerified()).asObject());
        colEtat.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEtat() != null ? (c.getValue().getEtat() ? "Actif" : "Inactif") : "—"));

        cbFiltreRole.setItems(FXCollections.observableArrayList(
                "Tous", "ROLE_USER", "ROLE_MEDECIN", "ROLE_ADMIN", "ROLE_PATIENT"));
        cbFiltreRole.setValue("Tous");
        cbFiltreEtat.setItems(FXCollections.observableArrayList("Tous", "Actif", "Inactif"));
        cbFiltreEtat.setValue("Tous");

        tfRecherche.textProperty().addListener((obs, o, n) -> applyFilters());
        cbFiltreRole.valueProperty().addListener((obs, o, n) -> applyFilters());
        cbFiltreEtat.valueProperty().addListener((obs, o, n) -> applyFilters());

        tableUsers.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && tableUsers.getSelectionModel().getSelectedItem() != null) {
                showFormDialog(tableUsers.getSelectionModel().getSelectedItem());
            }
        });

        loadData();
    }

    private String parseRole(String roles) {
        if (roles == null || roles.isEmpty()) return "";
        return roles.replace("[", "").replace("]", "")
                .replace("\"", "").split(",")[0].trim();
    }

    private void loadData() {
        userList = FXCollections.observableArrayList(userService.getAll());
        tableUsers.setItems(userList);
        updateStats();
        updateCharts();
    }

    private void updateStats() {
        long verifies = userList.stream().filter(User::isVerified).count();
        long actifs = userList.stream().filter(u -> Boolean.TRUE.equals(u.getEtat())).count();
        lblTotal.setText(String.valueOf(userList.size()));
        lblVerifies.setText(String.valueOf(verifies));
        lblActifs.setText(String.valueOf(actifs));
    }

    private void updateCharts() {
        Map<String, Long> roleCount = userList.stream()
                .collect(Collectors.groupingBy(
                        u -> parseRole(u.getRoles()).isEmpty() ? "Aucun" : parseRole(u.getRoles()),
                        Collectors.counting()));
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        roleCount.forEach((r, c) -> pieData.add(new PieChart.Data(r + " (" + c + ")", c)));
        pieRoles.setData(pieData);
        pieRoles.setTitle("Par Rôle");

        Map<String, Long> specCount = userList.stream()
                .filter(u -> u.getSpecialite() != null && !u.getSpecialite().isEmpty())
                .collect(Collectors.groupingBy(User::getSpecialite, Collectors.counting()));
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Utilisateurs");
        specCount.forEach((s, c) -> series.getData().add(new XYChart.Data<>(s, c)));
        barSpecialites.getData().clear();
        barSpecialites.getData().add(series);
        barSpecialites.setTitle("Par Spécialité");
    }

    private void showFormDialog(User existing) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Nouvel utilisateur" : "Modifier utilisateur #" + existing.getId());
        dialog.setHeaderText(existing == null ? "Remplissez les informations" : "Modifiez les informations");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setPrefWidth(550);

        TextField tfNom = new TextField(); tfNom.setPromptText("Nom");
        TextField tfPrenom = new TextField(); tfPrenom.setPromptText("Prénom");
        TextField tfEmail = new TextField(); tfEmail.setPromptText("Email");
        TextField tfUserName = new TextField(); tfUserName.setPromptText("Username");
        PasswordField pfPassword = new PasswordField();
        pfPassword.setPromptText(existing == null ? "Mot de passe" : "Nouveau mot de passe (vide = garder)");
        ComboBox<String> cbRole = new ComboBox<>(FXCollections.observableArrayList(
                "ROLE_USER", "ROLE_MEDECIN", "ROLE_ADMIN", "ROLE_PATIENT"));
        TextField tfSpecialite = new TextField(); tfSpecialite.setPromptText("Spécialité");
        TextArea taDescSpec = new TextArea(); taDescSpec.setPrefRowCount(2); taDescSpec.setPromptText("Description spécialité");
        TextField tfAdresse = new TextField(); tfAdresse.setPromptText("Adresse");
        TextField tfGrade = new TextField(); tfGrade.setPromptText("Grade");
        CheckBox chkVerifie = new CheckBox("Vérifié");
        CheckBox chkEtat = new CheckBox("Actif");
        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11; -fx-font-weight: bold;");
        lblError.setWrapText(true);

        if (existing != null) {
            tfNom.setText(existing.getNom());
            tfPrenom.setText(existing.getPrenom());
            tfEmail.setText(existing.getEmail());
            tfUserName.setText(existing.getUserName());
            cbRole.setValue(parseRole(existing.getRoles()));
            tfSpecialite.setText(existing.getSpecialite());
            taDescSpec.setText(existing.getDescriptionSpecialite());
            tfAdresse.setText(existing.getAdresse());
            tfGrade.setText(existing.getGrade());
            chkVerifie.setSelected(existing.isVerified());
            chkEtat.setSelected(Boolean.TRUE.equals(existing.getEtat()));
        }

        grid.add(new Label("Nom *"), 0, 0); grid.add(tfNom, 1, 0);
        grid.add(new Label("Prénom *"), 0, 1); grid.add(tfPrenom, 1, 1);
        grid.add(new Label("Email *"), 0, 2); grid.add(tfEmail, 1, 2);
        grid.add(new Label("Username"), 0, 3); grid.add(tfUserName, 1, 3);
        grid.add(new Label("Mot de passe" + (existing == null ? " *" : "")), 0, 4); grid.add(pfPassword, 1, 4);
        grid.add(new Label("Rôle *"), 0, 5); grid.add(cbRole, 1, 5);
        grid.add(new Label("Spécialité"), 0, 6); grid.add(tfSpecialite, 1, 6);
        grid.add(new Label("Description"), 0, 7); grid.add(taDescSpec, 1, 7);
        grid.add(new Label("Adresse"), 0, 8); grid.add(tfAdresse, 1, 8);
        grid.add(new Label("Grade"), 0, 9); grid.add(tfGrade, 1, 9);
        HBox checks = new HBox(15, chkVerifie, chkEtat);
        grid.add(checks, 0, 10, 2, 1);
        grid.add(lblError, 0, 11, 2, 1);

        dialog.getDialogPane().setContent(grid);

        final javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            StringBuilder errors = new StringBuilder();
            if (tfNom.getText().trim().isEmpty()) errors.append("• Nom obligatoire\n");
            if (tfPrenom.getText().trim().isEmpty()) errors.append("• Prénom obligatoire\n");
            String email = tfEmail.getText().trim();
            if (email.isEmpty()) errors.append("• Email obligatoire\n");
            else if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) errors.append("• Email invalide\n");
            else if (userService.emailExists(email, existing != null ? existing.getId() : 0)) errors.append("• Email déjà utilisé\n");
            if (existing == null && pfPassword.getText().trim().isEmpty()) errors.append("• Mot de passe obligatoire\n");
            if (cbRole.getValue() == null) errors.append("• Rôle obligatoire\n");

            if (errors.length() > 0) {
                lblError.setText(errors.toString());
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                User u = existing != null ? existing : new User();
                u.setNom(tfNom.getText().trim());
                u.setPrenom(tfPrenom.getText().trim());
                u.setEmail(tfEmail.getText().trim());
                u.setUserName(tfUserName.getText().trim().isEmpty() ? null : tfUserName.getText().trim());
                if (!pfPassword.getText().isEmpty()) u.setPassword(pfPassword.getText());
                u.setRoles("[\"" + cbRole.getValue() + "\"]");
                u.setSpecialite(tfSpecialite.getText().trim().isEmpty() ? null : tfSpecialite.getText().trim());
                u.setDescriptionSpecialite(taDescSpec.getText() != null && !taDescSpec.getText().trim().isEmpty() ? taDescSpec.getText().trim() : null);
                u.setAdresse(tfAdresse.getText().trim().isEmpty() ? null : tfAdresse.getText().trim());
                u.setGrade(tfGrade.getText().trim().isEmpty() ? null : tfGrade.getText().trim());
                u.setVerified(chkVerifie.isSelected());
                u.setEtat(chkEtat.isSelected());
                return u;
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();
        result.ifPresent(u -> {
            if (existing == null) {
                userService.save(u);
                showSuccess("Utilisateur ajouté !");
            } else {
                userService.update(u);
                showSuccess("Utilisateur modifié !");
            }
            loadData();
        });
    }

    @FXML private void handleAdd() { showFormDialog(null); }

    @FXML private void handleUpdate() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Sélectionnez un utilisateur."); return; }
        showFormDialog(selected);
    }

    @FXML private void handleDelete() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Sélectionnez un utilisateur."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer " + selected.getEmail() + " ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) { userService.delete(selected.getId()); loadData(); showSuccess("Supprimé !"); }
        });
    }

    @FXML private void handleResetFilters() {
        tfRecherche.clear(); cbFiltreRole.setValue("Tous"); cbFiltreEtat.setValue("Tous");
        tableUsers.setItems(userList);
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vous déconnecter ?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
                    Pane root = loader.load();
                    Stage stage = (Stage) tableUsers.getScene().getWindow();
                    Scene scene = new Scene(root, 900, 550);
                    scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
                    stage.setTitle("MyUpskilly - Connexion");
                    stage.setResizable(false);
                    stage.setScene(scene);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void applyFilters() {
        String keyword = tfRecherche.getText();
        String role = cbFiltreRole.getValue();
        String etat = cbFiltreEtat.getValue();

        ObservableList<User> filtered = userList.filtered(u -> {
            boolean matchKw = keyword == null || keyword.isEmpty() ||
                    (u.getNom() != null && u.getNom().toLowerCase().contains(keyword.toLowerCase())) ||
                    (u.getPrenom() != null && u.getPrenom().toLowerCase().contains(keyword.toLowerCase())) ||
                    (u.getEmail() != null && u.getEmail().toLowerCase().contains(keyword.toLowerCase()));
            boolean matchRole = role == null || role.equals("Tous") || parseRole(u.getRoles()).equals(role);
            boolean matchEtat = etat == null || etat.equals("Tous") ||
                    (etat.equals("Actif") && Boolean.TRUE.equals(u.getEtat())) ||
                    (etat.equals("Inactif") && !Boolean.TRUE.equals(u.getEtat()));
            return matchKw && matchRole && matchEtat;
        });
        tableUsers.setItems(filtered);
    }

    @FXML
    private void handleAiAssistant() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ai_assistant.fxml"));
            Pane root = loader.load();
            AiAssistantController ctrl = loader.getController();
            Stage stage = (Stage) tableUsers.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 620));
            stage.setTitle("MyUpskilly - Assistant IA");
            stage.setResizable(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String msg) { new Alert(Alert.AlertType.WARNING, msg).showAndWait(); }
    private void showSuccess(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
}