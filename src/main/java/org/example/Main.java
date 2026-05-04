package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.service.ConsultationWebServer;

public class Main extends Application {

    private BorderPane root;
    private Button btnConsultations, btnPathologies, btnCalendrier;

    @Override
    public void start(Stage stage) throws Exception {
        // Démarrer le serveur web pour QR Code et Stripe
        ConsultationWebServer.getInstance().start();

        root = new BorderPane();

        VBox sidebar = new VBox();
        sidebar.setPrefWidth(230);
        sidebar.getStyleClass().add("sidebar");

        VBox logoBox = new VBox(4);
        logoBox.setPadding(new Insets(24, 20, 24, 20));
        logoBox.getStyleClass().add("sidebar-logo");
        Label lblLogo = new Label("SANTER");
        lblLogo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #60a5fa; -fx-letter-spacing: 2;");
        Label lblSub = new Label("Health System");
        lblSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #8ba3be;");
        logoBox.getChildren().addAll(lblLogo, lblSub);

        VBox menuBox = new VBox(0);
        menuBox.setPadding(new Insets(12, 0, 0, 0));

        btnConsultations = createMenuButton("Consultations", true);
        btnPathologies = createMenuButton("Pathologies", false);
        btnCalendrier = createMenuButton("Calendrier", false);

        btnConsultations.setOnAction(e -> { setActive(btnConsultations); loadPage("/consultation.fxml"); });
        btnPathologies.setOnAction(e -> { setActive(btnPathologies); loadPage("/pathologie.fxml"); });
        btnCalendrier.setOnAction(e -> { setActive(btnCalendrier); loadPage("/calendrier.fxml"); });

        menuBox.getChildren().addAll(btnConsultations, btnPathologies, btnCalendrier);
        sidebar.getChildren().addAll(logoBox, menuBox);

        root.setLeft(sidebar);
        loadPage("/consultation.fxml");

        Scene scene = new Scene(root, 1250, 800);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setTitle("Santer Health System - MyUpskilly");
        stage.setScene(scene);
        stage.show();
    }

    private Button createMenuButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.getStyleClass().add("sidebar-btn");
        if (active) btn.getStyleClass().add("sidebar-btn-active");
        return btn;
    }

    private void setActive(Button active) {
        btnConsultations.getStyleClass().remove("sidebar-btn-active");
        btnPathologies.getStyleClass().remove("sidebar-btn-active");
        btnCalendrier.getStyleClass().remove("sidebar-btn-active");
        active.getStyleClass().add("sidebar-btn-active");
    }

    private void loadPage(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            javafx.scene.Node page = loader.load();
            root.setCenter(page);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        ConsultationWebServer.getInstance().stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}