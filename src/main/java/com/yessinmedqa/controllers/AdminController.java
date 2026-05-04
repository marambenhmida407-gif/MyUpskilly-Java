package com.yessinmedqa.controllers;

import com.yessinmedqa.Session;
import com.yessinmedqa.services.AdminService;
import com.yessinmedqa.services.ModerationService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AdminController {

    @FXML private VBox  contentArea;
    @FXML private Label sectionTitle;
    @FXML private Label statUsersCount;
    @FXML private Label statQuestCount;
    @FXML private Label statRepCount;
    @FXML private Label statSignalCount;

    private final AdminService      adminService      = new AdminService();
    private final ModerationService moderationService = new ModerationService();

    @FXML
    public void initialize() {
        loadStats();
        showDashboard(null);
    }

    // ── STATS ────────────────────────────────────────────────
    private void loadStats() {
        try {
            Map<String, Integer> stats = adminService.getStats();
            statUsersCount.setText(String.valueOf(stats.getOrDefault("users", 0)));
            statQuestCount.setText(String.valueOf(stats.getOrDefault("questions", 0)));
            statRepCount.setText(String.valueOf(stats.getOrDefault("reponses", 0)));
            statSignalCount.setText(String.valueOf(stats.getOrDefault("reports", 0)));
        } catch (Exception e) {
            System.err.println("Stats error: " + e.getMessage());
        }
    }

    // ── DASHBOARD ────────────────────────────────────────────
    @FXML
    public void showDashboard(ActionEvent event) {
        sectionTitle.setText("📊 Dashboard");
        contentArea.getChildren().clear();

        Label info = new Label("Bienvenue dans le panneau d'administration.\n" +
                "Utilisez le menu à gauche pour gérer la plateforme.");
        info.setStyle("-fx-font-size: 13px; -fx-text-fill: #555; -fx-wrap-text: true;");
        contentArea.getChildren().add(info);
        loadStats();
    }

    // ── USERS ────────────────────────────────────────────────
    @FXML
    public void showUsers(ActionEvent event) {
        sectionTitle.setText("👥 Gestion des Utilisateurs");
        contentArea.getChildren().clear();

        try {
            List<Map<String, String>> users = adminService.getAllUsers();

            if (users.isEmpty()) {
                contentArea.getChildren().add(emptyLabel("Aucun utilisateur."));
                return;
            }

            for (Map<String, String> u : users) {
                HBox row = new HBox(12);
                row.setStyle("-fx-background-color: white; -fx-padding: 14; -fx-background-radius: 10;");
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Label icon = new Label(
                        "doctor".equalsIgnoreCase(u.get("role")) ? "👨‍⚕️" : "👤");
                icon.setStyle("-fx-font-size: 22px;");

                VBox info = new VBox(2);
                Label name = new Label(u.get("prenom") + " " + u.get("nom"));
                name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                Label email = new Label(u.get("email") + " — " + u.get("role"));
                email.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
                info.getChildren().addAll(name, email);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button deleteBtn = new Button("🗑️ Supprimer");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;" +
                        "-fx-background-radius: 7; -fx-font-size: 11px;");
                deleteBtn.setOnAction(e -> {
                    Alert confirm = confirmDialog("Supprimer cet utilisateur ?");
                    confirm.showAndWait().ifPresent(r -> {
                        if (r == ButtonType.OK) {
                            try {
                                adminService.deleteUser(Integer.parseInt(u.get("id")));
                                showUsers(null);
                                loadStats();
                            } catch (Exception ex) {
                                System.err.println(ex.getMessage());
                            }
                        }
                    });
                });

                row.getChildren().addAll(icon, info, spacer, deleteBtn);
                contentArea.getChildren().add(row);
            }

        } catch (Exception e) {
            contentArea.getChildren().add(emptyLabel("Erreur: " + e.getMessage()));
        }
    }

    // ── QUESTIONS ────────────────────────────────────────────
    @FXML
    public void showQuestions(ActionEvent event) {
        sectionTitle.setText("❓ Gestion des Questions");
        contentArea.getChildren().clear();

        try {
            List<Map<String, String>> questions = adminService.getAllQuestions();

            if (questions.isEmpty()) {
                contentArea.getChildren().add(emptyLabel("Aucune question."));
                return;
            }

            for (Map<String, String> q : questions) {
                VBox card = new VBox(6);
                card.setStyle("-fx-background-color: white; -fx-padding: 14;" +
                        "-fx-background-radius: 10;");

                Label titre = new Label(q.get("titre"));
                titre.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;" +
                        "-fx-text-fill: #0a5f7a;");
                titre.setWrapText(true);

                Label desc = new Label(q.get("description"));
                desc.setStyle("-fx-text-fill: #555; -fx-font-size: 11px;");
                desc.setWrapText(true);

                Label meta = new Label("👤 Patient ID: " + q.get("patientId") +
                        "  |  📂 " + q.get("categorie"));
                meta.setStyle("-fx-text-fill: #999; -fx-font-size: 10px;");

                // Flag indicator
                if ("true".equals(q.get("flagged"))) {
                    Label flagged = new Label("⚠️ Signalée automatiquement");
                    flagged.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 10px;" +
                            "-fx-font-weight: bold;");
                    card.getChildren().add(flagged);
                }

                Button deleteBtn = new Button("🗑️ Supprimer cette question");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;" +
                        "-fx-background-radius: 7; -fx-font-size: 11px;");
                deleteBtn.setOnAction(e -> {
                    Alert confirm = confirmDialog("Supprimer cette question et ses réponses ?");
                    confirm.showAndWait().ifPresent(r -> {
                        if (r == ButtonType.OK) {
                            try {
                                adminService.deleteQuestion(
                                        Integer.parseInt(q.get("id")));
                                showQuestions(null);
                                loadStats();
                            } catch (Exception ex) {
                                System.err.println(ex.getMessage());
                            }
                        }
                    });
                });

                card.getChildren().addAll(titre, desc, meta, deleteBtn);
                contentArea.getChildren().add(card);
            }

        } catch (Exception e) {
            contentArea.getChildren().add(emptyLabel("Erreur: " + e.getMessage()));
        }
    }

    // ── REPONSES ─────────────────────────────────────────────
    @FXML
    public void showReponses(ActionEvent event) {
        sectionTitle.setText("💬 Gestion des Réponses");
        contentArea.getChildren().clear();

        try {
            List<Map<String, String>> reponses = adminService.getAllReponses();

            if (reponses.isEmpty()) {
                contentArea.getChildren().add(emptyLabel("Aucune réponse."));
                return;
            }

            for (Map<String, String> r : reponses) {
                VBox card = new VBox(6);
                card.setStyle("-fx-background-color: white; -fx-padding: 14;" +
                        "-fx-background-radius: 10;");

                Label question = new Label("❓ " + r.get("questionTitre"));
                question.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;" +
                        "-fx-text-fill: #0a5f7a;");
                question.setWrapText(true);

                Label contenu = new Label(r.get("contenu"));
                contenu.setStyle("-fx-text-fill: #333; -fx-font-size: 11px;");
                contenu.setWrapText(true);

                Label meta = new Label("👨‍⚕️ Dr ID: " + r.get("doctorId"));
                meta.setStyle("-fx-text-fill: #999; -fx-font-size: 10px;");

                Button deleteBtn = new Button("🗑️ Supprimer cette réponse");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;" +
                        "-fx-background-radius: 7; -fx-font-size: 11px;");
                deleteBtn.setOnAction(e -> {
                    Alert confirm = confirmDialog("Supprimer cette réponse ?");
                    confirm.showAndWait().ifPresent(res -> {
                        if (res == ButtonType.OK) {
                            try {
                                adminService.deleteReponse(
                                        Integer.parseInt(r.get("id")));
                                showReponses(null);
                                loadStats();
                            } catch (Exception ex) {
                                System.err.println(ex.getMessage());
                            }
                        }
                    });
                });

                card.getChildren().addAll(question, contenu, meta, deleteBtn);
                contentArea.getChildren().add(card);
            }

        } catch (Exception e) {
            contentArea.getChildren().add(emptyLabel("Erreur: " + e.getMessage()));
        }
    }

    // ── REPORTS ──────────────────────────────────────────────
    @FXML
    public void showReports(ActionEvent event) {
        sectionTitle.setText("🚩 Signalements");
        contentArea.getChildren().clear();

        try {
            List<Map<String, String>> reports = moderationService.getAllReports();

            if (reports.isEmpty()) {
                contentArea.getChildren().add(emptyLabel("Aucun signalement."));
                return;
            }

            for (Map<String, String> r : reports) {
                VBox card = new VBox(8);
                card.setStyle(
                        "-fx-background-color: #fff8f8; -fx-padding: 14;" +
                                "-fx-background-radius: 10; -fx-border-color: #e74c3c;" +
                                "-fx-border-radius: 10; -fx-border-width: 1;");

                // ✅ Show type badge
                String type = r.get("type") != null ? r.get("type") : "question";
                Label typeBadge = new Label(
                        "feedback".equals(type) ? "💬 Signalement — Commentaire"
                                : "❓ Signalement — Question");
                typeBadge.setStyle(
                        "-fx-font-size: 11px; -fx-font-weight: bold;" +
                                "-fx-text-fill: " + ("feedback".equals(type)
                                ? "#8e44ad" : "#e74c3c") + ";");

                // ✅ Show what was reported
                Label contenu;
                if ("feedback".equals(type)) {
                    String comment = r.get("feedbackCommentaire");
                    contenu = new Label("💬 Commentaire: " +
                            (comment != null ? comment : "N/A"));
                } else {
                    String titre = r.get("questionTitre");
                    contenu = new Label("❓ Question: " +
                            (titre != null ? titre : "ID " + r.get("questionId")));
                }
                contenu.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;" +
                        "-fx-text-fill: #0a3d5c;");
                contenu.setWrapText(true);

                Label raison = new Label("⚠️ Raison: " + r.get("raison"));
                raison.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
                raison.setWrapText(true);

                Label status = new Label("Statut: " + r.get("status"));
                status.setStyle("-fx-text-fill: #888; -fx-font-size: 10px;");

                Label date = new Label("📅 " +
                        (r.get("date") != null && r.get("date").length() >= 10
                                ? r.get("date").substring(0, 10) : ""));
                date.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10px;");

                HBox btnRow = new HBox(8);

                if ("feedback".equals(type)) {
                    // ✅ Feedback report — delete comment or dismiss
                    Button deleteFeedbackBtn = new Button("🗑️ Supprimer le commentaire");
                    deleteFeedbackBtn.setStyle(
                            "-fx-background-color: #e74c3c; -fx-text-fill: white;" +
                                    "-fx-background-radius: 7; -fx-font-size: 11px;");
                    deleteFeedbackBtn.setOnAction(e -> {
                        Alert confirm = confirmDialog("Supprimer ce commentaire ?");
                        confirm.showAndWait().ifPresent(res -> {
                            if (res == ButtonType.OK) {
                                try {
                                    adminService.deleteFeedback(
                                            Integer.parseInt(r.get("feedbackId")));
                                    moderationService.updateReportStatus(
                                            Integer.parseInt(r.get("id")), "REVIEWED");
                                    showReports(null);
                                    loadStats();
                                } catch (Exception ex) {
                                    System.err.println(ex.getMessage());
                                }
                            }
                        });
                    });

                    Button dismissFeedbackBtn = new Button("✅ Ignorer");
                    dismissFeedbackBtn.setStyle(
                            "-fx-background-color: #1aaa8a; -fx-text-fill: white;" +
                                    "-fx-background-radius: 7; -fx-font-size: 11px;");
                    dismissFeedbackBtn.setOnAction(e -> {
                        try {
                            moderationService.updateReportStatus(
                                    Integer.parseInt(r.get("id")), "DISMISSED");
                            showReports(null);
                            loadStats();
                        } catch (Exception ex) {
                            System.err.println(ex.getMessage());
                        }
                    });

                    btnRow.getChildren().addAll(deleteFeedbackBtn, dismissFeedbackBtn);

                } else {
                    // ✅ Question report — delete question or dismiss
                    Button deleteQBtn = new Button("🗑️ Supprimer la question");
                    deleteQBtn.setStyle(
                            "-fx-background-color: #e74c3c; -fx-text-fill: white;" +
                                    "-fx-background-radius: 7; -fx-font-size: 11px;");
                    deleteQBtn.setOnAction(e -> {
                        Alert confirm = confirmDialog(
                                "Supprimer cette question et ses réponses ?");
                        confirm.showAndWait().ifPresent(res -> {
                            if (res == ButtonType.OK) {
                                try {
                                    adminService.deleteQuestion(
                                            Integer.parseInt(r.get("questionId")));
                                    moderationService.updateReportStatus(
                                            Integer.parseInt(r.get("id")), "REVIEWED");
                                    showReports(null);
                                    loadStats();
                                } catch (Exception ex) {
                                    System.err.println(ex.getMessage());
                                }
                            }
                        });
                    });

                    Button dismissBtn = new Button("✅ Ignorer");
                    dismissBtn.setStyle(
                            "-fx-background-color: #1aaa8a; -fx-text-fill: white;" +
                                    "-fx-background-radius: 7; -fx-font-size: 11px;");
                    dismissBtn.setOnAction(e -> {
                        try {
                            moderationService.updateReportStatus(
                                    Integer.parseInt(r.get("id")), "DISMISSED");
                            showReports(null);
                            loadStats();
                        } catch (Exception ex) {
                            System.err.println(ex.getMessage());
                        }
                    });

                    btnRow.getChildren().addAll(deleteQBtn, dismissBtn);
                }

                card.getChildren().addAll(
                        typeBadge, contenu, raison, status, date, btnRow);
                contentArea.getChildren().add(card);
            }

        } catch (Exception e) {
            contentArea.getChildren().add(emptyLabel("Erreur: " + e.getMessage()));
        }
    }

    // ── HELPERS ──────────────────────────────────────────────
    private Label emptyLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #888; -fx-font-size: 13px;");
        return l;
    }

    private Alert confirmDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(message);
        alert.setContentText("Cette action est irréversible.");
        return alert;
    }

    @FXML
    public void seDeconnecter(ActionEvent event) {
        try {
            Session.clear();
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/yessinmedqa/login.fxml"));
            sectionTitle.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }
}