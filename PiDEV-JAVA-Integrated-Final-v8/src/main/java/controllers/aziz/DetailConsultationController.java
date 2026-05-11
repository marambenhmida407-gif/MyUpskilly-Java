package controllers.aziz;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import models.aziz.Consultation;
import services.aziz.EmailService;
import services.aziz.GoogleCalendarService;
import services.aziz.QRCodeService;
import services.aziz.StripeService;
import services.aziz.TwilioSMSService;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;

public class DetailConsultationController implements Initializable {

    @FXML private Label lblTitle;
    @FXML private Label lblConsultId;
    @FXML private Label lblDate;
    @FXML private Label lblStatut;
    @FXML private Label lblPatient;
    @FXML private Label lblPathologie;
    @FXML private Label lblMotif;
    @FXML private Label lblDiagnostic;
    @FXML private Label lblObservations;
    @FXML private Label lblOrdonnance;
    @FXML private ImageView imgQRCode;

    private Consultation consultation;
    private final GoogleCalendarService googleCalendarService = new GoogleCalendarService();
    private final QRCodeService qrCodeService = new QRCodeService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    public void setConsultation(Consultation c) {
        this.consultation = c;

        lblConsultId.setText("Consultation #" + c.getId());
        lblDate.setText(c.getDateConsultation() != null ? c.getDateConsultation().toString().replace("T", " à ") : "—");

        String statut = c.getStatut() != null ? c.getStatut() : "—";
        lblStatut.setText(statut);
        switch (statut) {
            case "en_cours" -> lblStatut.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-padding: 4 14; -fx-background-radius: 12; -fx-font-weight: bold;");
            case "terminee" -> lblStatut.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #047857; -fx-padding: 4 14; -fx-background-radius: 12; -fx-font-weight: bold;");
            case "annulee" -> lblStatut.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-padding: 4 14; -fx-background-radius: 12; -fx-font-weight: bold;");
            case "programmee" -> lblStatut.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 4 14; -fx-background-radius: 12; -fx-font-weight: bold;");
            default -> lblStatut.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-padding: 4 14; -fx-background-radius: 12;");
        }

        String patientInfo = c.getPatientNom() != null ? c.getPatientNom() : "Patient #" + c.getPatientId();
        if (c.getPatientEmail() != null) patientInfo += " - " + c.getPatientEmail();
        lblPatient.setText(patientInfo);

        if (c.getPathologieNom() != null) {
            lblPathologie.setText(c.getPathologieNom());
            lblPathologie.setStyle("-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8; -fx-padding: 4 14; -fx-background-radius: 12; -fx-font-weight: bold;");
        } else {
            lblPathologie.setText("Aucune");
            lblPathologie.setStyle("-fx-text-fill: #94a3b8;");
        }

        lblMotif.setText(c.getMotif() != null ? c.getMotif() : "—");
        lblDiagnostic.setText(c.getDiagnostic() != null ? c.getDiagnostic() : "—");
        lblObservations.setText(c.getObservations() != null && !c.getObservations().isEmpty() ? c.getObservations() : "Aucune observation");
        lblOrdonnance.setText(c.getOrdonnance() != null && !c.getOrdonnance().isEmpty() ? c.getOrdonnance() : "Aucune ordonnance");

        // Générer QR Code
        javafx.scene.image.Image qrImage = qrCodeService.generateQRCodeImage(c, 200);
        if (qrImage != null) {
            imgQRCode.setImage(qrImage);
        }
    }

    @FXML
    private void handleRappel() {
        if (consultation.getPatientEmail() == null || consultation.getPatientEmail().isEmpty()) {
            showAlert("Ce patient n'a pas d'adresse email.");
            return;
        }
        try {
            new EmailService().sendRappel(consultation, consultation.getPatientEmail());
            showSuccess("Rappel envoyé à " + consultation.getPatientEmail() + " !");
        } catch (Exception e) {
            showAlert("Erreur email : " + e.getMessage());
        }
    }

    @FXML
    private void handleGoogleCalendar() {
        String link = googleCalendarService.generateGoogleCalendarLink(consultation, consultation.getPatientNom());
        if (link != null) {
            try {
                Desktop.getDesktop().browse(new URI(link));
                showSuccess("Google Calendar ouvert !");
            } catch (Exception e) {
                showAlert("Erreur : " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDownloadICal() {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("consultation_" + consultation.getId() + ".ics");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("iCal", "*.ics"));
        java.io.File file = fc.showSaveDialog(lblTitle.getScene().getWindow());
        if (file != null) {
            googleCalendarService.generateICalFile(consultation, consultation.getPatientNom(), file.getAbsolutePath());
            showSuccess("Fichier iCal enregistré !");
        }
    }

    @FXML
    private void handleSaveQRCode() {
        FileChooser fc = new FileChooser();
        fc.setInitialFileName("qr_consultation_" + consultation.getId() + ".png");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        java.io.File file = fc.showSaveDialog(lblTitle.getScene().getWindow());
        if (file != null) {
            qrCodeService.saveQRCodeToFile(consultation, file.getAbsolutePath(), 400);
            showSuccess("QR Code enregistré !\n" + file.getAbsolutePath());
        }
    }

    @FXML
    private void handleModifier() {
        showAlert("Retournez à la liste et double-cliquez sur la consultation pour la modifier.");
    }

    @FXML
    private void handlePayer() {
        try {
            String url = new StripeService().createCheckoutUrl(consultation,
                    consultation.getPatientNom() != null ? consultation.getPatientNom() : "Patient");
            if (url != null) {
                Desktop.getDesktop().browse(new URI(url));
                showSuccess("Page de paiement Stripe ouverte !");
            } else {
                showAlert("Erreur lors de la création du paiement.");
            }
        } catch (Exception e) {
            showAlert("Erreur Stripe : " + e.getMessage());
        }
    }

    @FXML
    private void handleRetour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/aziz/consultation.fxml"));
            javafx.scene.Node page = loader.load();
            BorderPane root = (BorderPane) lblTitle.getScene().getRoot();
            root.setCenter(page);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleSMS() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Envoyer SMS");
        dialog.setHeaderText("Rappel par SMS");
        dialog.setContentText("Numéro de téléphone (ex: +21612345678) :");
        dialog.showAndWait().ifPresent(number -> {
            try {
                new TwilioSMSService().sendRappelSMS(consultation, number);
                showSuccess("SMS envoyé à " + number + " !");
            } catch (Exception e) {
                showAlert("Erreur SMS : " + e.getMessage());
            }
        });
    }
    private void showAlert(String msg) { new Alert(Alert.AlertType.WARNING, msg).showAndWait(); }
    private void showSuccess(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
}
