package org.example.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.example.model.Consultation;

public class TwilioSMSService {

    private static final String ACCOUNT_SID = "VOTRE_ACCOUNT_SID";
    private static final String AUTH_TOKEN = "VOTRE_AUTH_TOKEN";
    private static final String FROM_NUMBER = "+1234567890"; // Numéro Twilio

    public void sendRappelSMS(Consultation c, String toNumber) {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        String body = "📋 Rappel - Consultation #" + c.getId() + "\n"
                + "📅 Date: " + (c.getDateConsultation() != null ? c.getDateConsultation().toString().replace("T", " à ") : "—") + "\n"
                + "🏥 Motif: " + (c.getMotif() != null ? c.getMotif() : "—") + "\n"
                + "📌 Statut: " + (c.getStatut() != null ? c.getStatut() : "—") + "\n"
                + "\nSanter Health System - MyUpskilly";

        Message message = Message.creator(
                new PhoneNumber(toNumber),
                new PhoneNumber(FROM_NUMBER),
                body
        ).create();

        System.out.println("SMS envoyé: " + message.getSid());
    }

    public void sendConfirmationSMS(Consultation c, String toNumber) {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        String body = "✅ Consultation confirmée #" + c.getId() + "\n"
                + "📅 " + (c.getDateConsultation() != null ? c.getDateConsultation().toString().replace("T", " à ") : "") + "\n"
                + "🏥 " + (c.getMotif() != null ? c.getMotif() : "") + "\n"
                + "\nSanter Health System";

        Message message = Message.creator(
                new PhoneNumber(toNumber),
                new PhoneNumber(FROM_NUMBER),
                body
        ).create();

        System.out.println("SMS envoyé: " + message.getSid());
    }

    public void sendAnnulationSMS(Consultation c, String toNumber) {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        String body = "❌ Consultation annulée #" + c.getId() + "\n"
                + "📅 " + (c.getDateConsultation() != null ? c.getDateConsultation().toString().replace("T", " à ") : "") + "\n"
                + "Veuillez reprendre rendez-vous.\n"
                + "\nSanter Health System";

        Message message = Message.creator(
                new PhoneNumber(toNumber),
                new PhoneNumber(FROM_NUMBER),
                body
        ).create();

        System.out.println("SMS envoyé: " + message.getSid());
    }
}