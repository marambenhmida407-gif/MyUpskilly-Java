package controllers.yessine;

import utils.Session; // Session management will be handled by the main project
import models.yessine.Question;
import models.yessine.Reponse;
import services.yessine.AIService;
import services.yessine.ReponseService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import services.yessine.RecommandationService;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.util.List;


public class EspaceMedecinController {

    // ── FXML FIELDS ──────────────────────────────────────────
    @FXML private Label    welcomeLabel;
    @FXML private Label    statsLabel;
    @FXML private VBox     questionsContainer;
    @FXML private TextArea reponseField;
    @FXML private Label    reponseError;
    @FXML private Label    successLabel;
    @FXML private Label    selectedQuestionLabel;
    @FXML private VBox     selectedQuestionBox;
    @FXML private VBox     aiBox;
    @FXML private Label    aiLabel;
    @FXML private Label    aiLoading;
    @FXML private HBox     successBox;
    @FXML private StackPane innerContentPane;
    @FXML private HBox qaContentBox;

    private final ReponseService reponseService  = new ReponseService();
    private List<Question>       questionsEnAttente;
    private Question             selectedQuestion = null;
    // ── INITIALIZE ───────────────────────────────────────────
    @FXML
    public void initialize() {
        welcomeLabel.setText("👨‍⚕️ Dr " + Session.getPrenom() + " " + Session.getNom()
                + "  |  " + (Session.getSpecialite() != null
                ? Session.getSpecialite() : "Médecin"));
        chargerQuestions();
        chargerRecommandations();
        showRecommandationsPopup();
    }

    @FXML
    private void showQA() {
        innerContentPane.getChildren().setAll(qaContentBox);
    }
    // Maps question categories to doctor specialities
    private static final java.util.Map<String, String> CATEGORY_MAP =
            new java.util.HashMap<>() {{
                put("Cardiologie",                  "Cardiologue");
                put("Dermatologie",                 "Dermatologue");
                put("Gynécologie",                  "Gynécologue");
                put("Ophtalmologie",                "Ophtalmologue");
                put("Psychiatrie",                  "Psychiatre");
                put("Pédiatrie",                    "Pédiatre");
                put("O.R.L",                        "ORL");
                put("Sexologie",                    "Sexologue");
                put("Urologie",                     "Urologue");
                put("Orthopédie - Traumatologie",   "Orthopédiste");
                put("Endocrinologie - Diabétologie","Endocrinologue");
                put("Médecine dentaire",            "Dentiste");
                put("Gastro-entérologue",           "Gastro-entérologue");
                put("Carcinologie",                 "Cardiologue");
                put("Médecine générale",            "Médecin Généraliste");
            }};

    // Check if this question matches the doctor's speciality
    private boolean isMySpecialite(Question q) {
        String doctorSpec = Session.getSpecialite();
        if (doctorSpec == null) return true;
        if (q.getCategorie() == null) return true;

        // ✅ Fix 1 : correspondance directe catégorie ↔ spécialité
        // Ex: doctorSpec = "Médecine générale", categorie = "Médecine générale" → match direct
        if (doctorSpec.equalsIgnoreCase(q.getCategorie())) return true;

        // ✅ Fix 2 : correspondance via CATEGORY_MAP
        String requiredSpec = CATEGORY_MAP.get(q.getCategorie());
        if (requiredSpec == null) return true;

        return doctorSpec.equalsIgnoreCase(requiredSpec);
    }

    // ── LOAD UNANSWERED QUESTIONS ────────────────────────────
    private void chargerQuestions() {
        questionsEnAttente = reponseService.getEnAttente();
        questionsContainer.getChildren().clear();

        // Split into mine vs others
        List<Question> mesQuestions   = new ArrayList<>();
        List<Question> autresQuestions = new ArrayList<>();

        for (Question q : questionsEnAttente) {
            if (isMySpecialite(q)) mesQuestions.add(q);
            else autresQuestions.add(q);
        }

        statsLabel.setText(mesQuestions.size() + " question(s) dans votre spécialité");

        if (mesQuestions.isEmpty() && autresQuestions.isEmpty()) {
            Label empty = new Label("✅ Aucune question en attente !");
            empty.setStyle("-fx-text-fill: #4dd9ac; -fx-font-size: 14px; " +
                    "-fx-font-weight: bold;");
            questionsContainer.getChildren().add(empty);
            return;
        }

        // MY SPECIALITY section
        if (!mesQuestions.isEmpty()) {
            Label sectionTitle = new Label("✅ Questions dans votre spécialité");
            sectionTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; " +
                    "-fx-text-fill: #0a3d5c; -fx-padding: 4 0 4 0;");
            questionsContainer.getChildren().add(sectionTitle);

            for (Question q : mesQuestions) {
                questionsContainer.getChildren().add(buildQuestionCard(q, true));
            }
        }

        // OTHER SPECIALITIES section
        if (!autresQuestions.isEmpty()) {
            Label sectionTitle2 = new Label("⚠️ Hors de votre spécialité — Recommander un médecin");
            sectionTitle2.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; " +
                    "-fx-text-fill: #e67e22; -fx-padding: 12 0 4 0;");
            questionsContainer.getChildren().add(sectionTitle2);

