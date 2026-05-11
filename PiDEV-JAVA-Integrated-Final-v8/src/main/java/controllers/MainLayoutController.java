package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import utils.Session;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class MainLayoutController implements Initializable {

    @FXML private StackPane contentPane;
    @FXML private Label     lblPageTitle;
    @FXML private Label     lblDate;
    @FXML private Label     lblUserName;
    @FXML private Button    btnLogout;

    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavSuivi;
    @FXML private Button btnNavPrescription;
    @FXML private Button btnNavMap;
    @FXML private Button btnNavCalendar;
    @FXML private Button btnNavStock;
    @FXML private Button btnNavMedical;
    @FXML private Button btnNavQuestionsRep;
    @FXML private Button btnNavConsultation;
    @FXML private Button btnNavPathologie;

    // Public so controllers in any package (yessine, aziz, etc.) can navigate
    public static MainLayoutController instance;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        instance = this;

        lblDate.setText(LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy",
                        java.util.Locale.FRENCH)));

        String prenom = Session.getPrenom();
        String nom    = Session.getNom();
        String role   = Session.getRole();
        if (prenom != null && !prenom.isEmpty()) {
            String prefix = "doctor".equals(role) ? "Dr " : "";
            lblUserName.setText("👤 " + prefix + prenom + " " + (nom != null ? nom : ""));
        }

        loadPage("/Dashboard.fxml", "Tableau de bord", btnNavDashboard);
    }

    // ── Public methods for DashboardController quick actions ──────────────────

    public void navigateSuivi() { showSuivi(); }
    public void navigatePrescription() { showPrescriptions(); }
    public void navigateConsultation() { showConsultations(); }
    public void navigatePathologie() { showPathologies(); }

    // ── FXML nav handlers ─────────────────────────────────────────────────────

    @FXML private void showDashboard() {
        loadPage("/Dashboard.fxml", "Tableau de bord", btnNavDashboard);
    }

    @FXML private void showSuivi() {
        loadPage("/SuiviList.fxml", "Suivi Thérapeutique", btnNavSuivi);
    }

    @FXML private void showPrescriptions() {
        loadPage("/PrescriptionList.fxml", "Prescriptions Médicales", btnNavPrescription);
    }

    @FXML private void showMap() {
        loadPage("/PharmacyMap.fxml", "Carte des Pharmacies", btnNavMap);
    }

    @FXML private void showCalendar() {
        loadPage("/CalendarView.fxml", "Calendrier des Suivis", btnNavCalendar);
    }

    @FXML private void showStockChecker() {
        loadPage("/StockChecker.fxml", "Vérificateur de Stock", btnNavStock);
    }

    @FXML private void showMedical() {
        loadPage("/yessine/espaceMedecin.fxml", "Questions Patients Q&A", btnNavMedical);
    }

    @FXML private void showQuestionsRepondues() {
        loadPage("/yessine/questionsRepondues.fxml", "Questions Répondues", btnNavQuestionsRep);
    }

    @FXML private void showConsultations() {
        loadPage("/aziz/consultation.fxml", "Gestion des Consultations", btnNavConsultation);
    }

    @FXML private void showPathologies() {
        loadPage("/aziz/pathologie.fxml", "Gestion des Pathologies", btnNavPathologie);
    }

    @FXML private void handleLogout() {
        Session.clear();
        instance = null;
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/yessine/login.fxml"));
            btnLogout.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Logout error: " + e.getMessage());
        }
    }

    // ── Core loader ───────────────────────────────────────────────────────────

    public void loadPage(String fxml, String title, Button active) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource(fxml));
            contentPane.getChildren().setAll(page);
            lblPageTitle.setText(title);

            Button[] navButtons = {
                    btnNavDashboard, btnNavSuivi, btnNavPrescription,
                    btnNavMap, btnNavCalendar, btnNavStock,
                    btnNavMedical, btnNavQuestionsRep,
                    btnNavConsultation, btnNavPathologie
            };
            for (Button b : navButtons) {
                if (b != null) b.getStyleClass().remove("nav-active");
            }
            if (active != null) active.getStyleClass().add("nav-active");

        } catch (IOException e) {
            System.err.println("Navigation error loading " + fxml + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
