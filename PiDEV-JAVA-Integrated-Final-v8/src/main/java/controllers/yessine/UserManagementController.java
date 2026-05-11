package controllers.yessine;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import models.User;
import services.yessine.UserService;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class UserManagementController implements Initializable {

    // ── TableView ─────────────────────────────────────────────────────────────
    @FXML private TableView<User>            tableUsers;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String>  colNom;
    @FXML private TableColumn<User, String>  colPrenom;
    @FXML private TableColumn<User, String>  colEmail;
    @FXML private TableColumn<User, String>  colRole;
    @FXML private TableColumn<User, String>  colSpecialite;
    @FXML private TableColumn<User, Boolean> colVerifie;
    @FXML private TableColumn<User, String>  colEtat;

    // ── Filtres ───────────────────────────────────────────────────────────────
    @FXML private TextField        tfRecherche;
    @FXML private ComboBox<String> cbFiltreRole;
    @FXML private ComboBox<String> cbFiltreEtat;

    // ── Stats ─────────────────────────────────────────────────────────────────
    @FXML private Label    lblTotal;
    @FXML private Label    lblVerifies;
    @FXML private Label    lblActifs;
    @FXML private PieChart pieRoles;
    @FXML private BarChart<String, Number> barSpecialites;

    private final UserService userService = new UserService();
    private ObservableList<User> userList;

    // ── Callback pour notifier AdminController après chaque changement ────────
    private Runnable onChangeCallback;

    public void setOnChangeCallback(Runnable callback) {
        this.onChangeCallback = callback;
    }

    private void notifyChange() {
        if (onChangeCallback != null) onChangeCallback.run();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        setupFilters();

        // Double-clic → édition
        tableUsers.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && tableUsers.getSelectionModel().getSelectedItem() != null)
                showFormDialog(tableUsers.getSelectionModel().getSelectedItem());
        });

        loadData();
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private void setupColumns() {
        colId.setCellValueFactory(        c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colNom.setCellValueFactory(       c -> new SimpleStringProperty(c.getValue().getNom()));
        colPrenom.setCellValueFactory(    c -> new SimpleStringProperty(c.getValue().getPrenom()));
        colEmail.setCellValueFactory(     c -> new SimpleStringProperty(c.getValue().getEmail()));
        colRole.setCellValueFactory(      c -> new SimpleStringProperty(c.getValue().getRole()));
        colSpecialite.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSpecialite()));
        colVerifie.setCellValueFactory(   c -> new SimpleBooleanProperty(c.getValue().isVerified()).asObject());
        colEtat.setCellValueFactory(      c -> new SimpleStringProperty(
                c.getValue().getEtat() == null ? "—"
                        : (Boolean.TRUE.equals(c.getValue().getEtat()) ? "✅ Actif" : "🚫 Inactif")));
    }

    private void setupFilters() {
        cbFiltreRole.setItems(FXCollections.observableArrayList(
                "Tous", "patient", "doctor", "admin"));
        cbFiltreRole.setValue("Tous");

        cbFiltreEtat.setItems(FXCollections.observableArrayList("Tous", "Actif", "Inactif"));
        cbFiltreEtat.setValue("Tous");

        if (tfRecherche != null)
            tfRecherche.textProperty().addListener((obs, o, n) -> applyFilters());
        cbFiltreRole.valueProperty().addListener((obs, o, n) -> applyFilters());
        cbFiltreEtat.valueProperty().addListener((obs, o, n) -> applyFilters());
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        userList = FXCollections.observableArrayList(userService.findAll());
        tableUsers.setItems(userList);
        updateStats();
        updateCharts();
    }

    private void updateStats() {
        long verifies = userList.stream().filter(User::isVerified).count();
        long actifs   = userList.stream().filter(u -> Boolean.TRUE.equals(u.getEtat())).count();
        if (lblTotal    != null) lblTotal.setText(String.valueOf(userList.size()));
        if (lblVerifies != null) lblVerifies.setText(String.valueOf(verifies));
        if (lblActifs   != null) lblActifs.setText(String.valueOf(actifs));
    }

    private void updateCharts() {
        if (pieRoles != null) {
            Map<String, Long> roleCount = userList.stream()
                    .collect(Collectors.groupingBy(
                            u -> u.getRole() == null ? "Aucun" : u.getRole(),
                            Collectors.counting()));
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            roleCount.forEach((r, c) -> pieData.add(new PieChart.Data(r + " (" + c + ")", c)));
            pieRoles.setData(pieData);
        }

        if (barSpecialites != null) {
            Map<String, Long> specCount = userList.stream()
                    .filter(u -> u.getSpecialite() != null && !u.getSpecialite().isEmpty())
                    .collect(Collectors.groupingBy(User::getSpecialite, Collectors.counting()));
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            specCount.forEach((s, c) -> series.getData().add(new XYChart.Data<>(s, c)));
            barSpecialites.getData().clear();
            barSpecialites.getData().add(series);
        }
    }

    private void applyFilters() {
        String keyword = tfRecherche != null ? tfRecherche.getText() : "";
        String role    = cbFiltreRole.getValue();
        String etat    = cbFiltreEtat.getValue();

        tableUsers.setItems(userList.filtered(u -> {
            boolean matchKw = keyword == null || keyword.isEmpty()
                    || (u.getNom()   != null && u.getNom().toLowerCase().contains(keyword.toLowerCase()))
                    || (u.getEmail() != null && u.getEmail().toLowerCase().contains(keyword.toLowerCase()));
            boolean matchRole = role == null || "Tous".equals(role) || role.equals(u.getRole());
            boolean matchEtat = etat == null || "Tous".equals(etat)
                    || ("Actif".equals(etat)   &&  Boolean.TRUE.equals(u.getEtat()))
                    || ("Inactif".equals(etat) && !Boolean.TRUE.equals(u.getEtat()));
            return matchKw && matchRole && matchEtat;
        }));
    }

    // ── CRUD Actions ──────────────────────────────────────────────────────────

    @FXML private void handleAdd() { showFormDialog(null); }

    @FXML private void handleDelete() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Sélectionnez un utilisateur à supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer " + selected.getPrenom() + " " + selected.getNom() + " ?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                userService.delete(selected.getId());
                loadData();
                notifyChange(); // ← notifie AdminController pour rafraîchir les stats
            }
        });
    }

    @FXML private void handleToggleEtat() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Sélectionnez un utilisateur.");
            return;
        }
        selected.setEtat(!Boolean.TRUE.equals(selected.getEtat()));
        userService.update(selected);
        loadData();
        notifyChange(); // ← notifie AdminController
    }

    @FXML private void handleRefresh() {
        loadData();
        notifyChange(); // ← rafraîchit aussi les stats en haut
    }

    // ── Formulaire d'édition ──────────────────────────────────────────────────

    private void showFormDialog(User existing) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Nouvel utilisateur" : "Modifier — " + existing.getEmail());

        ButtonType saveBtn = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField        tfNom    = new TextField();
        TextField        tfPrenom = new TextField();
        TextField        tfEmail  = new TextField();
        PasswordField    pfPass   = new PasswordField();
        ComboBox<String> cbRole   = new ComboBox<>(
                FXCollections.observableArrayList("patient", "doctor", "admin"));
        TextField        tfSpec   = new TextField();
        CheckBox         cbActif  = new CheckBox("Compte actif");
        CheckBox         cbVerif  = new CheckBox("Compte vérifié");

        if (existing != null) {
            tfNom.setText(existing.getNom());
            tfPrenom.setText(existing.getPrenom());
            tfEmail.setText(existing.getEmail());
            cbRole.setValue(existing.getRole());
            tfSpec.setText(existing.getSpecialite() != null ? existing.getSpecialite() : "");
            cbActif.setSelected(Boolean.TRUE.equals(existing.getEtat()));
            cbVerif.setSelected(existing.isVerified());
        } else {
            cbRole.setValue("patient");
            cbActif.setSelected(true);
        }

        int row = 0;
        grid.add(new Label("Nom"),        0, row); grid.add(tfNom,    1, row++);
        grid.add(new Label("Prénom"),     0, row); grid.add(tfPrenom, 1, row++);
        grid.add(new Label("Email"),      0, row); grid.add(tfEmail,  1, row++);
        grid.add(new Label("Mot de passe (laisser vide = inchangé)"),
                0, row); grid.add(pfPass,   1, row++);
        grid.add(new Label("Rôle"),       0, row); grid.add(cbRole,   1, row++);
        grid.add(new Label("Spécialité"), 0, row); grid.add(tfSpec,   1, row++);
        grid.add(cbActif, 0, row, 2, 1); row++;
        grid.add(cbVerif, 0, row, 2, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                User u = existing != null ? existing : new User();
                u.setNom(tfNom.getText().trim());
                u.setPrenom(tfPrenom.getText().trim());
                u.setEmail(tfEmail.getText().trim());
                if (!pfPass.getText().isEmpty()) u.setPassword(pfPass.getText());
                u.setRole(cbRole.getValue());
                u.setSpecialite(tfSpec.getText().trim());
                u.setEtat(cbActif.isSelected());
                u.setVerified(cbVerif.isSelected());
                return u;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(u -> {
            try {
                if (existing == null) {
                    userService.save(u);
                    showAlert(Alert.AlertType.INFORMATION, "Utilisateur créé avec succès.");
                } else {
                    userService.update(u);
                    showAlert(Alert.AlertType.INFORMATION, "Utilisateur mis à jour.");
                }
                loadData();
                notifyChange(); // ← notifie AdminController après ajout/modif
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage());
            }
        });
    }

    private void showAlert(Alert.AlertType type, String msg) {
        new Alert(type, msg).showAndWait();
    }
}