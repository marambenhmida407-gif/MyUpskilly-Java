package org.example.service;

import org.example.model.Consultation;

import java.io.FileWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GoogleCalendarService {

    private static final DateTimeFormatter GOOGLE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    public String generateGoogleCalendarLink(Consultation c, String patientNom) {
        LocalDateTime start = c.getDateConsultation();
        LocalDateTime end = start.plusHours(1);

        String title = "Consultation - " + (patientNom != null ? patientNom : "Patient");
        String details = "Motif: " + (c.getMotif() != null ? c.getMotif() : "") +
                "\nDiagnostic: " + (c.getDiagnostic() != null ? c.getDiagnostic() : "") +
                "\nStatut: " + (c.getStatut() != null ? c.getStatut() : "");

        try {
            return "https://calendar.google.com/calendar/render?action=TEMPLATE" +
                    "&text=" + URLEncoder.encode(title, StandardCharsets.UTF_8) +
                    "&dates=" + start.format(GOOGLE_FORMAT) + "/" + end.format(GOOGLE_FORMAT) +
                    "&details=" + URLEncoder.encode(details, StandardCharsets.UTF_8) +
                    "&sf=true&output=xml";
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void generateICalFile(Consultation c, String patientNom, String filePath) {
        LocalDateTime start = c.getDateConsultation();
        LocalDateTime end = start.plusHours(1);

        DateTimeFormatter icalFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

        String ical = "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "PRODID:-//MyUpskilly//Consultation//FR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:" + start.format(icalFormat) + "\n" +
                "DTEND:" + end.format(icalFormat) + "\n" +
                "SUMMARY:Consultation - " + (patientNom != null ? patientNom : "Patient") + "\n" +
                "DESCRIPTION:Motif: " + (c.getMotif() != null ? c.getMotif() : "") +
                "\\nDiagnostic: " + (c.getDiagnostic() != null ? c.getDiagnostic() : "") +
                "\\nStatut: " + (c.getStatut() != null ? c.getStatut() : "") + "\n" +
                "STATUS:CONFIRMED\n" +
                "BEGIN:VALARM\n" +
                "TRIGGER:-PT30M\n" +
                "ACTION:DISPLAY\n" +
                "DESCRIPTION:Consultation dans 30 minutes\n" +
                "END:VALARM\n" +
                "BEGIN:VALARM\n" +
                "TRIGGER:-P1D\n" +
                "ACTION:DISPLAY\n" +
                "DESCRIPTION:Consultation demain\n" +
                "END:VALARM\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(ical);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}