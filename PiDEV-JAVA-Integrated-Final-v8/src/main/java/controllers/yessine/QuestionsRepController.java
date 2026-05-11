package controllers.yessine;

import utils.Session;
import models.yessine.Reponse;
import services.yessine.ModerationService;
import services.yessine.QuestionService;
import services.yessine.ReponseService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.List;

public class QuestionsRepController {

    @FXML private VBox      cardsContainer;
    @FXML private TextField searchField;
    @FXML private VBox      patientCard;
    @FXML private Button    poserBtn;

    // ✅ FIX: bind the statRepondues label that was in FXML but unbound
    @FXML private Label statRepondues;

    private final ReponseService reponseService = new ReponseService();
    private List<Reponse> currentReponses;

    private void retourEspaceMedecin() {
        if (controllers.MainLayoutController.instance != null) {
            controllers.MainLayoutController.instance.loadPage(
                    "/yessine/espaceMedecin.fxml", "Questions Patients Q&A", null);
        } else {
            try {
                Parent root = FXMLLoader.load(getClass().getResource(
                        "/yessine/espaceMedecin.fxml"));
                searchField.getScene().setRoot(root);
            } catch (IOException e) {
                System.err.println("❌ Retour erreur: " + e.getMessage());
            }
        }
    }

    @FXML
    public void initialize() {
        if (Session.isDoctor()) {
            patientCard.setVisible(false);
            patientCard.setManaged(false);
            poserBtn.setText("← Retour aux questions");
            poserBtn.setOnAction(e -> retourEspaceMedecin());
        } else {
            poserBtn.setText("✏️  Poser une question");
        }

        chargerReponses("", null);
        updateStatLabel();
    }

    // ✅ FIX: update the statRepondues label with actual count
    private void updateStatLabel() {
        if (statRepondues == null) return;
        int count = reponseService.getRepondues("", null).size();
        statRepondues.setText(count + " questions répondues");
    }

    // ── LOAD QUESTIONS ───────────────────────────────────────
    private void chargerReponses(String keyword, String categorie) {
        currentReponses = reponseService.getRepondues(keyword, categorie);
        cardsContainer.getChildren().clear();

        if (currentReponses.isEmpty()) {
            Label empty = new Label("Aucune question répondue pour le moment.");
            empty.setStyle("-fx-text-fill: #888; -fx-font-size: 13px;");
            cardsContainer.getChildren().add(empty);
            return;
        }

        for (Reponse rep : currentReponses) {
            cardsContainer.getChildren().add(buildCard(rep));
        }

        updateStatLabel();
    }

