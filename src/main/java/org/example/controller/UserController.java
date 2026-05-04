package org.example.controller;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import org.example.service.UserService;
import org.example.model.User;

import java.net.URL;
import java.util.*;
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

        Map<String, Long> specCount = userList.stream()
                .filter(u -> u.getSpecialite() != null && !u.getSpecialite().isEmpty())
                .collect(Collectors.groupingBy(User::getSpecialite, Collectors.counting()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        specCount.forEach((s, c) -> series.getData().add(new XYChart.Data<>(s, c)));

        barSpecialites.getData().clear();
        barSpecialites.getData().add(series);
    }

    private void showFormDialog(User existing) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Nouvel utilisateur" : "Modifier utilisateur #" + existing.getId());

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField tfNom = new TextField();
        TextField tfPrenom = new TextField();
        TextField tfEmail = new TextField();
        PasswordField pfPassword = new PasswordField();
        ComboBox<String> cbRole = new ComboBox<>(FXCollections.observableArrayList(
                "ROLE_USER", "ROLE_MEDECIN", "ROLE_ADMIN", "ROLE_PATIENT"));

        if (existing != null) {
            tfNom.setText(existing.getNom());
            tfPrenom.setText(existing.getPrenom());
            tfEmail.setText(existing.getEmail());
            cbRole.setValue(parseRole(existing.getRoles()));
        }

        grid.add(new Label("Nom"), 0, 0); grid.add(tfNom, 1, 0);
        grid.add(new Label("Prénom"), 0, 1); grid.add(tfPrenom, 1, 1);
        grid.add(new Label("Email"), 0, 2); grid.add(tfEmail, 1, 2);
        grid.add(new Label("Mot de passe"), 0, 3); grid.add(pfPassword, 1, 3);
        grid.add(new Label("Rôle"), 0, 4); grid.add(cbRole, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveButtonType) {
                User u = existing != null ? existing : new User();
                u.setNom(tfNom.getText());
                u.setPrenom(tfPrenom.getText());
                u.setEmail(tfEmail.getText());
                if (!pfPassword.getText().isEmpty()) u.setPassword(pfPassword.getText());
                u.setRoles("[\"" + cbRole.getValue() + "\"]");
                return u;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(u -> {
            if (existing == null) {
                userService.save(u);
                showSuccess("Ajouté !");
            } else {
                userService.update(u);
                showSuccess("Modifié !");
            }
            loadData();
        });
    }

    @FXML private void handleAdd() { showFormDialog(null); }

    @FXML private void handleDelete() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected != null) {
            userService.delete(selected.getId());
            loadData();
        }
    }

    private void applyFilters() {
        String keyword = tfRecherche.getText();
        tableUsers.setItems(userList.filtered(u ->
                keyword == null || keyword.isEmpty() ||
                u.getNom().toLowerCase().contains(keyword.toLowerCase())
        ));
    }

    private void showSuccess(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}