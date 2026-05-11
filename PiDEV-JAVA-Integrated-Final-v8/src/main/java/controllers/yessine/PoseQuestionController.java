package controllers.yessine;

import models.yessine.Question;
import services.yessine.AIService;
import services.yessine.ModerationService;
import services.yessine.QuestionService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import utils.Session;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PoseQuestionController {

    @FXML private TextField    titreField;
    @FXML private TextArea     descriptionField;
    @FXML private TextField    tailleField;
    @FXML private TextField    poidsField;

    @FXML private RadioButton  traitementOui;
    @FXML private RadioButton  traitementNon;
    @FXML private RadioButton  allergiesOui;
    @FXML private RadioButton  allergiesNon;

    @FXML private VBox         traitementDetails;
    @FXML private VBox         allergiesDetails;

    @FXML private ComboBox<String> categorieCombo;
    @FXML private Label        categorieError;

    @FXML private TextArea     descTraitementField;
    @FXML private TextArea     descAllergiesField;

    @FXML private Label        fichierLabel;
    @FXML private Label        titreError;
    @FXML private Label        descriptionError;
    @FXML private Label        tailleError;
    @FXML private Label        poidsError;
    @FXML private Label        traitementError;
    @FXML private Label        allergiesError;
    @FXML private Label        fichierError;
    @FXML private Label        successLabel;

    // Similar cases panel
    @FXML private VBox         similarBox;
    @FXML private VBox         similarContainer;
    @FXML private Label        similarLoading;

    // ✅ Image analysis panel
    @FXML private VBox         imageAnalysisBox;
    @FXML private Label        imageAnalysisLabel;

    // ✅ AI question analysis panel
    @FXML private VBox         aiAnalysisBox;
    @FXML private Label        aiAnalysisLoading;
    @FXML private Label        aiAnalysisLabel;

    private File selectedFile = null;
    private final ToggleGroup traitementGroup = new ToggleGroup();
    private final ToggleGroup allergiesGroup  = new ToggleGroup();

    @FXML
    public void initialize() {
        traitementOui.setToggleGroup(traitementGroup);
        traitementNon.setToggleGroup(traitementGroup);
        allergiesOui.setToggleGroup(allergiesGroup);
        allergiesNon.setToggleGroup(allergiesGroup);

        categorieCombo.setItems(FXCollections.observableArrayList(
                "Médecine générale", "Médecine dentaire", "Cardiologie",
                "Dermatologie", "Endocrinologie - Diabétologie", "Gynécologie",
                "Ophtalmologie", "O.R.L", "Orthopédie - Traumatologie",
                "Pédiatrie", "Psychiatrie", "Sexologie",
                "Médecine esthétique", "Gastro-entérologue", "Carcinologie"
        ));

        traitementGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean show = (newVal == traitementOui);
            traitementDetails.setVisible(show);
            traitementDetails.setManaged(show);
        });

        allergiesGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean show = (newVal == allergiesOui);
            allergiesDetails.setVisible(show);
            allergiesDetails.setManaged(show);
        });

        // ✅ Trigger similar cases + AI analysis when patient leaves description field
        descriptionField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused
                    && !titreField.getText().trim().isEmpty()
                    && !descriptionField.getText().trim().isEmpty()) {
                searchSimilarCases(
                        titreField.getText().trim() + " " +
                                descriptionField.getText().trim());
                analyzeQuestionWithAI();
            }
        });
    }

    // ── SIMILAR CASES SEARCH ─────────────────────────────────
    private void searchSimilarCases(String query) {
        if (similarBox == null) return;

        similarContainer.getChildren().clear();
        similarLoading.setText("🔍 Recherche de cas similaires...");
        similarBox.setVisible(true);
        similarBox.setManaged(true);

        new Thread(() -> {
            AIService ai = new AIService();
            List<String[]> similar = ai.findSimilarQuestions(query);

            Platform.runLater(() -> {
                similarLoading.setText("");

                // ✅ Hide box if nothing relevant found
                if (similar == null || similar.isEmpty()) {
                    similarBox.setVisible(false);
                    similarBox.setManaged(false);
                    return;
                }

                similarContainer.getChildren().clear();

                for (String[] q : similar) {
                    VBox caseCard = new VBox(6);
                    caseCard.setStyle(
                            "-fx-background-color: #f0f8ff; -fx-padding: 12;" +
                                    "-fx-background-radius: 10;" +
                                    "-fx-border-color: #1a7a9a; -fx-border-radius: 10;" +
                                    "-fx-border-width: 1;");

                    Label qTitle = new Label("❓ " + q[1]);
                    qTitle.setStyle(
                            "-fx-font-weight: bold; -fx-font-size: 12px;" +
                                    "-fx-text-fill: #0a3d5c;");
                    qTitle.setWrapText(true);

                    String answerPreview = q[3] != null && q[3].length() > 120
                            ? q[3].substring(0, 120) + "..." : q[3];
                    Label aContent = new Label("💬 " + answerPreview);
                    aContent.setStyle("-fx-font-size: 11px; -fx-text-fill: #444;");
                    aContent.setWrapText(true);

                    caseCard.getChildren().addAll(qTitle, aContent);
                    similarContainer.getChildren().add(caseCard);
                }
            });
        }).start();
    }

    // ── AI QUESTION ANALYSIS ─────────────────────────────────
    private void analyzeQuestionWithAI() {
        if (aiAnalysisBox == null) return;

        aiAnalysisBox.setVisible(true);
        aiAnalysisBox.setManaged(true);
        aiAnalysisLoading.setText("⏳ Analyse IA en cours...");
        aiAnalysisLabel.setText("");

        String titre       = titreField.getText().trim();
        String description = descriptionField.getText().trim();
        String categorie   = categorieCombo.getValue();

        new Thread(() -> {
            AIService ai = new AIService();
            String analysis = ai.analyzePatientQuestion(titre, description, categorie);
            Platform.runLater(() -> {
                aiAnalysisLoading.setText("");
                aiAnalysisLabel.setText(
                        analysis != null ? analysis : "Analyse indisponible.");
            });
        }).start();
    }

    // ── FILE CHOOSER + IMAGE ANALYSIS ────────────────────────
    @FXML
    public void choisirFichier(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Images & PDF", "*.png", "*.jpg", "*.jpeg", "*.pdf")
        );
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            if (file.length() > 2 * 1024 * 1024) {
                fichierError.setText("Fichier trop grand (max 2MB)");
                selectedFile = null;
                fichierLabel.setText("Aucun fichier choisi");
            } else {
                fichierError.setText("");
                selectedFile = file;
                fichierLabel.setText("📎 " + file.getName());

                // ✅ Analyze image immediately after selection
                if (imageAnalysisBox != null) {
                    imageAnalysisBox.setVisible(true);
                    imageAnalysisBox.setManaged(true);
                    imageAnalysisLabel.setText("⏳ Analyse de l'image en cours...");

                    new Thread(() -> {
                        AIService ai = new AIService();
                        String result = ai.analyzeImage(file.getAbsolutePath());
                        Platform.runLater(() -> {
                            if (result != null && !result.isEmpty()) {
                                imageAnalysisLabel.setText(result);
                            } else {
                                imageAnalysisLabel.setText(
                                        "⚠️ Analyse indisponible — " +
                                                "le médecin examinera l'image directement.");
                            }
                        });
                    }).start();
                }
            }
        }
    }

    // ── POST QUESTION ────────────────────────────────────────
    @FXML
    public void posterQuestion(ActionEvent event) {
        titreError.setText("");
        descriptionError.setText("");
        tailleError.setText("");
        poidsError.setText("");
        traitementError.setText("");
        allergiesError.setText("");
        fichierError.setText("");
        successLabel.setText("");

        boolean valid = true;

        String titre = titreField.getText().trim();
        if (titre.isEmpty()) {
            titreError.setText("Le titre est obligatoire.");
            valid = false;
        } else if (titre.length() < 5) {
            titreError.setText("Minimum 5 caractères.");
            valid = false;
        }

        String description = descriptionField.getText().trim();
        if (description.isEmpty()) {
            descriptionError.setText("La description est obligatoire.");
            valid = false;
        } else if (description.length() < 10) {
            descriptionError.setText("Minimum 10 caractères.");
            valid = false;
        }

        double taille = 0;
        try {
            taille = Double.parseDouble(tailleField.getText().trim());
            if (taille < 50 || taille > 250) {
                tailleError.setText("Taille invalide (50-250 cm).");
                valid = false;
            }
        } catch (NumberFormatException e) {
            tailleError.setText("Veuillez entrer un nombre valide.");
            valid = false;
        }

        double poids = 0;
        try {
            poids = Double.parseDouble(poidsField.getText().trim());
            if (poids < 2 || poids > 300) {
                poidsError.setText("Poids invalide (2-300 kg).");
                valid = false;
            }
        } catch (NumberFormatException e) {
            poidsError.setText("Veuillez entrer un nombre valide.");
            valid = false;
        }

        if (traitementGroup.getSelectedToggle() == null) {
            traitementError.setText("Veuillez répondre à cette question.");
            valid = false;
        }

        if (allergiesGroup.getSelectedToggle() == null) {
            allergiesError.setText("Veuillez répondre à cette question.");
            valid = false;
        }

        if (categorieCombo.getSelectionModel().isEmpty()) {
            categorieError.setText("Veuillez choisir une catégorie.");
            valid = false;
        }

        if (!valid) return;

        boolean sousTraitement = (traitementGroup.getSelectedToggle() == traitementOui);
        boolean aDesAllergies  = (allergiesGroup.getSelectedToggle() == allergiesOui);
        String fichierChemin   = selectedFile != null ? selectedFile.getAbsolutePath() : null;
        String descTraitement  = sousTraitement ? descTraitementField.getText().trim() : null;
        String descAllergies   = aDesAllergies  ? descAllergiesField.getText().trim()  : null;
        String categorie       = categorieCombo.getValue();

        // Question q = new Question(Session.getUserId(), titre, description, // Session management will be handled by the main project
        Question q = new Question(Session.getUserId(), titre, description,
                sousTraitement, aDesAllergies, fichierChemin,
                taille, poids, descTraitement, descAllergies);
        q.setCategorie(categorie);

        QuestionService qs = new QuestionService();
        qs.insert(q);

        // Auto-moderation in background
        new Thread(() ->
                new ModerationService().autoModerate(q.getId(), titre, description)
        ).start();

        successLabel.setText("✅ Question postée avec succès !");

        // Reset form
        titreField.clear();
        descriptionField.clear();
        tailleField.clear();
        poidsField.clear();
        categorieCombo.getSelectionModel().clearSelection();
        traitementGroup.selectToggle(null);
        allergiesGroup.selectToggle(null);
        traitementDetails.setVisible(false);
        traitementDetails.setManaged(false);
        allergiesDetails.setVisible(false);
        allergiesDetails.setManaged(false);
        descTraitementField.clear();
        descAllergiesField.clear();
        fichierLabel.setText("Aucun fichier choisi");
        selectedFile = null;

        // Hide all AI panels after posting
        if (similarBox != null) {
            similarBox.setVisible(false);
            similarBox.setManaged(false);
        }
        if (aiAnalysisBox != null) {
            aiAnalysisBox.setVisible(false);
            aiAnalysisBox.setManaged(false);
        }
        if (imageAnalysisBox != null) {
            imageAnalysisBox.setVisible(false);
            imageAnalysisBox.setManaged(false);
        }
    }

    // ── NAVIGATION ───────────────────────────────────────────
    @FXML
    public void seDeconnecter(ActionEvent event) {
        try {
            Session.clear();
            Parent root = FXMLLoader.load(getClass().getResource("/yessine/login.fxml"));
            titreField.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }

    @FXML
    public void voirReponses(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(
                    "/yessine/questionsRepondues.fxml"));
            titreField.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur navigation: " + e.getMessage());
        }
    }
}