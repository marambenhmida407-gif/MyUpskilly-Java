package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import models.SuiviTherapeutique;
import services.ServiceSuiviTherapeutique;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class CalendarController implements Initializable {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private GridPane calendarGrid;
    @FXML private GridPane dayHeaderGrid;
    @FXML private Label    lblMonthYear;
    @FXML private Label    lblSelectedInfo;

    // Detail panel
    @FXML private VBox  detailCards;
    @FXML private Label lblDetailDay;
    @FXML private Label lblDetailCount;
    @FXML private Label lblTodayBadge;

    // ── State ─────────────────────────────────────────────────────────────────
    private YearMonth   currentMonth;
    private LocalDate   selectedDate = null;
    private StackPane   selectedCell = null;

    private List<SuiviTherapeutique>             allSuivis = new ArrayList<>();
    private final ServiceSuiviTherapeutique      service   = new ServiceSuiviTherapeutique();
    private final Map<LocalDate, StackPane>       cellMap   = new HashMap<>();

    private static final String[] DAY_NAMES = {"L", "M", "M", "J", "V", "S", "D"};
    private static final int      CELL_SIZE = 52; // compact square cell

    // ── Color scheme per statut ───────────────────────────────────────────────
    private record Colors(String dot, String bg, String border, String text, String cardBg) {}
    private static final Map<String, Colors> PALETTE = Map.of(
            "En cours", new Colors("#10B981","#ECFDF5","#10B981","#065F46","#F0FDF4"),
            "Planifié", new Colors("#3B82F6","#EFF6FF","#3B82F6","#1E40AF","#EFF6FF"),
            "Suspendu", new Colors("#F59E0B","#FFFBEB","#F59E0B","#92400E","#FFFBEB"),
            "Terminé",  new Colors("#94A3B8","#F8FAFC","#CBD5E1","#475569","#F8FAFC")
    );
    private static Colors palette(String statut) {
        return PALETTE.getOrDefault(statut,
                new Colors("#CBD5E1","#F8FAFC","#E2E8F0","#64748B","#F8FAFC"));
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentMonth = YearMonth.now();
        loadSuivis();
        buildDayHeaders();
        buildCalendar();
        // Show today's events by default
        showDayDetail(LocalDate.now());
    }

    private void loadSuivis() {
        try { allSuivis = service.afficherAll(); }
        catch (SQLException e) { System.err.println("Calendar load error: " + e.getMessage()); }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DAY HEADERS  L M M J V S D
    // ════════════════════════════════════════════════════════════════════════
    private void buildDayHeaders() {
        dayHeaderGrid.getChildren().clear();
        dayHeaderGrid.getColumnConstraints().clear();

        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setMinWidth(CELL_SIZE);
            cc.setPrefWidth(CELL_SIZE);
            cc.setHgrow(Priority.ALWAYS);
            dayHeaderGrid.getColumnConstraints().add(cc);

            boolean weekend = (i == 5 || i == 6);
            Label lbl = new Label(DAY_NAMES[i]);
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            lbl.setStyle(
                    "-fx-font-size: 10px; -fx-font-weight: bold;"
                            + "-fx-text-fill: " + (weekend ? "#CBD5E1" : "#94A3B8") + ";"
                            + "-fx-padding: 6 0;"
            );
            dayHeaderGrid.add(lbl, i, 0);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CALENDAR GRID
    // ════════════════════════════════════════════════════════════════════════
    private void buildCalendar() {
        calendarGrid.getChildren().clear();
        calendarGrid.getRowConstraints().clear();
        calendarGrid.getColumnConstraints().clear();
        cellMap.clear();

        // Month label
        String monthName = currentMonth.getMonth()
                .getDisplayName(TextStyle.FULL, Locale.FRENCH);
        monthName = monthName.substring(0,1).toUpperCase() + monthName.substring(1);
        lblMonthYear.setText(monthName + " " + currentMonth.getYear());

        // 7 equal columns
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setMinWidth(CELL_SIZE);
            cc.setPrefWidth(CELL_SIZE);
            cc.setHgrow(Priority.ALWAYS);
            calendarGrid.getColumnConstraints().add(cc);
        }

        LocalDate firstDay    = currentMonth.atDay(1);
        int       startCol    = firstDay.getDayOfWeek().getValue() - 1;
        int       daysInMonth = currentMonth.lengthOfMonth();
        LocalDate today       = LocalDate.now();

        // Pre-add all row constraints
        int totalRows = (int) Math.ceil((startCol + daysInMonth) / 7.0);
        for (int r = 0; r < totalRows; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setMinHeight(CELL_SIZE);
            rc.setPrefHeight(CELL_SIZE);
            rc.setVgrow(Priority.ALWAYS);
            calendarGrid.getRowConstraints().add(rc);
        }

        int row = 0, col = startCol;
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date  = currentMonth.atDay(day);
            StackPane cell  = buildDayCell(date, day, today);
            cellMap.put(date, cell);
            calendarGrid.add(cell, col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }

        // Re-highlight selected date if still in this month
        if (selectedDate != null && cellMap.containsKey(selectedDate)) {
            applySelectedStyle(cellMap.get(selectedDate), true);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SINGLE CELL — compact square with dot indicators
    // ════════════════════════════════════════════════════════════════════════
    private StackPane buildDayCell(LocalDate date, int day, LocalDate today) {
        StackPane cell = new StackPane();
        cell.setMinSize(CELL_SIZE, CELL_SIZE);
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        cell.setCursor(Cursor.HAND);

        boolean isToday   = date.equals(today);
        boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        List<SuiviTherapeutique> suivis = getSuivisForDate(date);
        boolean hasEvents = !suivis.isEmpty();

        // Cell background
        String bg     = isToday ? "#EFF6FF" : "white";
        String border = isToday ? "#BFDBFE" : (hasEvents ? "#E2E8F0" : "#F8FAFC");
        cell.setStyle(
                "-fx-background-color: " + bg + ";"
                        + "-fx-border-color: " + border + ";"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 8;"
                        + "-fx-background-radius: 8;"
        );

        VBox content = new VBox(3);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(7, 4, 5, 4));

        // Day number
        Label dayLbl = new Label(String.valueOf(day));
        if (isToday) {
            dayLbl.setStyle(
                    "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;"
                            + "-fx-background-color: #3B82F6; -fx-background-radius: 50%;"
                            + "-fx-min-width: 24; -fx-min-height: 24;"
                            + "-fx-max-width: 24; -fx-max-height: 24;"
                            + "-fx-alignment: center;"
            );
        } else {
            dayLbl.setStyle(
                    "-fx-font-size: 12px; -fx-font-weight: bold;"
                            + "-fx-text-fill: " + (isWeekend ? "#CBD5E1" : "#374151") + ";"
            );
        }
        content.getChildren().add(dayLbl);

        // Dot indicators (max 3 dots)
        if (hasEvents) {
            HBox dots = new HBox(3);
            dots.setAlignment(Pos.CENTER);
            int shown = Math.min(suivis.size(), 3);
            for (int i = 0; i < shown; i++) {
                Colors c = palette(suivis.get(i).getStatut());
                Label dot = new Label();
                dot.setStyle(
                        "-fx-background-color: " + c.dot() + ";"
                                + "-fx-min-width: 5; -fx-min-height: 5;"
                                + "-fx-max-width: 5; -fx-max-height: 5;"
                                + "-fx-background-radius: 50%;"
                );
                dots.getChildren().add(dot);
            }
            // "+N" label if overflow
            if (suivis.size() > 3) {
                Label more = new Label("+" + (suivis.size() - 3));
                more.setStyle("-fx-font-size: 8px; -fx-text-fill: #94A3B8; -fx-font-weight: bold;");
                dots.getChildren().add(more);
            }
            content.getChildren().add(dots);

            // Tooltip with summary
            Tooltip tip = new Tooltip(suivis.size() + " suivi(s) ce jour");
            tip.setStyle("-fx-font-size: 11px;");
            Tooltip.install(cell, tip);
        }

        cell.getChildren().add(content);

        // Click → show detail panel
        cell.setOnMouseClicked(e -> {
            // Deselect previous
            if (selectedCell != null) applySelectedStyle(selectedCell, false);
            selectedCell = cell;
            selectedDate = date;
            applySelectedStyle(cell, true);
            showDayDetail(date);
        });

        // Hover effect
        cell.setOnMouseEntered(e -> {
            if (!date.equals(selectedDate)) {
                cell.setStyle(cell.getStyle().replace(bg, "#F0F9FF"));
            }
        });
        cell.setOnMouseExited(e -> {
            if (!date.equals(selectedDate)) {
                cell.setStyle(cell.getStyle().replace("#F0F9FF", bg));
            }
        });

        return cell;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SELECTED CELL STYLE
    // ════════════════════════════════════════════════════════════════════════
    private void applySelectedStyle(StackPane cell, boolean selected) {
        if (selected) {
            cell.setStyle(
                    "-fx-background-color: #1E293B;"
                            + "-fx-border-color: #1E293B;"
                            + "-fx-border-width: 1;"
                            + "-fx-border-radius: 8;"
                            + "-fx-background-radius: 8;"
            );
            // Make day number white
            cell.getChildren().stream()
                    .filter(n -> n instanceof VBox)
                    .map(n -> (VBox) n)
                    .flatMap(v -> v.getChildren().stream())
                    .filter(n -> n instanceof Label)
                    .map(n -> (Label) n)
                    .findFirst()
                    .ifPresent(l -> {
                        String s = l.getStyle();
                        if (!s.contains("background-color: #3B82F6")) {
                            l.setStyle(s.replaceAll("-fx-text-fill: [^;]+;",
                                    "-fx-text-fill: white;"));
                        }
                    });
        } else {
            // Rebuild cell style from scratch based on date
            LocalDate date = selectedDate;
            boolean isToday   = date != null && date.equals(LocalDate.now());
            boolean hasEvents = date != null && !getSuivisForDate(date).isEmpty();
            String bg     = isToday ? "#EFF6FF" : "white";
            String border = isToday ? "#BFDBFE" : (hasEvents ? "#E2E8F0" : "#F8FAFC");
            cell.setStyle(
                    "-fx-background-color: " + bg + ";"
                            + "-fx-border-color: " + border + ";"
                            + "-fx-border-width: 1;"
                            + "-fx-border-radius: 8;"
                            + "-fx-background-radius: 8;"
            );
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DETAIL PANEL — shown on right when a day is clicked
    // ════════════════════════════════════════════════════════════════════════
    private void showDayDetail(LocalDate date) {
        detailCards.getChildren().clear();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
        String dayStr = fmt.format(date);
        dayStr = dayStr.substring(0,1).toUpperCase() + dayStr.substring(1);

        lblDetailDay.setText(dayStr);
        lblTodayBadge.setVisible(date.equals(LocalDate.now()));

        List<SuiviTherapeutique> suivis = getSuivisForDate(date);

        if (suivis.isEmpty()) {
            lblDetailCount.setText("Aucun suivi ce jour");
            lblSelectedInfo.setText("Aucun suivi");
            detailCards.getChildren().add(buildEmptyState());
        } else {
            lblDetailCount.setText(suivis.size() + " suivi" + (suivis.size() > 1 ? "s" : "") + " ce jour");
            lblSelectedInfo.setText(suivis.size() + " suivi" + (suivis.size() > 1 ? "s" : ""));
            for (SuiviTherapeutique s : suivis) {
                detailCards.getChildren().add(buildDetailCard(s));
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DETAIL CARD — full info for one suivi
    // ════════════════════════════════════════════════════════════════════════
    private VBox buildDetailCard(SuiviTherapeutique s) {
        Colors c = palette(s.getStatut());

        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color: white;"
                        + "-fx-background-radius: 12;"
                        + "-fx-border-color: " + c.border() + ";"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 12;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);"
        );

        // ── Card top accent bar ───────────────────────────────────────────────
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(12, 16, 12, 16));
        topBar.setStyle(
                "-fx-background-color: " + c.cardBg() + ";"
                        + "-fx-background-radius: 12 12 0 0;"
        );

        // Color dot
        Label dot = new Label();
        dot.setStyle(
                "-fx-background-color: " + c.dot() + ";"
                        + "-fx-min-width: 10; -fx-min-height: 10;"
                        + "-fx-max-width: 10; -fx-max-height: 10;"
                        + "-fx-background-radius: 50%;"
        );

        // Title
        Label typeLbl = new Label(s.getTypeSuivi());
        typeLbl.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: bold;"
                        + "-fx-text-fill: " + c.text() + ";"
        );
        HBox.setHgrow(typeLbl, Priority.ALWAYS);

        // Status badge
        Label badge = new Label(s.getStatut());
        badge.setStyle(
                "-fx-background-color: " + c.bg() + ";"
                        + "-fx-text-fill: " + c.text() + ";"
                        + "-fx-border-color: " + c.border() + ";"
                        + "-fx-border-width: 1;"
                        + "-fx-padding: 3 10;"
                        + "-fx-background-radius: 20;"
                        + "-fx-border-radius: 20;"
                        + "-fx-font-size: 10px;"
                        + "-fx-font-weight: bold;"
        );

        topBar.getChildren().addAll(dot, typeLbl, badge);
        card.getChildren().add(topBar);

        // ── Card body ─────────────────────────────────────────────────────────
        VBox body = new VBox(10);
        body.setPadding(new Insets(14, 16, 16, 16));

        // Date row
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy");
        String debut = s.getDateDebut() != null ? dateFmt.format(s.getDateDebut()) : "—";
        String fin   = s.getDateFin()   != null ? dateFmt.format(s.getDateFin())   : "—";

        HBox dateRow = new HBox(16);
        dateRow.setAlignment(Pos.CENTER_LEFT);
        dateRow.getChildren().addAll(
                infoChip("📅", "Début", debut),
                infoChip("🏁", "Fin",   fin)
        );
        body.getChildren().add(dateRow);

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-opacity: 0.5;");
        body.getChildren().add(sep);

        // Objectif
        Label objTitle = new Label("Objectif thérapeutique");
        objTitle.setStyle(
                "-fx-font-size: 10px; -fx-font-weight: bold;"
                        + "-fx-text-fill: #94A3B8; -fx-padding: 0 0 4 0;"
        );

        Label objText = new Label(s.getObjectifTherapeutique());
        objText.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151; -fx-wrap-text: true;");
        objText.setWrapText(true);
        objText.setMaxWidth(Double.MAX_VALUE);

        body.getChildren().addAll(objTitle, objText);
        card.getChildren().add(body);

        return card;
    }

    /** Small info chip: icon + label + value */
    private VBox infoChip(String icon, String label, String value) {
        VBox chip = new VBox(2);
        chip.setStyle(
                "-fx-background-color: #F8FAFC;"
                        + "-fx-background-radius: 8;"
                        + "-fx-border-color: #F1F5F9;"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 8;"
                        + "-fx-padding: 8 12;"
        );
        Label lbl = new Label(icon + "  " + label);
        lbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        chip.getChildren().addAll(lbl, val);
        return chip;
    }

    /** Empty state when no events on selected day */
    private VBox buildEmptyState() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(50));

        Label icon = new Label("📭");
        icon.setStyle("-fx-font-size: 36px;");

        Label msg = new Label("Aucun suivi prévu ce jour");
        msg.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #64748B;");

        Label hint = new Label("Sélectionnez une autre date\nou ajoutez un suivi thérapeutique.");
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-text-alignment: center;");
        hint.setWrapText(true);

        box.getChildren().addAll(icon, msg, hint);
        return box;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DATA HELPER
    // ════════════════════════════════════════════════════════════════════════
    private List<SuiviTherapeutique> getSuivisForDate(LocalDate date) {
        List<SuiviTherapeutique> result = new ArrayList<>();
        for (SuiviTherapeutique s : allSuivis) {
            if (s.getDateDebut() == null) continue;
            LocalDate debut = s.getDateDebut().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate fin = s.getDateFin() != null
                    ? s.getDateFin().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    : debut.plusMonths(1);
            if (!date.isBefore(debut) && !date.isAfter(fin)) result.add(s);
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════════════════════════════════════
    @FXML private void prevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        selectedCell = null;
        buildCalendar();
    }

    @FXML private void nextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        selectedCell = null;
        buildCalendar();
    }

    @FXML private void goToday() {
        currentMonth = YearMonth.now();
        selectedCell = null;
        buildCalendar();
        showDayDetail(LocalDate.now());
    }
}
