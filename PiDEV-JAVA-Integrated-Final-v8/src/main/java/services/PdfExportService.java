package services;

import models.PrescriptionMedicale;
import models.SuiviTherapeutique;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class PdfExportService {

    private static final SimpleDateFormat FMT = new SimpleDateFormat("dd/MM/yyyy");
    private static final float MARGIN       = 50;
    private static final float PAGE_WIDTH   = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT  = PDRectangle.A4.getHeight();
    private static final float CONTENT_W    = PAGE_WIDTH - 2 * MARGIN;

    // ── Fonts ────────────────────────────────────────────────────────────────
    private PDType1Font fontBold;
    private PDType1Font fontRegular;

    private PDDocument   doc;
    private PDPage       page;
    private PDPageContentStream cs;
    private float        y; // current vertical position

    public File export(SuiviTherapeutique suivi,
                       ArrayList<PrescriptionMedicale> prescriptions) throws IOException {

        doc         = new PDDocument();
        fontBold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        addPage();

        // ── Header band ──────────────────────────────────────────────────────
        drawRect(MARGIN, y - 60, CONTENT_W, 60, new Color(30, 41, 59)); // #1E293B
        writeText("RAPPORT DE SUIVI THÉRAPEUTIQUE",
                fontBold, 14, Color.WHITE,
                MARGIN + 16, y - 24);
        writeText("Généré le " + FMT.format(new Date()),
                fontRegular, 9, new Color(148, 163, 184),
                MARGIN + 16, y - 42);
        writeText("Gestion Thérapeutique · PiDEV ESPRIT",
                fontRegular, 9, new Color(148, 163, 184),
                PAGE_WIDTH - MARGIN - 170, y - 42);
        y -= 80;

        // ── Section: Suivi info ───────────────────────────────────────────────
        sectionTitle("INFORMATIONS DU SUIVI");
        infoRow("Type de suivi",     suivi.getTypeSuivi());
        infoRow("Statut",            suivi.getStatut());
        infoRow("Objectif",          suivi.getObjectifTherapeutique());
        infoRow("Date de début",     suivi.getDateDebut() != null
                ? FMT.format(suivi.getDateDebut()) : "—");
        infoRow("Date de fin",       suivi.getDateFin() != null
                ? FMT.format(suivi.getDateFin()) : "—");
        y -= 10;

        // ── Statut badge ─────────────────────────────────────────────────────
        Color badgeColor = switch (suivi.getStatut()) {
            case "En cours" -> new Color(16, 185, 129);
            case "Terminé"  -> new Color(71, 85, 105);
            case "Suspendu" -> new Color(245, 158, 11);
            case "Planifié" -> new Color(59, 130, 246);
            default         -> new Color(100, 116, 139);
        };
        drawRect(MARGIN, y - 22, 90, 20, badgeColor);
        writeText(suivi.getStatut(), fontBold, 9, Color.WHITE, MARGIN + 8, y - 15);
        y -= 30;

        // ── Section: Prescriptions ────────────────────────────────────────────
        sectionTitle("PRESCRIPTIONS MÉDICALES (" + prescriptions.size() + ")");

        if (prescriptions.isEmpty()) {
            writeText("Aucune prescription associée à ce suivi.",
                    fontRegular, 10, new Color(148, 163, 184),
                    MARGIN, y);
            y -= 20;
        } else {
            for (int i = 0; i < prescriptions.size(); i++) {
                if (y < 160) addPage(); // new page if needed
                prescriptionCard(i + 1, prescriptions.get(i));
            }
        }

        // ── Footer ────────────────────────────────────────────────────────────
        drawLine(MARGIN, 55, PAGE_WIDTH - MARGIN, 55, new Color(226, 232, 240));
        writeText("Document généré automatiquement — Gestion Thérapeutique",
                fontRegular, 8, new Color(148, 163, 184), MARGIN, 44);
        writeText("Page 1",
                fontRegular, 8, new Color(148, 163, 184),
                PAGE_WIDTH - MARGIN - 30, 44);

        cs.close();

        // ── Save to Desktop ───────────────────────────────────────────────────
        String desktop = System.getProperty("user.home") + "/Desktop";
        String filename = "Suivi_" + suivi.getId() + "_"
                + suivi.getTypeSuivi().replaceAll("\\s+", "_")
                + "_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date())
                + ".pdf";
        File file = new File(desktop, filename);
        doc.save(file);
        doc.close();
        return file;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void addPage() throws IOException {
        if (cs != null) cs.close();
        page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        cs = new PDPageContentStream(doc, page);
        y = PAGE_HEIGHT - MARGIN;
    }

    private void sectionTitle(String title) throws IOException {
        y -= 6;
        drawRect(MARGIN, y - 22, CONTENT_W, 22, new Color(241, 245, 249));
        drawRect(MARGIN, y - 22, 4, 22, new Color(16, 185, 129));
        writeText(title, fontBold, 10, new Color(30, 41, 59), MARGIN + 12, y - 15);
        y -= 32;
    }

    private void infoRow(String label, String value) throws IOException {
        if (y < 100) addPage();
        writeText(label + " :", fontBold, 10, new Color(100, 116, 139), MARGIN, y);
        writeText(value != null ? value : "—",
                fontRegular, 10, new Color(30, 41, 59), MARGIN + 130, y);
        drawLine(MARGIN, y - 6, MARGIN + CONTENT_W, y - 6,
                new Color(241, 245, 249));
        y -= 20;
    }

    private void prescriptionCard(int num,
                                  PrescriptionMedicale p) throws IOException {
        float cardH = 80;
        drawRect(MARGIN, y - cardH, CONTENT_W, cardH, new Color(248, 250, 252));
        drawRect(MARGIN, y - cardH, 3, cardH, new Color(59, 130, 246));

        // Num badge
        drawRect(MARGIN + 10, y - 20, 20, 18, new Color(59, 130, 246));
        writeText(String.valueOf(num), fontBold, 9, Color.WHITE, MARGIN + 16, y - 14);

        // Date
        String dateStr = p.getDatePrescription() != null
                ? FMT.format(p.getDatePrescription()) : "—";
        writeText("Date : " + dateStr,
                fontBold, 9, new Color(100, 116, 139), MARGIN + 38, y - 14);

        // Medicaments
        writeText("Médicaments :",
                fontBold, 9, new Color(30, 41, 59), MARGIN + 12, y - 34);
        writeText(truncate(p.getMedicaments(), 70),
                fontRegular, 9, new Color(71, 85, 105), MARGIN + 100, y - 34);

        // Recommandations
        if (p.getRecommandations() != null && !p.getRecommandations().isBlank()) {
            writeText("Recommandations :",
                    fontBold, 9, new Color(30, 41, 59), MARGIN + 12, y - 50);
            writeText(truncate(p.getRecommandations(), 65),
                    fontRegular, 9, new Color(71, 85, 105), MARGIN + 120, y - 50);
        }

        // Suivi notes
        if (p.getSuivi() != null && !p.getSuivi().isBlank()) {
            writeText("Suivi :",
                    fontBold, 9, new Color(30, 41, 59), MARGIN + 12, y - 66);
            writeText(truncate(p.getSuivi(), 75),
                    fontRegular, 9, new Color(71, 85, 105), MARGIN + 60, y - 66);
        }

        y -= (cardH + 8);
    }

    private void writeText(String text, PDType1Font font, float size,
                           Color color, float x, float yPos) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, yPos);
        cs.showText(text != null ? text : "");
        cs.endText();
    }

    private void drawRect(float x, float yPos, float w, float h,
                          Color color) throws IOException {
        cs.setNonStrokingColor(color);
        cs.addRect(x, yPos, w, h);
        cs.fill();
    }

    private void drawLine(float x1, float y1, float x2, float y2,
                          Color color) throws IOException {
        cs.setStrokingColor(color);
        cs.setLineWidth(0.5f);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private String truncate(String text, int max) {
        if (text == null) return "—";
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }
}