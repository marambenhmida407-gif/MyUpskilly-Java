package controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import models.PrescriptionMedicale;
import models.SuiviTherapeutique;
import services.ServicePrescriptionMedicale;
import services.ServiceSuiviTherapeutique;
import utils.Session;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class DashboardController implements Initializable {

    // ── Welcome ──────────────────────────────────────────────────────────────
    @FXML private Label lblWelcome;

    // ── Suivi stat labels ────────────────────────────────────────────────────
    @FXML private Label lblSuiviTotal;
    @FXML private Label lblSuiviEnCours;
    @FXML private Label lblSuiviTermine;
    @FXML private Label lblSuiviSuspendu;
    @FXML private Label lblSuiviPlanifie;

    // ── Prescription stat labels ─────────────────────────────────────────────
    @FXML private Label lblPrescTotal;
    @FXML private Label lblPrescAvec;
    @FXML private Label lblPrescSans;
    @FXML private Label lblPrescSuivis;

    // ── Charts ───────────────────────────────────────────────────────────────
    @FXML private PieChart pieStatut;
    @FXML private BarChart<String, Number> barPrescriptions;
    @FXML private CategoryAxis barXAxis;
    @FXML private NumberAxis   barYAxis;

    // ── Alerts ───────────────────────────────────────────────────────────────
    @FXML private javafx.scene.layout.VBox alertsBox;

    private final ServiceSuiviTherapeutique   suiviService = new ServiceSuiviTherapeutique();
    private final ServicePrescriptionMedicale prescService = new ServicePrescriptionMedicale();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Personalized welcome
        String prenom = Session.getPrenom();
        String nom    = Session.getNom();
        String role   = Session.getRole();
        if (prenom != null && !prenom.isEmpty()) {
            String prefix = "doctor".equals(role) ? "Bonjour, Dr " : "Bonjour, ";
            lblWelcome.setText(prefix + prenom + " " + (nom != null ? nom : ""));
        } else {
            lblWelcome.setText("Vue d'ensemble");
        }

        loadSuiviStats();
        loadPrescriptionStats();
        loadAlerts();
        setupScrollFix();
    }

    // ── Quick action navigation ───────────────────────────────────────────────

    @FXML
    private void goToSuivi(MouseEvent e) {
        if (MainLayoutController.instance != null)
            MainLayoutController.instance.navigateSuivi();
    }

    @FXML
    private void goToPrescription(MouseEvent e) {
        if (MainLayoutController.instance != null)
            MainLayoutController.instance.navigatePrescription();
    }

    @FXML
    private void goToConsultations(MouseEvent e) {
        if (MainLayoutController.instance != null)
            MainLayoutController.instance.navigateConsultation();
    }

    @FXML
    private void goToPathologies(MouseEvent e) {
        if (MainLayoutController.instance != null)
            MainLayoutController.instance.navigatePathologie();
    }

    // ── Suivi stats + pie chart ───────────────────────────────────────────────
    private void loadSuiviStats() {
        try {
            ArrayList<SuiviTherapeutique> list = suiviService.afficherAll();
            int total = 0, enCours = 0, termine = 0, suspendu = 0, planifie = 0;

            for (SuiviTherapeutique s : list) {
                total++;
                switch (s.getStatut()) {
                    case "En cours" -> enCours++;
                    case "Terminé"  -> termine++;
                    case "Suspendu" -> suspendu++;
                    case "Planifié" -> planifie++;
                }
            }

            lblSuiviTotal.setText(String.valueOf(total));
            lblSuiviEnCours.setText(String.valueOf(enCours));
            lblSuiviTermine.setText(String.valueOf(termine));
            lblSuiviSuspendu.setText(String.valueOf(suspendu));
            lblSuiviPlanifie.setText(String.valueOf(planifie));

            pieStatut.getData().clear();
            final int finalTotal = total; // effectively final for lambda capture
            if (total > 0) {
                if (enCours  > 0) pieStatut.getData().add(new PieChart.Data("En cours ("  + enCours  + ")", enCours));
                if (termine  > 0) pieStatut.getData().add(new PieChart.Data("Terminé ("   + termine  + ")", termine));
                if (suspendu > 0) pieStatut.getData().add(new PieChart.Data("Suspendu ("  + suspendu + ")", suspendu));
                if (planifie > 0) pieStatut.getData().add(new PieChart.Data("Planifié ("  + planifie + ")", planifie));

                Platform.runLater(() -> {
                    for (PieChart.Data data : pieStatut.getData()) {
                        String name = data.getName();
                        String color;
                        if      (name.startsWith("En cours"))  color = "#10B981";
                        else if (name.startsWith("Terminé"))   color = "#64748B";
                        else if (name.startsWith("Suspendu"))  color = "#F59E0B";
                        else if (name.startsWith("Planifié"))  color = "#3B82F6";
                        else                                    color = "#94A3B8";
                        data.getNode().setStyle("-fx-pie-color: " + color + ";");

                        javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip(
                                data.getName() + "\n" + Math.round(data.getPieValue() / finalTotal * 100) + "%");
                        tip.setStyle("-fx-font-size: 12px;");
                        javafx.scene.control.Tooltip.install(data.getNode(), tip);
                    }
                });
            } else {
                pieStatut.getData().add(new PieChart.Data("Aucun suivi", 1));
            }

        } catch (SQLException e) {
            System.err.println("Dashboard suivi error: " + e.getMessage());
        }
    }

    // ── Prescription stats + bar chart ───────────────────────────────────────
    private void loadPrescriptionStats() {
        try {
            ArrayList<PrescriptionMedicale> list = prescService.afficherAll();
            int total = 0, avec = 0, sans = 0;
            Set<Integer> suiviIds = new HashSet<>();
            Map<Integer, Integer> prescParSuivi = new HashMap<>();

            for (PrescriptionMedicale p : list) {
                total++;
                if (p.getRecommandations() != null && !p.getRecommandations().trim().isEmpty()) avec++;
                else sans++;
                suiviIds.add(p.getSuiviTherapeutiqueId());
                prescParSuivi.merge(p.getSuiviTherapeutiqueId(), 1, Integer::sum);
            }

            lblPrescTotal.setText(String.valueOf(total));
            lblPrescAvec.setText(String.valueOf(avec));
            lblPrescSans.setText(String.valueOf(sans));
            lblPrescSuivis.setText(String.valueOf(suiviIds.size()));

            barPrescriptions.getData().clear();
            barXAxis.setLabel("ID Suivi");
            barYAxis.setLabel("Nb prescriptions");
            barYAxis.setTickUnit(1);
            barYAxis.setMinorTickVisible(false);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Prescriptions");

            prescParSuivi.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> series.getData().add(
                            new XYChart.Data<>("Suivi #" + entry.getKey(), entry.getValue())));

            barPrescriptions.getData().add(series);

            Platform.runLater(() -> {
                for (XYChart.Data<String, Number> bar : series.getData()) {
                    if (bar.getNode() != null) {
                        bar.getNode().setStyle("-fx-bar-fill: #3B82F6;");
                        javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip(
                                bar.getXValue() + "\n" + bar.getYValue() + " prescription(s)");
                        tip.setStyle("-fx-font-size: 12px;");
                        javafx.scene.control.Tooltip.install(bar.getNode(), tip);
                    }
                }
            });

        } catch (SQLException e) {
            System.err.println("Dashboard prescription error: " + e.getMessage());
        }
    }

    // ── Expiry alerts ─────────────────────────────────────────────────────────
    private void loadAlerts() {
        if (alertsBox == null) return;
        alertsBox.getChildren().clear();
        try {
            ArrayList<SuiviTherapeutique> list = suiviService.afficherAll();
            java.time.LocalDate today = java.time.LocalDate.now();

            for (SuiviTherapeutique s : list) {
                if (s.getDateFin() == null) continue;
                java.time.LocalDate fin = s.getDateFin().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, fin);
                if (daysLeft < 0) continue;

                if (daysLeft <= 3) {
                    alertsBox.getChildren().add(buildAlert(s.getTypeSuivi(), daysLeft,
                            "#FEF2F2", "#EF4444", "#DC2626", "🔴"));
                } else if (daysLeft <= 7) {
                    alertsBox.getChildren().add(buildAlert(s.getTypeSuivi(), daysLeft,
                            "#FFFBEB", "#F59E0B", "#D97706", "🟡"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Alert load error: " + e.getMessage());
        }
    }

    private javafx.scene.layout.HBox buildAlert(String typeSuivi, long daysLeft,
                                                String bg, String border, String textColor, String icon) {
        String dayText = daysLeft == 0 ? "aujourd'hui !"
                : daysLeft == 1 ? "demain !"
                  : "dans " + daysLeft + " jours";

        javafx.scene.control.Label iconLbl = new javafx.scene.control.Label(icon);
        iconLbl.setStyle("-fx-font-size: 14px;");

        javafx.scene.control.Label msgLbl = new javafx.scene.control.Label(
                "Suivi \"" + typeSuivi + "\" se termine " + dayText);
        msgLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.control.Label daysLbl = new javafx.scene.control.Label(
                daysLeft == 0 ? "Expire aujourd'hui" : daysLeft + "j restants");
        daysLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");

        javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(10, iconLbl, msgLbl, spacer, daysLbl);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.setStyle("-fx-background-color: " + bg + ";"
                + "-fx-border-color: " + border + ";"
                + "-fx-border-width: 1; -fx-border-radius: 8;"
                + "-fx-background-radius: 8; -fx-padding: 12 16;");
        return box;
    }

    // ── Scroll speed fix ──────────────────────────────────────────────────────
    private void setupScrollFix() {
        lblSuiviTotal.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> {
                    ScrollPane sp = (ScrollPane) newScene.lookup(".scroll-pane");
                    if (sp == null) return;
                    sp.addEventFilter(ScrollEvent.SCROLL, e -> {
                        double delta     = e.getDeltaY() * 4;
                        double contentH  = sp.getContent().getBoundsInLocal().getHeight();
                        double viewportH = sp.getViewportBounds().getHeight();
                        double scrollable = contentH - viewportH;
                        if (scrollable <= 0) return;
                        sp.setVvalue(sp.getVvalue() - delta / scrollable);
                        e.consume();
                    });
                });
            }
        });
    }
}