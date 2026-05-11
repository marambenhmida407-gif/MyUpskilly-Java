package services.aziz;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import models.aziz.Consultation;
import models.aziz.Pathologie;

import java.io.FileOutputStream;
import java.util.List;

public class ExcelExportService {

    public void exportConsultations(List<Consultation> consultations, String filePath) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Consultations");

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        String[] headers = {"ID", "Patient ID", "Date", "Motif", "Diagnostic", "Observations", "Ordonnance", "Statut", "Pathologie"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i); cell.setCellValue(headers[i]); cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (Consultation c : consultations) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(c.getId());
            row.createCell(1).setCellValue(c.getPatientId());
            row.createCell(2).setCellValue(c.getDateConsultation() != null ? c.getDateConsultation().toString() : "");
            row.createCell(3).setCellValue(c.getMotif() != null ? c.getMotif() : "");
            row.createCell(4).setCellValue(c.getDiagnostic() != null ? c.getDiagnostic() : "");
            row.createCell(5).setCellValue(c.getObservations() != null ? c.getObservations() : "");
            row.createCell(6).setCellValue(c.getOrdonnance() != null ? c.getOrdonnance() : "");
            row.createCell(7).setCellValue(c.getStatut() != null ? c.getStatut() : "");
            row.createCell(8).setCellValue(c.getPathologieNom() != null ? c.getPathologieNom() : "");
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

        // Stats sheet
        Sheet statsSheet = workbook.createSheet("Statistiques");
        Row sh = statsSheet.createRow(0);
        Cell s0 = sh.createCell(0); s0.setCellValue("Statut"); s0.setCellStyle(headerStyle);
        Cell s1 = sh.createCell(1); s1.setCellValue("Nombre"); s1.setCellStyle(headerStyle);
        java.util.Map<String, Long> stats = consultations.stream()
                .collect(java.util.stream.Collectors.groupingBy(c -> c.getStatut() != null ? c.getStatut() : "Inconnu", java.util.stream.Collectors.counting()));
        int sr = 1;
        for (java.util.Map.Entry<String, Long> entry : stats.entrySet()) {
            Row row = statsSheet.createRow(sr++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }

        FileOutputStream out = new FileOutputStream(filePath);
        workbook.write(out); out.close(); workbook.close();
    }

    public void exportPathologies(List<Pathologie> pathologies, String filePath) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Pathologies");

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font headerFont = workbook.createFont();
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        String[] headers = {"ID", "Nom", "Description", "Type", "Gravité"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i); cell.setCellValue(headers[i]); cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (Pathologie p : pathologies) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(p.getId());
            row.createCell(1).setCellValue(p.getNom() != null ? p.getNom() : "");
            row.createCell(2).setCellValue(p.getDescription() != null ? p.getDescription() : "");
            row.createCell(3).setCellValue(p.getType() != null ? p.getType() : "");
            row.createCell(4).setCellValue(p.getGravite() != null ? p.getGravite() : "");
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

        FileOutputStream out = new FileOutputStream(filePath);
        workbook.write(out); out.close(); workbook.close();
    }
}