    // ── BUILD CARD ───────────────────────────────────────────
    private VBox buildCard(Reponse rep) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 20;" +
                "-fx-background-radius: 14;" +
                "-fx-effect: dropshadow(gaussian, #b0d4d4, 8, 0, 0, 2);");

        // Category tag
        if (rep.getCategorie() != null) {
            Label cat = new Label("📂 " + rep.getCategorie());
            cat.setStyle("-fx-background-color: #e8f8e8; -fx-text-fill: #1aaa8a;" +
                    "-fx-font-size: 10px; -fx-font-weight: bold;" +
                    "-fx-padding: 3 8 3 8; -fx-background-radius: 10;");
            card.getChildren().add(cat);
        }

        // Question title
        Label titre = new Label(rep.getQuestionTitre());
        titre.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0a5f7a;");
        titre.setWrapText(true);

        // Description
        String desc = rep.getQuestionDescription();
        if (desc != null && desc.length() > 130) desc = desc.substring(0, 130) + "...";
        Label description = new Label(desc);
        description.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");
        description.setWrapText(true);

        card.getChildren().addAll(titre, description, new Separator());

        // ✅ Load ALL responses for this question
        List<Reponse> allReponses = reponseService.getReponsesByQuestion(rep.getQuestionId());

        if (allReponses.isEmpty()) {
            Label noRep = new Label("Aucune réponse pour le moment.");
            noRep.setStyle("-fx-text-fill: #aaa; -fx-font-size: 11px;");
            card.getChildren().add(noRep);
        } else {
            for (Reponse r : allReponses) {
                VBox reponseBlock = new VBox(6);
                reponseBlock.setStyle("-fx-background-color: #f0f8ff; -fx-padding: 12;" +
                        "-fx-background-radius: 10; -fx-border-color: #c8eaed;" +
                        "-fx-border-radius: 10; -fx-border-width: 1;");

                // Doctor info row
                HBox doctorRow = new HBox(10);
                doctorRow.setStyle("-fx-alignment: CENTER_LEFT;");

                Label avatar = new Label("👨‍⚕️");
                avatar.setStyle("-fx-font-size: 24px;");

                VBox doctorInfo = new VBox(2);
                Label doctorName = new Label("Dr " + r.getDoctorPrenom() + " " + r.getDoctorNom());
                doctorName.setStyle("-fx-font-weight: bold; -fx-text-fill: #1aaa8a;" +
                        "-fx-font-size: 13px;");
                Label specialite = new Label(r.getDoctorSpecialite() != null
                        ? r.getDoctorSpecialite() : "Médecin");
                specialite.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");
                doctorInfo.getChildren().addAll(doctorName, specialite);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Button voirBtn = new Button("Voir →");
                voirBtn.setStyle("-fx-background-color: #0a5f7a; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 8;" +
                        "-fx-pref-height: 30; -fx-font-size: 11px;");
                voirBtn.setOnAction(e -> ouvrirReponse(r));

                doctorRow.getChildren().addAll(avatar, doctorInfo, spacer, voirBtn);

                // Answer preview
                String preview = r.getContenu();
                if (preview != null && preview.length() > 120)
                    preview = preview.substring(0, 120) + "...";
                Label contenuPreview = new Label("💬 " + preview);
                contenuPreview.setStyle("-fx-text-fill: #444; -fx-font-size: 11px;");
                contenuPreview.setWrapText(true);

                reponseBlock.getChildren().addAll(doctorRow, contenuPreview);

                // Edit button for the doctor who wrote this response
                if (Session.isDoctor() && r.getDoctorId() == Session.getUserId()) {
                    TextArea editArea = new TextArea(r.getContenu());
                    editArea.setStyle("-fx-pref-height: 80; -fx-font-size: 12px;" +
                            "-fx-background-radius: 8; -fx-border-color: #c8eaed;" +
                            "-fx-border-radius: 8;");
                    editArea.setVisible(false);
                    editArea.setManaged(false);

                    Button editBtn = new Button("✏️ Modifier");
                    editBtn.setStyle("-fx-background-color: #f0a500; -fx-text-fill: white;" +
                            "-fx-font-weight: bold; -fx-background-radius: 8;" +
                            "-fx-pref-height: 30; -fx-font-size: 11px;");

                    Button saveBtn = new Button("💾 Sauvegarder");
                    saveBtn.setStyle("-fx-background-color: #1aaa8a; -fx-text-fill: white;" +
                            "-fx-font-weight: bold; -fx-background-radius: 8;" +
                            "-fx-pref-height: 30; -fx-font-size: 11px;");
                    saveBtn.setVisible(false);
                    saveBtn.setManaged(false);

                    Button cancelBtn = new Button("❌ Annuler");
                    cancelBtn.setStyle("-fx-background-color: #888; -fx-text-fill: white;" +
                            "-fx-background-radius: 8; -fx-pref-height: 30;" +
                            "-fx-font-size: 11px;");
                    cancelBtn.setVisible(false);
                    cancelBtn.setManaged(false);

                    Label editSuccess = new Label();
                    editSuccess.setStyle("-fx-text-fill: #1aaa8a; -fx-font-size: 11px;" +
                            "-fx-font-weight: bold;");

                    editBtn.setOnAction(e -> {
                        editArea.setVisible(true);  editArea.setManaged(true);
                        saveBtn.setVisible(true);   saveBtn.setManaged(true);
                        cancelBtn.setVisible(true); cancelBtn.setManaged(true);
                        editBtn.setVisible(false);  editBtn.setManaged(false);
                    });

                    cancelBtn.setOnAction(e -> {
                        editArea.setText(r.getContenu());
                        editArea.setVisible(false);  editArea.setManaged(false);
                        saveBtn.setVisible(false);   saveBtn.setManaged(false);
                        cancelBtn.setVisible(false); cancelBtn.setManaged(false);
                        editBtn.setVisible(true);    editBtn.setManaged(true);
                    });

                    saveBtn.setOnAction(e -> {
                        String newContenu = editArea.getText().trim();
                        if (newContenu.isEmpty()) {
                            editSuccess.setText("❌ Réponse vide.");
                            return;
                        }
                        reponseService.updateReponse(r.getId(), Session.getUserId(), newContenu);
                        editSuccess.setText("✅ Modifiée !");
                        editArea.setVisible(false);  editArea.setManaged(false);
                        saveBtn.setVisible(false);   saveBtn.setManaged(false);
                        cancelBtn.setVisible(false); cancelBtn.setManaged(false);
                        editBtn.setVisible(true);    editBtn.setManaged(true);
                        chargerReponses("", null);
                    });

                    HBox editRow = new HBox(8, editBtn, saveBtn, cancelBtn);
                    reponseBlock.getChildren().addAll(editRow, editArea, editSuccess);
                }

                card.getChildren().add(reponseBlock);
            }
        }

        // Patient actions
        if (Session.isPatient()) {
            try {
                if (rep.getQuestionUserId() == Session.getUserId()) {
                    Button deleteBtn = new Button("🗑️ Supprimer ma question");
                    deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;" +
                            "-fx-font-size: 11px; -fx-background-radius: 6;" +
                            "-fx-pref-height: 30;");
                    deleteBtn.setOnAction(e -> {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Confirmation");
                        confirm.setHeaderText("Supprimer cette question ?");
                        confirm.setContentText("Cette action est irréversible.");
                        confirm.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.OK) {
                                new QuestionService().deleteQuestion(
                                        rep.getQuestionId(), Session.getUserId());
                                chargerReponses("", null);
                            }
                        });
                    });
                    card.getChildren().add(deleteBtn);
                }
            } catch (Exception e) {
                System.err.println("❌ Delete button error: " + e.getMessage());
            }

            // Report button
            Button reportBtn = new Button("🚩 Signaler");
            reportBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c;" +
                    "-fx-font-size: 10px; -fx-cursor: hand;");
            reportBtn.setOnAction(e -> {
                Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
                dialog.setTitle("Signaler cette question");
                dialog.setHeaderText("Pourquoi signalez-vous cette question ?");
                TextField raisonField = new TextField();
                raisonField.setPromptText("Ex: contenu inapproprié, insultes...");
                dialog.getDialogPane().setContent(raisonField);
                dialog.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.OK && !raisonField.getText().trim().isEmpty()) {
                        new ModerationService().reportQuestion(
                                rep.getQuestionId(),
                                Session.getUserId(),
                                raisonField.getText().trim());
                        Alert ok = new Alert(Alert.AlertType.INFORMATION);
                        ok.setTitle("Signalement envoyé");
                        ok.setHeaderText(null);
                        ok.setContentText("✅ Merci, le signalement a été transmis à l'admin.");
                        ok.showAndWait();
                    }
                });
            });
            card.getChildren().add(reportBtn);
        }

        // Doctor add answer
        if (Session.isDoctor()) {
            card.getChildren().add(new Separator());

            VBox addAnswerBox = new VBox(8);
            addAnswerBox.setVisible(false);
            addAnswerBox.setManaged(false);

            Button addBtn = new Button("➕ Ajouter une réponse");
            addBtn.setStyle("-fx-background-color: #0a5f7a; -fx-text-fill: white;" +
                    "-fx-font-weight: bold; -fx-background-radius: 8;" +
                    "-fx-pref-height: 32; -fx-font-size: 11px;");

            TextArea newAnswerArea = new TextArea();
            newAnswerArea.setPromptText("Votre réponse complémentaire...");
            newAnswerArea.setStyle("-fx-pref-height: 80; -fx-font-size: 12px;" +
                    "-fx-background-radius: 8; -fx-border-color: #c8eaed;" +
                    "-fx-border-radius: 8;");

            Button submitNewBtn = new Button("✅ Soumettre");
            submitNewBtn.setStyle("-fx-background-color: #1aaa8a; -fx-text-fill: white;" +
                    "-fx-font-weight: bold; -fx-background-radius: 8;" +
                    "-fx-pref-height: 32; -fx-font-size: 11px;");

            Button cancelNewBtn = new Button("❌ Annuler");
            cancelNewBtn.setStyle("-fx-background-color: #888; -fx-text-fill: white;" +
                    "-fx-background-radius: 8; -fx-pref-height: 32;" +
                    "-fx-font-size: 11px;");

            Label addSuccess = new Label();
            addSuccess.setStyle("-fx-text-fill: #1aaa8a; -fx-font-size: 11px;" +
                    "-fx-font-weight: bold;");

            HBox newBtnRow = new HBox(10, submitNewBtn, cancelNewBtn);
            addAnswerBox.getChildren().addAll(newAnswerArea, newBtnRow, addSuccess);

            addBtn.setOnAction(e -> {
                addAnswerBox.setVisible(true);  addAnswerBox.setManaged(true);
                addBtn.setVisible(false);       addBtn.setManaged(false);
            });

            cancelNewBtn.setOnAction(e -> {
                newAnswerArea.clear();
                addAnswerBox.setVisible(false); addAnswerBox.setManaged(false);
                addBtn.setVisible(true);        addBtn.setManaged(true);
            });

            submitNewBtn.setOnAction(e -> {
                String contenu = newAnswerArea.getText().trim();
                if (contenu.isEmpty()) {
                    addSuccess.setText("❌ Réponse vide.");
                    addSuccess.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");
                    return;
                }
                reponseService.addAnotherReponse(rep.getQuestionId(), Session.getUserId(), contenu);
                addSuccess.setText("✅ Réponse ajoutée !");
                newAnswerArea.clear();
                addAnswerBox.setVisible(false); addAnswerBox.setManaged(false);
                addBtn.setVisible(true);        addBtn.setManaged(true);
                chargerReponses("", null);
            });

            card.getChildren().addAll(addBtn, addAnswerBox);
        }

        return card;
    }

    // ── NAVIGATION ───────────────────────────────────────────
    private void ouvrirReponse(Reponse rep) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/yessine/voirReponse.fxml"));
            Parent root = loader.load();
            VoirReponseController ctrl = loader.getController();
            ctrl.setReponse(rep);
            searchField.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur navigation: " + e.getMessage());
        }
    }

    @FXML
    public void rechercher(ActionEvent event) {
        chargerReponses(searchField.getText().trim(), null);
    }

    @FXML
    public void filtrerCategorie(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String text = clicked.getText().replace("  ", "").trim();

        VBox parent = (VBox) clicked.getParent();
        for (javafx.scene.Node node : parent.getChildren()) {
            if (node instanceof Button) {
                node.setStyle("-fx-background-color: transparent; -fx-text-fill: #444;" +
                        "-fx-font-size: 12px; -fx-cursor: hand;" +
                        "-fx-alignment: CENTER_LEFT; -fx-pref-width: 190;");
            }
        }
        clicked.setStyle("-fx-background-color: #e8f8e8; -fx-text-fill: #1aaa8a;" +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;" +
                "-fx-alignment: CENTER_LEFT; -fx-pref-width: 190;" +
                "-fx-background-radius: 8;");

        chargerReponses("", text.equals("Toutes les catégories") ? null : text);
    }

    @FXML
    public void allerPoserQuestion(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(
                    "/yessine/poserQuestion.fxml"));
            searchField.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur navigation: " + e.getMessage());
        }
    }

    @FXML
    public void seDeconnecter(ActionEvent event) {
        try {
            Session.clear();
            Parent root = FXMLLoader.load(getClass().getResource("/yessine/login.fxml"));
            searchField.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }
}
