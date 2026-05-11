package controllers.yessine;

import utils.Session;
import models.yessine.Reponse;
import services.yessine.FeedbackService;
import services.yessine.ReponseService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class VoirReponseController {

    @FXML private Label    titrePage;
    @FXML private Label    questionDescLabel;
    @FXML private Label    doctorNameLabel;
    @FXML private Label    doctorSpecLabel;
    @FXML private Label    reponseLabel;
    @FXML private HBox     emojiRow;
    @FXML private Label    ratingLabel;
    @FXML private TextArea commentaireField;
    @FXML private Label    feedbackSuccess;
    @FXML private Label    feedbackError;
    @FXML private VBox     imageContainer;
    @FXML private VBox     feedbackListContainer;
    @FXML private Label    categorieLabel;
    @FXML private VBox     feedbackSection;

    private Reponse reponse;
    private int selectedRating = 0;
    private final ReponseService reponseService = new ReponseService();
    private final FeedbackService feedbackService = new FeedbackService();

    private static final String[] EMOJIS = {"😞", "😐", "😊", "😄", "🤩"};
    private static final String[] EMOJI_LABELS = {
            "Pas satisfait", "Peu satisfait", "Satisfait",
            "Très satisfait", "Excellent !"
    };

    // ── SET RESPONSE ─────────────────────────────────────────
    public void setReponse(Reponse rep) {
        this.reponse = rep;
        titrePage.setText(rep.getQuestionTitre());
        questionDescLabel.setText(rep.getQuestionDescription());
        doctorNameLabel.setText("Dr " + rep.getDoctorPrenom()
                + " " + rep.getDoctorNom());
        doctorSpecLabel.setText(rep.getDoctorSpecialite() != null
                ? rep.getDoctorSpecialite() : "Médecin");
        reponseLabel.setText(rep.getContenu());

        if (rep.getCategorie() != null) {
            categorieLabel.setText("📂 " + rep.getCategorie());
        }

        // Hide feedback FORM for doctors — they can still see feedbacks
        if (Session.isDoctor()) {
            feedbackSection.setVisible(false);
            feedbackSection.setManaged(false);
        }

        loadImage(rep.getFichierChemin());
        buildEmojiRating();
        loadFeedbacks();
    }

    // ── LOAD IMAGE ───────────────────────────────────────────
    private void loadImage(String path) {
        imageContainer.getChildren().clear();
        if (path == null || path.isEmpty()) return;

        File file = new File(path);
        if (!file.exists()) return;

        if (path.toLowerCase().endsWith(".pdf")) {
            Label pdfLabel = new Label("📄 Fichier PDF joint: " + file.getName());
            pdfLabel.setStyle("-fx-text-fill: #1a73e8; -fx-font-size: 12px;");
            imageContainer.getChildren().add(pdfLabel);
            return;
        }

        try {
            Image image = new Image(file.toURI().toString());
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(450);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);

            Label imgLabel = new Label("📎 Image jointe par le patient:");
            imgLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;" +
                    "-fx-font-size: 12px;");

            imageContainer.getChildren().addAll(imgLabel, imageView);
        } catch (Exception e) {
            System.err.println("Erreur chargement image: " + e.getMessage());
        }
    }

    // ── LOAD FEEDBACKS ───────────────────────────────────────
    private void loadFeedbacks() {
        feedbackListContainer.getChildren().clear();

        List<String[]> feedbacks = reponseService.getFeedbacks(reponse.getId());

        if (feedbacks.isEmpty()) {
            Label noFeedback = new Label("Aucun avis pour le moment.");
            noFeedback.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
            feedbackListContainer.getChildren().add(noFeedback);
            return;
        }

        for (String[] fb : feedbacks) {
            feedbackListContainer.getChildren().add(buildFeedbackCard(fb));
        }
    }

    // ── BUILD FEEDBACK CARD ──────────────────────────────────
    private VBox buildFeedbackCard(String[] fb) {
        // fb: 0=rating, 1=commentaire, 2=user_nom, 3=created_at, 4=id
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 12;" +
                "-fx-background-radius: 10; -fx-border-color: #e0e0e0;" +
                "-fx-border-radius: 10;");

        // ── Top row: name + date + report button ──
        HBox topRow = new HBox(10);
        topRow.setStyle("-fx-alignment: CENTER_LEFT;");

        Label userName = new Label("👤 " + fb[2]);
        userName.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;" +
                "-fx-text-fill: #333;");

        Label date = new Label(fb[3] != null && fb[3].length() >= 10
                ? fb[3].substring(0, 10) : "");
        date.setStyle("-fx-text-fill: #999; -fx-font-size: 11px;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        // ✅ Report button — visible for everyone
        Button reportBtn = new Button("🚩");
        reportBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #e74c3c;" +
                        "-fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
        reportBtn.setTooltip(new Tooltip("Signaler ce commentaire"));

        final String feedbackId = fb.length > 4 ? fb[4] : null;

        reportBtn.setOnAction(e -> showReportDialog(feedbackId));

        topRow.getChildren().addAll(userName, sp, date, reportBtn);

        // ── Emoji rating ──
        int rating = 0;
        try { rating = Integer.parseInt(fb[0]); } catch (Exception ignored) {}
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < rating; i++)
            stars.append(EMOJIS[Math.min(rating - 1, 4)]);
        Label emojiStars = new Label(stars.toString());
        emojiStars.setStyle("-fx-font-size: 16px;");

        card.getChildren().addAll(topRow, emojiStars);

        // ── Comment ──
        if (fb[1] != null && !fb[1].isEmpty()) {
            Label comment = new Label(fb[1]);
            comment.setStyle("-fx-text-fill: #444; -fx-font-size: 12px;");
            comment.setWrapText(true);
            card.getChildren().add(comment);
        }

        return card;
    }

    // ── REPORT DIALOG ────────────────────────────────────────
    private void showReportDialog(String feedbackId) {
        System.out.println("=== showReportDialog called ===");
        System.out.println("feedbackId = " + feedbackId);
        if (feedbackId == null) {
            System.out.println("❌ feedbackId is NULL — returning");
            return;
        }

        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Signaler ce commentaire");
        dialog.setHeaderText("Pourquoi signalez-vous ce commentaire ?");

        ComboBox<String> raisonCombo = new ComboBox<>();
        raisonCombo.getItems().addAll(
                "Contenu inapproprié",
                "Insultes ou langage offensant",
                "Spam ou publicité",
                "Fausse information",
                "Autre"
        );
        raisonCombo.setPromptText("Choisir une raison...");
        raisonCombo.setPrefWidth(300);

        TextField autreField = new TextField();
        autreField.setPromptText("Précisez la raison...");
        autreField.setVisible(false);
        autreField.setManaged(false);

        raisonCombo.setOnAction(ev -> {
            boolean isAutre = "Autre".equals(raisonCombo.getValue());
            autreField.setVisible(isAutre);
            autreField.setManaged(isAutre);
        });

        VBox content = new VBox(10, raisonCombo, autreField);
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK && raisonCombo.getValue() != null) {
                String raison = "Autre".equals(raisonCombo.getValue())
                        ? autreField.getText().trim()
                        : raisonCombo.getValue();

                if (raison.isEmpty()) raison = "Contenu inapproprié";

                System.out.println("=== Calling reportFeedback ===");
                System.out.println("feedbackId: " + feedbackId);
                System.out.println("userId: " + Session.getUserId());
                System.out.println("raison: " + raison);
                feedbackService.reportFeedback(
                        Integer.parseInt(feedbackId),
                        Session.getUserId(),
                        raison
                );
                System.out.println("✅ reportFeedback done");

                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Signalement envoyé");
                ok.setHeaderText(null);
                ok.setContentText(
                        "✅ Merci, le signalement a été transmis à l'administrateur.");
                ok.showAndWait();
            }
        });
    }

    // ── BUILD EMOJI RATING ───────────────────────────────────
    private void buildEmojiRating() {
        emojiRow.getChildren().clear();
        for (int i = 0; i < EMOJIS.length; i++) {
            final int rating = i + 1;
            Button emojiBtn = new Button(EMOJIS[i]);
            emojiBtn.setStyle(
                    "-fx-font-size: 28px; -fx-background-color: transparent;" +
                            "-fx-cursor: hand; -fx-border-color: transparent;");
            emojiBtn.setOnAction(e -> {
                selectedRating = rating;
                ratingLabel.setText(EMOJI_LABELS[rating - 1]);
                highlightSelected(rating);
            });
            emojiRow.getChildren().add(emojiBtn);
        }
    }

    private void highlightSelected(int rating) {
        for (int i = 0; i < emojiRow.getChildren().size(); i++) {
            Button btn = (Button) emojiRow.getChildren().get(i);
            if (i < rating) {
                btn.setStyle(
                        "-fx-font-size: 32px; -fx-background-color: #eaf4fb;" +
                                "-fx-background-radius: 50; -fx-cursor: hand;");
            } else {
                btn.setStyle(
                        "-fx-font-size: 28px; -fx-background-color: transparent;" +
                                "-fx-cursor: hand;");
            }
        }
    }

    // ── SEND FEEDBACK ────────────────────────────────────────
    @FXML
    public void envoyerFeedback(ActionEvent event) {
        feedbackError.setText("");
        feedbackSuccess.setText("");

        if (selectedRating == 0) {
            feedbackError.setText("Veuillez sélectionner une note.");
            return;
        }

        String commentaire = commentaireField.getText().trim();
        String userName = Session.getPrenom() + " " + Session.getNom();

        feedbackService.insertFeedback(
                reponse.getId(), selectedRating, commentaire, userName);

        feedbackSuccess.setText("✅ Merci pour votre avis !");
        commentaireField.clear();
        selectedRating = 0;
        ratingLabel.setText("");

        for (javafx.scene.Node node : emojiRow.getChildren()) {
            ((Button) node).setStyle(
                    "-fx-font-size: 28px; -fx-background-color: transparent;" +
                            "-fx-cursor: hand;");
        }

        loadFeedbacks();
    }

    // ── NAVIGATION ───────────────────────────────────────────
    @FXML
    public void retour(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(
                    "/yessine/questionsRepondues.fxml"));
            titrePage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur retour: " + e.getMessage());
        }
    }

    @FXML
    public void seDeconnecter(ActionEvent event) {
        try {
            Session.clear();
            Parent root = FXMLLoader.load(getClass().getResource(
                    "/yessine/login.fxml"));
            titrePage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }

    @FXML
    public void initialize() {}
}