            for (Question q : autresQuestions) {
                questionsContainer.getChildren().add(buildQuestionCard(q, false));
            }
        }
    }

    private VBox buildQuestionCard(Question q, boolean canAnswer) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-padding: 20; " +
                "-fx-background-radius: 16; " +
                "-fx-effect: dropshadow(gaussian, #00000018, 12, 0, 0, 4); " +
                "-fx-border-color: " + (canAnswer ? "#d0e8f5" : "#f5d0a9") + "; " +
                "-fx-border-radius: 16; -fx-border-width: 1.5;");

        // Category tag
        if (q.getCategorie() != null) {
            Label cat = new Label("📂  " + q.getCategorie());
            cat.setStyle("-fx-background-color: " + (canAnswer ? "#eaf6ff" : "#fff3e0") + "; " +
                    "-fx-text-fill: " + (canAnswer ? "#1a6e8f" : "#e67e22") + "; " +
                    "-fx-font-size: 10px; -fx-font-weight: bold; " +
                    "-fx-padding: 4 10 4 10; -fx-background-radius: 10;");
            card.getChildren().add(cat);
        }

        // Title
        Label titre = new Label(q.getTitre());
        titre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0a3d5c;");
        titre.setWrapText(true);

        // Description truncated
        String desc = q.getDescription();
        if (desc != null && desc.length() > 150) desc = desc.substring(0, 150) + "...";
        Label description = new Label(desc);
        description.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");
        description.setWrapText(true);

        // Medical info row
        HBox infoRow = new HBox(16);
        infoRow.setStyle("-fx-background-color: #f4f7fb; -fx-padding: 10; " +
                "-fx-background-radius: 8;");
        Label traitement = new Label("💊  Traitement: " + (q.isSousTraitement() ? "✅ Oui" : "❌ Non"));
        traitement.setStyle("-fx-font-size: 11px; -fx-text-fill: #444;");
        Label allergies = new Label("⚠️  Allergies: " + (q.isaDesAllergies() ? "✅ Oui" : "❌ Non"));
        allergies.setStyle("-fx-font-size: 11px; -fx-text-fill: #444;");
        String tailleStr = q.getTaille() > 0 ? q.getTaille() + " cm" : "N/A";
        String poidsStr  = q.getPoids()  > 0 ? q.getPoids()  + " kg" : "N/A";
        Label mesures = new Label("📏  " + tailleStr + "  |  ⚖️  " + poidsStr);
        mesures.setStyle("-fx-font-size: 11px; -fx-text-fill: #444;");
        infoRow.getChildren().addAll(traitement, allergies, mesures);

        // Image preview
        if (q.getFichierChemin() != null && !q.getFichierChemin().isEmpty()) {
            File file = new File(q.getFichierChemin());
            if (file.exists() && !q.getFichierChemin().toLowerCase().endsWith(".pdf")) {
                try {
                    Image image = new Image(file.toURI().toString());
                    ImageView iv = new ImageView(image);
                    iv.setFitWidth(280);
                    iv.setPreserveRatio(true);
                    Label imgLbl = new Label("📎  Image jointe:");
                    imgLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1a6e8f;");
                    card.getChildren().addAll(imgLbl, iv);
                } catch (Exception ignored) {}
            }
        }

        card.getChildren().addAll(titre, description, infoRow);

        Separator sep = new Separator();
        card.getChildren().add(sep);

        if (canAnswer) {
            // ── Buttons row ──
            HBox actionsRow = new HBox(10);
            actionsRow.setStyle("-fx-alignment: CENTER_LEFT;");

            Button replyBtn = new Button("✍️  Répondre à cette question");
            replyBtn.setStyle("-fx-background-color: #0a3d5c; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-background-radius: 10; " +
                    "-fx-pref-height: 36; -fx-font-size: 12px; " +
                    "-fx-padding: 0 18 0 18;");
            replyBtn.setOnAction(e -> {
                resetCardStyles();
                card.setStyle("-fx-background-color: #eaf6ff; -fx-padding: 20; " +
                        "-fx-background-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, #4dd9ac66, 12, 0, 0, 4); " +
                        "-fx-border-color: #4dd9ac; -fx-border-radius: 16; " +
                        "-fx-border-width: 2;");
                selectQuestion(q);
            });

            // ✅ Report button
            Button reportBtn = new Button("🚩 Signaler");
            reportBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; " +
                    "-fx-font-size: 11px; -fx-cursor: hand; " +
                    "-fx-border-color: #e74c3c; -fx-border-radius: 8; " +
                    "-fx-border-width: 1; -fx-pref-height: 36; " +
                    "-fx-padding: 0 12 0 12;");
            reportBtn.setOnAction(e -> showQuestionReportDialog(q));

            actionsRow.getChildren().addAll(replyBtn, reportBtn);
            card.getChildren().add(actionsRow);

        } else {
            String requiredSpec = CATEGORY_MAP.getOrDefault(
                    q.getCategorie(), "Médecin Généraliste");

            Label specLabel = new Label("🔍 Spécialité requise : " + requiredSpec);
            specLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #e67e22;" +
                    "-fx-font-weight: bold;");

            HBox actionsRow = new HBox(10);
            actionsRow.setStyle("-fx-alignment: CENTER_LEFT;");

            Button recommendBtn = new Button("👨‍⚕️  Recommander un médecin");
            recommendBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-background-radius: 10; " +
                    "-fx-pref-height: 36; -fx-font-size: 12px; " +
                    "-fx-padding: 0 18 0 18;");
            recommendBtn.setOnAction(e -> showRecommandationPopup(q, requiredSpec));

            // ✅ Report button
            Button reportBtn = new Button("🚩 Signaler");
            reportBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; " +
                    "-fx-font-size: 11px; -fx-cursor: hand; " +
                    "-fx-border-color: #e74c3c; -fx-border-radius: 8; " +
                    "-fx-border-width: 1; -fx-pref-height: 36; " +
                    "-fx-padding: 0 12 0 12;");
            reportBtn.setOnAction(e -> showQuestionReportDialog(q));

            actionsRow.getChildren().addAll(recommendBtn, reportBtn);
            card.getChildren().addAll(specLabel, actionsRow);
        }

        return card;
    }

    private void showQuestionReportDialog(Question q) {
        System.out.println("=== showQuestionReportDialog called === qId: " + q.getId());

        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.setTitle("🚩 Signaler cette question");
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.initOwner(welcomeLabel.getScene().getWindow());
        popup.setResizable(false);

        // Header
        Label header = new Label("🚩 Signaler cette question");
        header.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0a3d5c;");

        Label questionTitle = new Label("❓ " + q.getTitre());
        questionTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
        questionTitle.setWrapText(true);

        Separator sep = new Separator();

        Label chooseLabel = new Label("Raison du signalement :");
        chooseLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0a3d5c; -fx-font-size: 12px;");

        ComboBox<String> raisonCombo = new ComboBox<>();
        raisonCombo.getItems().addAll(
                "Contenu inapproprié",
                "Insultes ou langage offensant",
                "Question hors sujet médical",
                "Spam ou publicité",
                "Fausse information",
                "Autre"
        );
        raisonCombo.setPromptText("Choisir une raison...");
        raisonCombo.setPrefWidth(340);

        TextField autreField = new TextField();
        autreField.setPromptText("Précisez la raison...");
        autreField.setVisible(false);
        autreField.setManaged(false);

        raisonCombo.setOnAction(ev -> {
            boolean isAutre = "Autre".equals(raisonCombo.getValue());
            autreField.setVisible(isAutre);
            autreField.setManaged(isAutre);
        });

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");

        // Buttons
        Button sendBtn = new Button("✅ Envoyer le signalement");
        sendBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;" +
                "-fx-font-weight: bold; -fx-pref-height: 38;" +
                "-fx-pref-width: 220; -fx-background-radius: 10;" +
                "-fx-font-size: 12px;");
        sendBtn.setOnAction(e -> {
            if (raisonCombo.getValue() == null) {
                errorLabel.setText("Veuillez choisir une raison.");
                return;
            }
            String raison = "Autre".equals(raisonCombo.getValue())
                    ? autreField.getText().trim()
                    : raisonCombo.getValue();
            if (raison.isEmpty()) raison = "Contenu inapproprié";

            new services.yessine.ModerationService()
                    .reportQuestion(q.getId(), 1, raison); // Session management will be handled by the main project

            popup.close();

            // Confirmation
            javafx.stage.Stage confirm = new javafx.stage.Stage();
            confirm.setTitle("Signalement envoyé");
            confirm.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            confirm.initOwner(welcomeLabel.getScene().getWindow());
            Label msg = new Label("✅ Merci, le signalement a été transmis à l'administrateur.");
            msg.setStyle("-fx-font-size: 13px; -fx-text-fill: #0a3d5c; -fx-wrap-text: true;");
            msg.setWrapText(true);
            Button okBtn = new Button("OK");
            okBtn.setStyle("-fx-background-color: #0a3d5c; -fx-text-fill: white;" +
                    "-fx-font-weight: bold; -fx-pref-width: 100; -fx-background-radius: 8;");
            okBtn.setOnAction(ev -> confirm.close());
            VBox confirmLayout = new VBox(14, msg, okBtn);
            confirmLayout.setStyle("-fx-padding: 24; -fx-alignment: CENTER;");
            confirm.setScene(new javafx.scene.Scene(confirmLayout, 360, 140));
            confirm.show();
        });

        Button cancelBtn = new Button("❌ Annuler");
        cancelBtn.setStyle("-fx-background-color: #888; -fx-text-fill: white;" +
                "-fx-font-weight: bold; -fx-pref-height: 38;" +
                "-fx-pref-width: 120; -fx-background-radius: 10;" +
                "-fx-font-size: 12px;");
        cancelBtn.setOnAction(e -> popup.close());

        HBox btnRow = new HBox(12, cancelBtn, sendBtn);
        btnRow.setStyle("-fx-alignment: CENTER_RIGHT; -fx-padding: 10 0 0 0;");

        VBox layout = new VBox(14,
                header, questionTitle, sep,
                chooseLabel, raisonCombo, autreField,
                errorLabel, btnRow);
        layout.setStyle("-fx-padding: 24; -fx-background-color: white;");

        popup.setScene(new javafx.scene.Scene(layout, 420, 300));
        popup.show();
    }


    // ── RESET CARD STYLES ────────────────────────────────────
    private void resetCardStyles() {
        questionsContainer.getChildren().forEach(node ->
                node.setStyle("-fx-background-color: white; -fx-padding: 20; " +
                        "-fx-background-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, #00000018, 12, 0, 0, 4); " +
                        "-fx-border-color: #d0e8f5; -fx-border-radius: 16; " +
                        "-fx-border-width: 1.5;")
        );
    }

    // ── SELECT QUESTION + TRIGGER AI ─────────────────────────
    private void selectQuestion(Question q) {
        selectedQuestion = q;

        // Show selected question box
        selectedQuestionLabel.setText(q.getTitre());
        selectedQuestionBox.setVisible(true);
        selectedQuestionBox.setManaged(true);

        // Reset form
        reponseField.clear();
        reponseError.setText("");
        successLabel.setText("");

        // Hide success box
        if (successBox != null) {
            successBox.setVisible(false);
            successBox.setManaged(false);
        }

        // Remove old "voir tout" button if exists
        aiBox.getChildren().removeIf(n -> "voirToutBtn".equals(n.getId()));

        // Show AI box with loading
        aiBox.setVisible(true);
        aiBox.setManaged(true);
        aiLoading.setText("⏳ Analyse IA en cours...");
        aiLabel.setText("");

        // Run AI in background — never block JavaFX UI thread
        new Thread(() -> {
            AIService ai = new AIService();

            // Step 1: Analyze image if present
            String imageAnalysis = null;
            if (q.getFichierChemin() != null && !q.getFichierChemin().isEmpty()) {
                imageAnalysis = ai.analyzeImage(q.getFichierChemin());
            }

            // Step 2: Get doctor suggestions
            final String imgResult    = imageAnalysis;
            String suggestions = ai.getDoctorAssistance(
                    q.getTitre(),
                    q.getDescription(),
                    imgResult,
                    q.getCategorie()
            );

            final String finalImg         = imageAnalysis;
            final String finalSuggestions = suggestions;

            // Step 3: Update UI on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                aiLoading.setText("");

                // Build full text
                StringBuilder display = new StringBuilder();
                if (finalImg != null && !finalImg.isEmpty()) {
                    display.append("🖼️ Analyse image patient :\n")
                            .append(finalImg)
                            .append("\n\n");
                }
                if (finalSuggestions != null && !finalSuggestions.isEmpty()) {
                    display.append("🤖 Suggestions pour le médecin :\n")
                            .append(finalSuggestions);
                } else {
                    display.append("Suggestions IA indisponibles.");
                }

                String fullText = display.toString();

                // Show short preview in panel
                String preview = fullText.length() > 120
                        ? fullText.substring(0, 120) + "..."
                        : fullText;
                aiLabel.setText(preview);

                // Add "Voir tout" button
                Button voirTout = new Button("🔍 Voir l'analyse complète");
                voirTout.setId("voirToutBtn");
                voirTout.setStyle(
                        "-fx-background-color: #0a3d5c; -fx-text-fill: white; " +
                                "-fx-font-weight: bold; -fx-background-radius: 8; " +
                                "-fx-pref-height: 32; -fx-font-size: 11px; " +
                                "-fx-cursor: hand; -fx-padding: 0 14 0 14;");
                voirTout.setOnAction(ev -> showAIPopup(fullText));
                aiBox.getChildren().add(voirTout);
            });

        }).start();
    }

    // ── AI POPUP WINDOW ──────────────────────────────────────
    private void showAIPopup(String content) {
        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.setTitle("🤖 Analyse IA Complète");
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.setResizable(true);

        // Header
        Label header = new Label("🤖 Suggestions IA pour le médecin");
        header.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; " +
                "-fx-text-fill: #0a3d5c;");

        // Disclaimer
        Label disclaimer = new Label(
                "⚠️ Ces suggestions sont indicatives. Vous restez seul décisionnaire."
        );
        disclaimer.setStyle("-fx-font-size: 11px; -fx-text-fill: #e67e22; " +
                "-fx-font-style: italic;");
        disclaimer.setWrapText(true);

        // Full text scrollable read-only area
        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setStyle("-fx-font-size: 13px; -fx-pref-height: 380; " +
                "-fx-background-radius: 10; -fx-border-color: #d0e8f5; " +
                "-fx-border-radius: 10; -fx-border-width: 1.5; " +
                "-fx-control-inner-background: #f4f7fb;");

        // Copy button
        Button copyBtn = new Button("📋 Copier");
        copyBtn.setStyle("-fx-background-color: #1a6e8f; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-pref-height: 38; " +
                "-fx-pref-width: 140; -fx-background-radius: 10; " +
                "-fx-font-size: 13px;");
        copyBtn.setOnAction(e -> {
            javafx.scene.input.ClipboardContent cc =
                    new javafx.scene.input.ClipboardContent();
            cc.putString(content);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
            copyBtn.setText("✅ Copié !");
        });

        // Close button
        Button closeBtn = new Button("✅ Fermer");
        closeBtn.setStyle("-fx-background-color: #0a3d5c; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-pref-height: 38; " +
                "-fx-pref-width: 160; -fx-background-radius: 10; " +
                "-fx-font-size: 13px;");
        closeBtn.setOnAction(e -> popup.close());

        // Button row
        HBox btnRow = new HBox(12, copyBtn, closeBtn);
        btnRow.setStyle("-fx-alignment: CENTER_RIGHT; -fx-padding: 10 0 0 0;");

        // Layout
        VBox layout = new VBox(14, header, disclaimer, textArea, btnRow);
        layout.setStyle("-fx-padding: 24; -fx-background-color: white;");

        javafx.scene.Scene scene = new javafx.scene.Scene(layout, 660, 530);
        popup.setScene(scene);
        popup.show();
    }

    // ── SUBMIT ANSWER ────────────────────────────────────────
    @FXML
    public void soumettreReponse(ActionEvent event) {
        reponseError.setText("");

        if (selectedQuestion == null) {
            reponseError.setText("Veuillez sélectionner une question.");
            return;
        }

        String contenu = reponseField.getText().trim();
        if (contenu.isEmpty()) {
            reponseError.setText("La réponse ne peut pas être vide.");
            return;
        }
        if (contenu.length() < 10) {
            reponseError.setText("Réponse trop courte (min 10 caractères).");
            return;
        }

        // Insert answer
        Reponse r = new Reponse();
        r.setQuestionId(selectedQuestion.getId());
        r.setDoctorId(Session.getUserId());
        r.setContenu(contenu);
        reponseService.insert(r);

        // Show success
        if (successBox != null) {
            successBox.setVisible(true);
            successBox.setManaged(true);
        }
        successLabel.setText("Réponse soumise avec succès !");

        // Reset form
        reponseField.clear();
        selectedQuestion = null;
        selectedQuestionLabel.setText("");

        if (selectedQuestionBox != null) {
            selectedQuestionBox.setVisible(false);
            selectedQuestionBox.setManaged(false);
        }

        aiBox.setVisible(false);
        aiBox.setManaged(false);
        aiBox.getChildren().removeIf(n -> "voirToutBtn".equals(n.getId()));

        chargerQuestions();
        chargerRecommandations();
    }

    // ── NAVIGATION ───────────────────────────────────────────
    @FXML
    public void voirQuestionsRepondues(ActionEvent event) {
        // Navigate via MainLayout sidebar (avoids replacing the full scene)
        if (controllers.MainLayoutController.instance != null) {
            controllers.MainLayoutController.instance.loadPage(
                    "/yessine/questionsRepondues.fxml", "Questions Répondues", null);
        } else {
            // Fallback: standalone mode (should not happen in normal app flow)
            try {
                Parent root = FXMLLoader.load(getClass().getResource(
                        "/yessine/questionsRepondues.fxml"));
                welcomeLabel.getScene().setRoot(root);
            } catch (IOException e) {
                System.err.println("❌ Navigation erreur: " + e.getMessage());
            }
        }
    }

    @FXML
    public void seDeconnecter(ActionEvent event) {
        // Logout is handled by MainLayout topbar button — this method kept for FXML compat
        utils.Session.clear();
        controllers.MainLayoutController.instance = null;
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/yessine/login.fxml"));
            welcomeLabel.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("❌ Déconnexion erreur: " + e.getMessage());
        }
    }
    private void showRecommandationPopup(Question q, String requiredSpec) {
        RecommandationService rs = new RecommandationService();

        // Check if already recommended
        if (rs.dejaRecommande(q.getId(), Session.getUserId())) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Déjà recommandé");
            info.setHeaderText(null);
            info.setContentText("✅ Vous avez déjà recommandé un médecin pour cette question.");
            info.showAndWait();
            return;
        }

        // Get doctors of required speciality first
        List<String[]> doctors = rs.getDoctorsBySpecialite(requiredSpec, Session.getUserId());



        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.setTitle("👨‍⚕️ Recommander un médecin");
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.setResizable(false);

        // Header
        Label header = new Label("👨‍⚕️ Recommander un médecin");
        header.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0a3d5c;");

        Label subHeader = new Label("Question : " + q.getTitre());
        subHeader.setStyle("-fx-font-size: 11px; -fx-text-fill: #555; -fx-wrap-text: true;");
        subHeader.setWrapText(true);

        Label specInfo = new Label("🔍 Spécialité recommandée : " + requiredSpec);
        specInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #e67e22; -fx-font-weight: bold;");

        Separator sep = new Separator();

        // Doctor list
        Label chooseLabel = new Label("Choisir un médecin :");
        chooseLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0a3d5c; -fx-font-size: 12px;");

        // Radio buttons for doctor selection
        ToggleGroup group = new ToggleGroup();
        VBox doctorList = new VBox(8);

        final List<String[]> finalDoctors = doctors;
        // ✅ Separate matching vs non-matching doctors
        List<String[]> matching    = new ArrayList<>();
        List<String[]> nonMatching = new ArrayList<>();

        for (String[] doc : finalDoctors) {
            if (doc[3] != null && doc[3].equalsIgnoreCase(requiredSpec))
                matching.add(doc);
            else
                nonMatching.add(doc);
        }

