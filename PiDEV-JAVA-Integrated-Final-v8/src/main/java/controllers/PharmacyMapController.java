package controllers;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ResourceBundle;

public class PharmacyMapController implements Initializable {

    @FXML private WebView   webView;
    @FXML private TextField tfCity;
    @FXML private Label     lblStatus;

    private WebEngine engine;
    private boolean   mapReady = false;

    // ── Tunisian cities ──────────────────────────────────────────────────────
    private static final double TUNIS_LAT    = 36.8065, TUNIS_LNG    = 10.1815;
    private static final double SFAX_LAT     = 34.7406, SFAX_LNG     = 10.7603;
    private static final double SOUSSE_LAT   = 35.8256, SOUSSE_LNG   = 10.6369;
    private static final double MONASTIR_LAT = 35.7643, MONASTIR_LNG = 10.8113;

    // ── Overpass servers ─────────────────────────────────────────────────────
    private static final String[] SERVERS = {
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
            "https://overpass.openstreetmap.ru/api/interpreter"
    };

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        engine = webView.getEngine();
        engine.setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/124.0.0.0 Safari/537.36"
        );
        engine.setJavaScriptEnabled(true);

        // Wait for map to finish loading, then load Tunis pharmacies
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                mapReady = true;
                System.out.println("Map HTML loaded ✓");
                // Fetch Tunis data from Java side
                fetchAndInject(TUNIS_LAT, TUNIS_LNG, "Tunis");
            } else if (newState == Worker.State.FAILED) {
                System.err.println("Map HTML failed to load");
            }
        });

        URL mapUrl = getClass().getResource("/map.html");
        if (mapUrl != null) {
            engine.load(mapUrl.toExternalForm());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  QUICK CITY BUTTONS
    // ════════════════════════════════════════════════════════════════════════
    @FXML private void searchTunis()    { fetchAndInject(TUNIS_LAT,    TUNIS_LNG,    "Tunis"); }
    @FXML private void searchSfax()     { fetchAndInject(SFAX_LAT,     SFAX_LNG,     "Sfax"); }
    @FXML private void searchSousse()   { fetchAndInject(SOUSSE_LAT,   SOUSSE_LNG,   "Sousse"); }
    @FXML private void searchMonastir() { fetchAndInject(MONASTIR_LAT, MONASTIR_LNG, "Monastir"); }

    // ════════════════════════════════════════════════════════════════════════
    //  CITY SEARCH via Nominatim
    // ════════════════════════════════════════════════════════════════════════
    @FXML
    private void searchCity() {
        String city = tfCity.getText().trim();
        if (city.isEmpty()) return;
        lblStatus.setText("🔍 Recherche de " + city + "...");

        new Thread(() -> {
            try {
                String encoded = URLEncoder.encode(city + ", Tunisia",
                        StandardCharsets.UTF_8);
                String apiUrl = "https://nominatim.openstreetmap.org/search"
                        + "?q=" + encoded + "&format=json&limit=1";

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .timeout(Duration.ofSeconds(10))
                        .header("User-Agent", "GestionTherapeutique/1.0 (student project)")
                        .GET().build();

                HttpResponse<String> resp =
                        http.send(req, HttpResponse.BodyHandlers.ofString());

                String body = resp.body();
                if (body.contains("\"lat\"")) {
                    double lat = parseDouble(body, "lat");
                    double lon = parseDouble(body, "lon");
                    Platform.runLater(() -> tfCity.clear());
                    fetchAndInject(lat, lon, city);
                } else {
                    Platform.runLater(() ->
                            lblStatus.setText("❌ Ville introuvable : " + city));
                }
            } catch (Exception e) {
                Platform.runLater(() -> lblStatus.setText("❌ Erreur réseau"));
                System.err.println("Geocoding error: " + e.getMessage());
            }
        }).start();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CORE: Java fetches Overpass → injects JSON into WebView
    //  This bypasses WebView network restrictions entirely
    // ════════════════════════════════════════════════════════════════════════
    private void fetchAndInject(double lat, double lng, String cityName) {
        if (!mapReady) return;

        // Update UI immediately
        Platform.runLater(() -> {
            lblStatus.setText("🔍 " + cityName + " — chargement...");
            engine.executeScript("showLoader(); setInfoBar('Chargement...', false);");
            // Center map on location
            engine.executeScript(
                    "map.setView([" + lat + "," + lng + "], 14);"
            );
        });

        new Thread(() -> {
            String json = fetchOverpass(lat, lng);

            if (json != null) {
                // Escape the JSON for safe injection into JS
                String escaped = json
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\n", "")
                        .replace("\r", "");

                Platform.runLater(() -> {
                    try {
                        // Parse JSON in JS and build markers
                        engine.executeScript(
                                "var data = JSON.parse('" + escaped + "');" +
                                        "buildMarkers(data.elements || []);" +
                                        "hideLoader();"
                        );
                        lblStatus.setText("📍 " + cityName);
                    } catch (Exception e) {
                        System.err.println("JS injection error: " + e.getMessage());
                        lblStatus.setText("❌ Erreur d'affichage");
                        engine.executeScript("hideLoader(); showError();");
                    }
                });
            } else {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Serveur indisponible — réessayez");
                    engine.executeScript("hideLoader(); showError();");
                });
            }
        }).start();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HTTP: Try all Overpass servers until one responds
    // ════════════════════════════════════════════════════════════════════════
    private String fetchOverpass(double lat, double lng) {
        String query = "[out:json][timeout:25];"
                + "("
                + "node[\"amenity\"=\"pharmacy\"]"
                + "(around:5000," + lat + "," + lng + ");"
                + "way[\"amenity\"=\"pharmacy\"]"
                + "(around:5000," + lat + "," + lng + ");"
                + ");out center;";

        String encoded;
        try {
            encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }

        for (String server : SERVERS) {
            try {
                System.out.println("Trying Overpass server: " + server);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(server + "?data=" + encoded))
                        .timeout(Duration.ofSeconds(20))
                        .header("User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                                        + "AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36")
                        .header("Accept", "application/json")
                        .header("Referer", "https://www.openstreetmap.org/")
                        .GET().build();

                HttpResponse<String> resp =
                        http.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 200) {
                    String body = resp.body();
                    if (body.contains("\"elements\"")) {
                        System.out.println("✓ Got response from: " + server);
                        return body;
                    }
                } else if (resp.statusCode() == 429) {
                    System.out.println("Rate limited by " + server + " — trying next...");
                    Thread.sleep(1000);
                } else {
                    System.out.println("HTTP " + resp.statusCode() + " from " + server);
                }
            } catch (Exception e) {
                System.out.println("Failed: " + server + " — " + e.getMessage());
            }
        }

        System.err.println("All Overpass servers failed.");
        return null;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  JSON helper
    // ════════════════════════════════════════════════════════════════════════
    private double parseDouble(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx < 0) {
            search = "\"" + key + "\":";
            idx = json.indexOf(search);
            if (idx < 0) return 0;
            idx += search.length();
        } else {
            idx += search.length();
        }
        int end = json.indexOf("\"", idx);
        if (end < 0) end = json.indexOf(",", idx);
        if (end < 0) end = json.indexOf("}", idx);
        return Double.parseDouble(
                json.substring(idx, end).replace("\"", "").trim());
    }
}
