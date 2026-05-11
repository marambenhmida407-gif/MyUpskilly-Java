package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import models.StockMedicament;
import services.ServiceStock;
import services.ServiceStock.CheckResult;
import services.ServiceStock.RxResult;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;

public class StockCheckerController implements Initializable {

    @FXML private TextField        tfSearch;
    @FXML private Button           btnSearch;
    @FXML private VBox             resultsBox;
    @FXML private Label            lblStatus;
    @FXML private ProgressIndicator spinner;

    private final ServiceStock service = new ServiceStock();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        spinner.setVisible(false);
        lblStatus.setText("Recherchez un médicament pour vérifier son stock.");
        tfSearch.setOnAction(e -> handleSearch());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SEARCH
    // ════════════════════════════════════════════════════════════════════════

    @FXML
    private void handleSearch() {
        String query = tfSearch.getText().trim();
        if (query.isEmpty()) {
            lblStatus.setText("⚠️  Veuillez entrer un nom de médicament.");
            return;
        }

        setLoading(true);
        resultsBox.getChildren().clear();
        lblStatus.setText("Vérification en cours — stock local + RxNorm...");

        Thread t = new Thread(() -> {
            try {
                CheckResult result = service.fullCheck(query);
                Platform.runLater(() -> displayResults(query, result));
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    lblStatus.setText("❌  Erreur base de données : " + e.getMessage());
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DISPLAY
    // ════════════════════════════════════════════════════════════════════════

    private void displayResults(String query, CheckResult result) {
        setLoading(false);
        resultsBox.getChildren().clear();

        // RxNorm verification card always shown first
        resultsBox.getChildren().add(buildRxCard(query, result.rxResult));

        List<StockMedicament> matches = result.localMatches;
        if (matches.isEmpty()) {
            lblStatus.setText("Aucun résultat local pour « " + query + " »");
            resultsBox.getChildren().add(buildEmptyCard(query));
        } else {
            lblStatus.setText(matches.size() + " résultat(s) dans le stock local");
            for (StockMedicament med : matches) {
                resultsBox.getChildren().add(buildStockCard(med));
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CARD BUILDERS
    // ════════════════════════════════════════════════════════════════════════

    /** RxNorm verification card */
    private VBox buildRxCard(String query, RxResult rx) {
        VBox card = baseCard();
        String borderColor = rx.found ? "#3B82F6" : "#E2E8F0";
        card.setStyle(card.getStyle()
                + "-fx-border-color: " + borderColor + ";"
                + "-fx-border-width: 0 0 0 4; -fx-border-radius: 0 10 10 0;");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(rx.found ? "🔬" : "❓");
        icon.setStyle("-fx-font-size: 22px;");

        VBox titleBox = new VBox(3);
        Label title = new Label("Vérification RxNorm — " + query);
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        Label sub = new Label(rx.found
                ? "Médicament reconnu · Base internationale NLM / RxNorm"
                : "Non trouvé dans RxNorm — médicament local ou nom alternatif");
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        titleBox.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label(rx.found ? "✓  Reconnu" : "~  Non trouvé");
        badge.setStyle(
                "-fx-background-color: " + (rx.found ? "#DBEAFE" : "#F1F5F9") + ";"
                        + "-fx-text-fill: "       + (rx.found ? "#1E40AF" : "#64748B") + ";"
                        + "-fx-padding: 5 14; -fx-background-radius: 20;"
                        + "-fx-font-size: 11px; -fx-font-weight: bold;");

        header.getChildren().addAll(icon, titleBox, spacer, badge);
        card.getChildren().add(header);

        // Details (only when found)
        if (rx.found) {
            card.getChildren().add(new Separator());

            GridPane grid = new GridPane();
            grid.setHgap(20);
            grid.setVgap(6);
            int r = 0;
            if (!rx.name.isEmpty())
                addGridRow(grid, r++, "Nom officiel :", rx.name);
            if (!rx.rxcui.isEmpty())
                addGridRow(grid, r++, "RxCUI :", rx.rxcui);
            if (!rx.getTtyLabel().isEmpty())
                addGridRow(grid, r++, "Type :", rx.getTtyLabel());
            if (!rx.synonym.isEmpty())
                addGridRow(grid, r++, "Synonyme :", rx.synonym);

            card.getChildren().add(grid);

            // Hint label
            Label hint = new Label("💡  Essayez aussi le nom générique pour plus de résultats"
                    + " (ex: « paracetamol » pour Doliprane)");
            hint.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8; -fx-padding: 4 0 0 0;");
            hint.setWrapText(true);
            card.getChildren().add(hint);
        } else {
            // Suggestions when not found
            Label hint = new Label(
                    "💡  Essayez le nom générique : paracetamol, amoxicillin, ibuprofen, diclofenac, albuterol...");
            hint.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8; -fx-padding: 4 0 0 0;");
            hint.setWrapText(true);
            card.getChildren().add(hint);
        }

        return card;
    }

    /** Stock item card — green / orange / red */
    private VBox buildStockCard(StockMedicament med) {
        VBox card = baseCard();

        String accentColor, bgBadge, textBadge, badgeText;
        if (med.isOutOfStock()) {
            accentColor = "#EF4444"; bgBadge = "#FEE2E2";
            textBadge   = "#991B1B"; badgeText = "⛔  Rupture de stock";
        } else if (med.isLowStock()) {
            accentColor = "#F59E0B"; bgBadge = "#FEF3C7";
            textBadge   = "#92400E"; badgeText = "⚠️  Stock faible";
        } else {
            accentColor = "#10B981"; bgBadge = "#D1FAE5";
            textBadge   = "#065F46"; badgeText = "✅  Disponible";
        }

        card.setStyle(card.getStyle()
                + "-fx-border-color: " + accentColor + ";"
                + "-fx-border-width: 0 0 0 4; -fx-border-radius: 0 10 10 0;");

        // Top row
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox nameBox = new VBox(3);
        Label nomLbl = new Label(med.getNom());
        nomLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        Label formeLbl = new Label(med.getForme()
                + (med.getDosage().isEmpty() ? "" : "  ·  " + med.getDosage()));
        formeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        nameBox.getChildren().addAll(nomLbl, formeLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label(badgeText);
        badge.setStyle(
                "-fx-background-color: " + bgBadge + ";"
                        + "-fx-text-fill: " + textBadge + ";"
                        + "-fx-padding: 5 14; -fx-background-radius: 20;"
                        + "-fx-font-size: 11px; -fx-font-weight: bold;");

        topRow.getChildren().addAll(nameBox, spacer, badge);
        card.getChildren().add(topRow);
        card.getChildren().add(new Separator());

        // Details row
        HBox detailsRow = new HBox(24);
        detailsRow.setAlignment(Pos.CENTER_LEFT);

        // Big quantity number
        VBox qtyBox = new VBox(2);
        qtyBox.setAlignment(Pos.CENTER);
        Label qtyNum = new Label(String.valueOf(med.getQuantite()));
        qtyNum.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;"
                + "-fx-text-fill: " + accentColor + ";");
        Label qtyLbl = new Label(med.getUnite() + "s en stock");
        qtyLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");
        qtyBox.getChildren().addAll(qtyNum, qtyLbl);

        Separator vSep = new Separator();
        vSep.setOrientation(javafx.geometry.Orientation.VERTICAL);
        vSep.setPrefHeight(52);

        // Info grid
        GridPane info = new GridPane();
        info.setHgap(16);
        info.setVgap(5);
        int r = 0;
        addGridRow(info, r++, "Seuil alerte :",
                med.getSeuilAlerte() + " " + med.getUnite() + "s");
        if (med.getPrix() > 0)
            addGridRow(info, r++, "Prix unitaire :",
                    String.format("%.3f TND", med.getPrix()));
        if (med.getFournisseur() != null && !med.getFournisseur().isEmpty())
            addGridRow(info, r++, "Fournisseur :", med.getFournisseur());
        if (med.getDateExpiry() != null)
            addGridRow(info, r++, "Expiration :",
                    new SimpleDateFormat("MM/yyyy").format(med.getDateExpiry()));

        detailsRow.getChildren().addAll(qtyBox, vSep, info);
        card.getChildren().add(detailsRow);

        return card;
    }

    /** Empty state card */
    private VBox buildEmptyCard(String query) {
        VBox card = baseCard();
        card.setAlignment(Pos.CENTER);
        card.setSpacing(8);

        Label icon = new Label("📦");
        icon.setStyle("-fx-font-size: 30px;");
        Label msg = new Label("Aucun médicament « " + query + " » dans le stock local.");
        msg.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        msg.setWrapText(true);
        Label hint = new Label("Vérifiez l'orthographe ou ajoutez-le dans la base de données.");
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        card.getChildren().addAll(icon, msg, hint);
        return card;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════════════

    private VBox baseCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color: white;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-radius: 10;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 2);");
        return card;
    }

    private void addGridRow(GridPane grid, int row, String label, String value) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-font-weight: bold;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 12px; -fx-text-fill: #1E293B; -fx-font-weight: bold;");
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }

    private void setLoading(boolean loading) {
        spinner.setVisible(loading);
        btnSearch.setDisable(loading);
        tfSearch.setDisable(loading);
    }
}
