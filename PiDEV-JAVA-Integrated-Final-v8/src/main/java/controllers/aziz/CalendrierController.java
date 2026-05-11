package controllers.aziz;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import models.aziz.Consultation;
import services.aziz.ConsultationService;

import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class CalendrierController implements Initializable {

    @FXML private Label lblMoisAnnee;
    @FXML private GridPane gridCalendrier;
    @FXML private ListView<String> listConsultationsJour;
    @FXML private Label lblJourSelectionne;
    @FXML private ComboBox<String> cbFiltreStatut;

    private final ConsultationService consultationService = new ConsultationService();
    private List<Consultation> allConsultations;
    private YearMonth currentYearMonth;
    private LocalDate selectedDate;

    private static final String[] JOURS = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentYearMonth = YearMonth.now();
        cbFiltreStatut.setItems(FXCollections.observableArrayList("Tous", "en_cours", "terminee", "annulee", "programmee"));
        cbFiltreStatut.setValue("Tous");
        cbFiltreStatut.valueProperty().addListener((obs, o, n) -> buildCalendar());
        loadData();
    }

    private void loadData() {
        allConsultations = consultationService.getAll();
        buildCalendar();
    }

    private void buildCalendar() {
        gridCalendrier.getChildren().clear();
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(JOURS[i]);
            lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
            lbl.setTextFill(Color.web("#2c3e50"));
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            lbl.setPadding(new Insets(6));
            gridCalendrier.add(lbl, i, 0);
        }

        LocalDate firstDay = currentYearMonth.atDay(1);
        int startCol = firstDay.getDayOfWeek().getValue() - 1;
        int daysInMonth = currentYearMonth.lengthOfMonth();

        String filtreStatut = cbFiltreStatut.getValue();
        Map<LocalDate, List<Consultation>> parJour = allConsultations.stream()
                .filter(c -> c.getDateConsultation() != null)
                .filter(c -> filtreStatut == null || filtreStatut.equals("Tous") || filtreStatut.equals(c.getStatut()))
                .filter(c -> YearMonth.from(c.getDateConsultation()).equals(currentYearMonth))
                .collect(Collectors.groupingBy(c -> c.getDateConsultation().toLocalDate()));

        int col = startCol, row = 1;
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentYearMonth.atDay(day);
            List<Consultation> consults = parJour.getOrDefault(date, Collections.emptyList());
            VBox cell = createDayCell(day, date, consults);
            gridCalendrier.add(cell, col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }
        lblMoisAnnee.setText(currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)));
    }

    private VBox createDayCell(int day, LocalDate date, List<Consultation> consults) {
        VBox cell = new VBox(2);
        cell.setPadding(new Insets(4)); cell.setMinHeight(70); cell.setMinWidth(80);
        cell.setCursor(javafx.scene.Cursor.HAND);

        boolean isToday = date.equals(LocalDate.now());
        boolean isSelected = date.equals(selectedDate);

        if (isSelected) cell.setStyle("-fx-background-color: #3498db; -fx-background-radius: 6;");
        else if (isToday) cell.setStyle("-fx-background-color: #eaf4fb; -fx-background-radius: 6; -fx-border-color: #3498db; -fx-border-radius: 6; -fx-border-width: 2;");
        else cell.setStyle("-fx-background-color: white; -fx-background-radius: 6; -fx-border-color: #dcdde1; -fx-border-radius: 6;");

        Label lblDay = new Label(String.valueOf(day));
        lblDay.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblDay.setTextFill(isSelected ? Color.WHITE : isToday ? Color.web("#3498db") : Color.web("#2c3e50"));
        cell.getChildren().add(lblDay);

        for (int i = 0; i < Math.min(consults.size(), 2); i++) {
            Consultation c = consults.get(i);
            Label badge = new Label(c.getMotif() != null ? (c.getMotif().length() > 8 ? c.getMotif().substring(0, 8) + "…" : c.getMotif()) : "—");
            badge.setFont(Font.font("Segoe UI", 9));
            badge.setPadding(new Insets(1, 4, 1, 4));
            badge.setMaxWidth(Double.MAX_VALUE);
            badge.setStyle("-fx-background-color: " + getBadgeColor(c.getStatut()) + "; -fx-text-fill: white; -fx-background-radius: 3;");
            cell.getChildren().add(badge);
        }
        if (consults.size() > 2) {
            Label more = new Label("+" + (consults.size() - 2));
            more.setFont(Font.font("Segoe UI", 9));
            more.setTextFill(isSelected ? Color.WHITE : Color.web("#7f8c8d"));
            cell.getChildren().add(more);
        }

        cell.setOnMouseClicked(e -> { selectedDate = date; buildCalendar(); showConsultationsJour(date, consults); });
        return cell;
    }

    private String getBadgeColor(String statut) {
        if (statut == null) return "#95a5a6";
        return switch (statut) { case "en_cours" -> "#3498db"; case "terminee" -> "#27ae60"; case "annulee" -> "#e74c3c"; case "programmee" -> "#f39c12"; default -> "#95a5a6"; };
    }

    private void showConsultationsJour(LocalDate date, List<Consultation> consults) {
        lblJourSelectionne.setText("Consultations du " + date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)));
        List<String> items = consults.stream().map(c -> "• " + (c.getMotif() != null ? c.getMotif() : "-") + " | Patient #" + c.getPatientId() + " | " + (c.getStatut() != null ? c.getStatut() : "-")).collect(Collectors.toList());
        if (items.isEmpty()) items.add("Aucune consultation.");
        listConsultationsJour.setItems(FXCollections.observableArrayList(items));
    }

    @FXML private void handlePrevMois() { currentYearMonth = currentYearMonth.minusMonths(1); selectedDate = null; listConsultationsJour.getItems().clear(); lblJourSelectionne.setText("Cliquez sur un jour"); buildCalendar(); }
    @FXML private void handleNextMois() { currentYearMonth = currentYearMonth.plusMonths(1); selectedDate = null; listConsultationsJour.getItems().clear(); lblJourSelectionne.setText("Cliquez sur un jour"); buildCalendar(); }
    @FXML private void handleAujourdhui() {
        currentYearMonth = YearMonth.now(); selectedDate = LocalDate.now(); buildCalendar();
        List<Consultation> today = allConsultations.stream().filter(c -> c.getDateConsultation() != null && c.getDateConsultation().toLocalDate().equals(LocalDate.now())).collect(Collectors.toList());
        showConsultationsJour(LocalDate.now(), today);
    }
}