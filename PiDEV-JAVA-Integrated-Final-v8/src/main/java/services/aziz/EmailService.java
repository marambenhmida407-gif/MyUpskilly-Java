package services.aziz;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import models.aziz.Consultation;

import java.util.Properties;

public class EmailService {

    private static final String FROM_EMAIL = "azizdawdi02@gmail.com";
    private static final String PASSWORD = "lmih nbqg nrlh cmkn";
    private static final String HOST = "smtp.gmail.com";

    private Session getSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", HOST);
        props.put("mail.smtp.port", "587");
        return Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });
    }

    public void sendConfirmation(Consultation c, String toEmail) throws MessagingException {
        String subject = "Confirmation - Consultation #" + c.getId();
        String body = "<html><body style='font-family:Arial;padding:20px;'>"
                + "<h2 style='color:#27ae60;'>Consultation Confirmée</h2>"
                + "<p><b>ID:</b> " + c.getId() + "</p>"
                + "<p><b>Patient:</b> #" + c.getPatientId() + "</p>"
                + "<p><b>Date:</b> " + c.getDateConsultation() + "</p>"
                + "<p><b>Motif:</b> " + c.getMotif() + "</p>"
                + "<p><b>Statut:</b> " + c.getStatut() + "</p>"
                + "<p style='color:#7f8c8d;font-size:12px;'>MyUpskilly - Gestion Médicale</p>"
                + "</body></html>";
        sendEmail(toEmail, subject, body);
    }

    public void sendRappel(Consultation c, String toEmail) throws MessagingException {
        String subject = "Rappel - Consultation #" + c.getId();
        String body = "<html><body style='font-family:Arial;padding:20px;'>"
                + "<h2 style='color:#f39c12;'>Rappel de Consultation</h2>"
                + "<p>Votre consultation est prévue pour : <b>" + c.getDateConsultation() + "</b></p>"
                + "<p>Motif : " + c.getMotif() + "</p>"
                + "<p style='color:#7f8c8d;font-size:12px;'>MyUpskilly - Gestion Médicale</p>"
                + "</body></html>";
        sendEmail(toEmail, subject, body);
    }

    public void sendAnnulation(Consultation c, String toEmail) throws MessagingException {
        String subject = "Annulation - Consultation #" + c.getId();
        String body = "<html><body style='font-family:Arial;padding:20px;'>"
                + "<h2 style='color:#e74c3c;'>Consultation Annulée</h2>"
                + "<p>La consultation du " + c.getDateConsultation() + " a été annulée.</p>"
                + "<p style='color:#7f8c8d;font-size:12px;'>MyUpskilly - Gestion Médicale</p>"
                + "</body></html>";
        sendEmail(toEmail, subject, body);
    }

    private void sendEmail(String to, String subject, String htmlBody) throws MessagingException {
        Session session = getSession();
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setContent(htmlBody, "text/html; charset=utf-8");
        Transport.send(message);
    }
}