// ✅ Show matching doctors first with a header
        if (!matching.isEmpty()) {
            Label matchHeader = new Label(
                    "⭐ Médecins en " + requiredSpec + " (" + matching.size() + ")");
            matchHeader.setStyle(
                    "-fx-font-size: 11px; -fx-font-weight: bold;" +
                            "-fx-text-fill: #1a6e8f; -fx-padding: 4 0 4 0;");
            doctorList.getChildren().add(matchHeader);

            for (String[] doc : matching) {
                RadioButton rb = new RadioButton(
                        "⭐  Dr " + doc[2] + " " + doc[1] + "  —  " + doc[3]);
                rb.setToggleGroup(group);
                rb.setUserData(doc[0]);
                rb.setStyle(
                        "-fx-font-size: 12px; -fx-text-fill: #0a3d5c;" +
                                "-fx-font-weight: bold;");
                // ✅ Auto-select first matching doctor
                if (matching.indexOf(doc) == 0) rb.setSelected(true);
                doctorList.getChildren().add(rb);
            }
        }

// ✅ Show other doctors below with a separator
        if (!nonMatching.isEmpty()) {
            if (!matching.isEmpty()) {
                Label otherHeader = new Label(
                        "👨‍⚕️ Autres médecins (" + nonMatching.size() + ")");
                otherHeader.setStyle(
                        "-fx-font-size: 11px; -fx-font-weight: bold;" +
                                "-fx-text-fill: #888; -fx-padding: 8 0 4 0;");
                doctorList.getChildren().add(new Separator());
                doctorList.getChildren().add(otherHeader);
            }

            for (String[] doc : nonMatching) {
                RadioButton rb = new RadioButton(
                        "Dr " + doc[2] + " " + doc[1] + "  —  " + doc[3]);
                rb.setToggleGroup(group);
                rb.setUserData(doc[0]);
                rb.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
                doctorList.getChildren().add(rb);
            }
        }

        if (finalDoctors.isEmpty()) {
            Label noDoc = new Label("Aucun médecin disponible.");
            noDoc.setStyle("-fx-text-fill: #888;");
            doctorList.getChildren().add(noDoc);
        }

        if (finalDoctors.isEmpty()) {
            Label noDoc = new Label("Aucun médecin disponible.");
            noDoc.setStyle("-fx-text-fill: #888;");
            doctorList.getChildren().add(noDoc);
        }

        // Optional message
        Label msgLabel = new Label("Message (optionnel) :");
        msgLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0a3d5c; -fx-font-size: 12px;");

        TextArea msgArea = new TextArea();
        msgArea.setPromptText("Ex: Ce patient nécessite un avis spécialisé en " + requiredSpec);
        msgArea.setWrapText(true);
        msgArea.setPrefHeight(80);
        msgArea.setStyle("-fx-font-size: 12px; -fx-background-radius: 8; " +
                "-fx-border-color: #d0e8f5; -fx-border-radius: 8;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");

        // Buttons
        Button sendBtn = new Button("✅ Envoyer la recommandation");
        sendBtn.setStyle("-fx-background-color: #0a3d5c; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-pref-height: 38; " +
                "-fx-pref-width: 240; -fx-background-radius: 10; " +
                "-fx-font-size: 12px;");
        sendBtn.setOnAction(e -> {
            if (group.getSelectedToggle() == null) {
                errorLabel.setText("Veuillez sélectionner un médecin.");
                return;
            }
            int toDoctorId = Integer.parseInt(
                    group.getSelectedToggle().getUserData().toString()
            );
            String message = msgArea.getText().trim();
            rs.recommander(q.getId(), Session.getUserId(), toDoctorId, message);
            popup.close();

            // Show confirmation
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Recommandation envoyée");
            ok.setHeaderText(null);
            ok.setContentText("✅ La recommandation a été envoyée avec succès !");
            ok.showAndWait();
        });

        Button cancelBtn = new Button("❌ Annuler");
        cancelBtn.setStyle("-fx-background-color: #888; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-pref-height: 38; " +
                "-fx-pref-width: 120; -fx-background-radius: 10; " +
                "-fx-font-size: 12px;");
        cancelBtn.setOnAction(e -> popup.close());

        HBox btnRow = new HBox(12, cancelBtn, sendBtn);
        btnRow.setStyle("-fx-alignment: CENTER_RIGHT; -fx-padding: 10 0 0 0;");

        // Scrollable doctor list
        ScrollPane scroll = new ScrollPane(doctorList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(180);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox layout = new VBox(12,
                header, subHeader, specInfo, sep,
                chooseLabel, scroll,
                msgLabel, msgArea,
                errorLabel, btnRow
        );
        layout.setStyle("-fx-padding: 24; -fx-background-color: white;");

        javafx.scene.Scene scene = new javafx.scene.Scene(layout, 520, 560);
        popup.setScene(scene);
        popup.show();
    }
    @FXML private VBox recommandationsBox;
    @FXML private VBox recommandationsList;
    // ── LOAD RECOMMENDATIONS ─────────────────────────────
    private void chargerRecommandations() {
        RecommandationService rs = new RecommandationService();
        List<String[]> recs = rs.getRecommandationsForDoctor(Session.getUserId());

        if (recs.isEmpty()) {
            recommandationsBox.setVisible(false);
            recommandationsBox.setManaged(false);
            return;
        }

        // Show the glowing box
        recommandationsBox.setVisible(true);
        recommandationsBox.setManaged(true);
        recommandationsList.getChildren().clear();

        for (String[] rec : recs) {
            // rec: id, qid, titre, desc, categorie, message, from_nom, from_prenom, date
            VBox recCard = new VBox(6);
            recCard.setStyle(
                    "-fx-background-color: white; -fx-padding: 12;" +
                            "-fx-background-radius: 10;" +
                            "-fx-border-color: #f0a500; -fx-border-radius: 10;" +
                            "-fx-border-width: 1;");

            // From doctor
            Label from = new Label("👨‍⚕️ Dr " + rec[7] + " " + rec[6]
                    + " vous recommande");
            from.setStyle("-fx-font-size: 11px; -fx-text-fill: #b07000;" +
                    "-fx-font-weight: bold;");
            from.setWrapText(true);

            // Question title
            Label qtitle = new Label("❓ " + rec[2]);
            qtitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;" +
                    "-fx-text-fill: #0a3d5c;");
            qtitle.setWrapText(true);

            // Category
            if (rec[4] != null) {
                Label cat = new Label("📂 " + rec[4]);
                cat.setStyle("-fx-font-size: 10px; -fx-text-fill: #1a6e8f;");
                recCard.getChildren().add(cat);
            }

            // Optional message
            if (rec[5] != null && !rec[5].isEmpty()) {
                Label msg = new Label("💬 " + rec[5]);
                msg.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;" +
                        "-fx-font-style: italic;");
                msg.setWrapText(true);
                recCard.getChildren().addAll(from, qtitle, msg);
            } else {
                recCard.getChildren().addAll(from, qtitle);
            }

            // Answer button — loads this question directly
            Button answerBtn = new Button("✍️ Répondre maintenant");
            answerBtn.setStyle(
                    "-fx-background-color: #f0a500; -fx-text-fill: white;" +
                            "-fx-font-weight: bold; -fx-background-radius: 8;" +
                            "-fx-pref-height: 32; -fx-font-size: 11px;");

            // Build a Question object from recommendation data
            final String qid   = rec[1];
            final String titre = rec[2];
            final String desc  = rec[3];
            final String cat   = rec[4];

            answerBtn.setOnAction(e -> {
                // Build minimal Question to select it
                Question q = new Question();
                q.setId(Integer.parseInt(qid));
                q.setTitre(titre);
                q.setDescription(desc);
                q.setCategorie(cat);
                selectQuestion(q);

                // Scroll to reply form
                selectedQuestionBox.setVisible(true);
                selectedQuestionBox.setManaged(true);
            });

            recCard.getChildren().add(answerBtn);
            recommandationsList.getChildren().add(recCard);
        }
    }
    // ── RECOMMENDATIONS POPUP ────────────────────────────
    private void showRecommandationsPopup() {
        RecommandationService rs = new RecommandationService();
        List<String[]> recs = rs.getRecommandationsForDoctor(Session.getUserId());

        if (recs.isEmpty()) return; // no popup if nothing

        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.setTitle("🔔 Recommandations reçues");
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.setResizable(true);

        // ── HEADER ───────────────────────────────────────
        VBox header = new VBox(4);
        header.setStyle(
                "-fx-background-color: linear-gradient(to right, #f0a500, #e67e22);" +
                        "-fx-padding: 20 24 20 24;");

        HBox headerRow = new HBox(12);
        headerRow.setStyle("-fx-alignment: CENTER_LEFT;");
        Label bell = new Label("🔔");
        bell.setStyle("-fx-font-size: 30px;");
        VBox headerText = new VBox(3);
        Label headerTitle = new Label("Recommandations reçues");
        headerTitle.setStyle(
                "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label headerSub = new Label(
                recs.size() + " question(s) vous ont été recommandée(s)");
        headerSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #fff3cd;");
        headerText.getChildren().addAll(headerTitle, headerSub);
        headerRow.getChildren().addAll(bell, headerText);
        header.getChildren().add(headerRow);

        // ── CONTENT ──────────────────────────────────────
        VBox content = new VBox(12);
        content.setStyle("-fx-padding: 20;");

        for (String[] rec : recs) {
            // rec: id, qid, titre, desc, categorie, message, from_nom, from_prenom, date
            VBox card = new VBox(10);
            card.setStyle(
                    "-fx-background-color: #fff8e8; -fx-padding: 16;" +
                            "-fx-background-radius: 14;" +
                            "-fx-border-color: #f0a500; -fx-border-radius: 14;" +
                            "-fx-border-width: 1.5;" +
                            "-fx-effect: dropshadow(gaussian, #f0a50044, 8, 0, 0, 2);");

            // From doctor
            HBox fromRow = new HBox(8);
            fromRow.setStyle("-fx-alignment: CENTER_LEFT;");
            Label fromIcon = new Label("👨‍⚕️");
            fromIcon.setStyle("-fx-font-size: 20px;");
            VBox fromInfo = new VBox(2);
            Label fromName = new Label(
                    "Dr " + rec[7] + " " + rec[6] + " vous recommande cette question");
            fromName.setStyle(
                    "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #b07000;");
            Label fromDate = new Label("📅 " +
                    (rec[8] != null && rec[8].length() >= 10
                            ? rec[8].substring(0, 10) : ""));
            fromDate.setStyle("-fx-font-size: 10px; -fx-text-fill: #aaa;");
            fromInfo.getChildren().addAll(fromName, fromDate);
            fromRow.getChildren().addAll(fromIcon, fromInfo);

            // Category pill
            HBox catRow = new HBox(8);
            catRow.setStyle("-fx-alignment: CENTER_LEFT;");
            if (rec[4] != null) {
                Label cat = new Label("📂  " + rec[4]);
                cat.setStyle(
                        "-fx-background-color: #fff3cd; -fx-text-fill: #b07000;" +
                                "-fx-font-size: 10px; -fx-font-weight: bold;" +
                                "-fx-padding: 3 10 3 10; -fx-background-radius: 10;");
                catRow.getChildren().add(cat);
            }

            // Question title
            Label qtitle = new Label("❓  " + rec[2]);
            qtitle.setStyle(
                    "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0a3d5c;");
            qtitle.setWrapText(true);

            // Question description preview
            String descPreview = rec[3] != null && rec[3].length() > 120
                    ? rec[3].substring(0, 120) + "..." : rec[3];
            Label qdesc = new Label(descPreview);
            qdesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
            qdesc.setWrapText(true);

            // Optional message from recommending doctor
            if (rec[5] != null && !rec[5].isEmpty()) {
                VBox msgBox = new VBox(4);
                msgBox.setStyle(
                        "-fx-background-color: #ffffff; -fx-padding: 10;" +
                                "-fx-background-radius: 8;" +
                                "-fx-border-color: #ffd700; -fx-border-radius: 8;" +
                                "-fx-border-width: 1;");
                Label msgTitle = new Label("💬 Message du médecin :");
                msgTitle.setStyle(
                        "-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #b07000;");
                Label msgContent = new Label(rec[5]);
                msgContent.setStyle(
                        "-fx-font-size: 12px; -fx-text-fill: #444; -fx-font-style: italic;");
                msgContent.setWrapText(true);
                msgBox.getChildren().addAll(msgTitle, msgContent);
                card.getChildren().addAll(fromRow, catRow, qtitle, qdesc, msgBox);
            } else {
                card.getChildren().addAll(fromRow, catRow, qtitle, qdesc);
            }

            // Answer button
            Button answerBtn = new Button("✍️  Répondre à cette question");
            answerBtn.setStyle(
                    "-fx-background-color: #0a3d5c; -fx-text-fill: white;" +
                            "-fx-font-weight: bold; -fx-pref-height: 40;" +
                            "-fx-pref-width: 240; -fx-background-radius: 10;" +
                            "-fx-font-size: 12px;" +
                            "-fx-effect: dropshadow(gaussian, #0a3d5c66, 6, 0, 0, 2);");

            final String qid  = rec[1];
            final String tit  = rec[2];
            final String des  = rec[3];
            final String cat  = rec[4];

            answerBtn.setOnAction(e -> {
                popup.close();
                // Select question in main view
                Question q = new Question();
                q.setId(Integer.parseInt(qid));
                q.setTitre(tit);
                q.setDescription(des);
                q.setCategorie(cat);
                selectQuestion(q);
                selectedQuestionBox.setVisible(true);
                selectedQuestionBox.setManaged(true);
            });

            card.getChildren().add(answerBtn);
            content.getChildren().add(card);
        }

        // ── SCROLL + CLOSE ───────────────────────────────
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background-color: white; -fx-background: white;");

        Button closeBtn = new Button("✅  Fermer");
        closeBtn.setStyle(
                "-fx-background-color: #888; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-pref-height: 38;" +
                        "-fx-pref-width: 140; -fx-background-radius: 10;" +
                        "-fx-font-size: 12px;");
        closeBtn.setOnAction(e -> popup.close());

        HBox footer = new HBox(closeBtn);
        footer.setStyle(
                "-fx-alignment: CENTER_RIGHT; -fx-padding: 12 20 12 20;" +
                        "-fx-background-color: #f9f9f9;" +
                        "-fx-border-color: #eee; -fx-border-width: 1 0 0 0;");

        VBox layout = new VBox(header, scroll, footer);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        javafx.scene.Scene scene = new javafx.scene.Scene(layout, 580, 600);
        popup.setScene(scene);
        popup.show();
    }

}