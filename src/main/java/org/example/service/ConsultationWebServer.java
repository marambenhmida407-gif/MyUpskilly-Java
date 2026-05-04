package org.example.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.example.model.Consultation;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class ConsultationWebServer {

    private HttpServer server;
    private final ConsultationService consultationService = new ConsultationService();
    private static ConsultationWebServer instance;
    private String localIp;

    public static ConsultationWebServer getInstance() {
        if (instance == null) instance = new ConsultationWebServer();
        return instance;
    }

    public void start() {
        try {
            localIp = InetAddress.getLocalHost().getHostAddress();
            server = HttpServer.create(new InetSocketAddress(8085), 0);
            server.createContext("/consultation/", this::handleConsultation);
            server.createContext("/payment/success/", this::handlePaymentSuccess);
            server.setExecutor(null);
            server.start();
            System.out.println("Serveur web démarré sur http://" + localIp + ":8085");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getBaseUrl() {
        return "http://" + localIp + ":8085";
    }

    private void handleConsultation(HttpExchange exchange) {
        try {
            String path = exchange.getRequestURI().getPath();
            int id = Integer.parseInt(path.replace("/consultation/", ""));
            Consultation c = consultationService.getById(id);

            String html;
            if (c == null) {
                html = buildErrorPage("Consultation introuvable");
            } else {
                html = buildConsultationPage(c);
            }

            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handlePaymentSuccess(HttpExchange exchange) {
        try {
            String path = exchange.getRequestURI().getPath();
            int id = Integer.parseInt(path.replace("/payment/success/", ""));
            Consultation c = consultationService.getById(id);

            if (c != null) {
                c.setStatut("terminee");
                consultationService.update(c);
            }

            String html = buildPaymentSuccessPage(c);
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildConsultationPage(Consultation c) {
        return "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<title>Consultation #" + c.getId() + "</title>"
                + "<style>"
                + "* { margin:0; padding:0; box-sizing:border-box; }"
                + "body { font-family:-apple-system,'Segoe UI',sans-serif; background:#f0f4f8; padding:20px; }"
                + ".card { background:white; border-radius:16px; max-width:600px; margin:0 auto; overflow:hidden; box-shadow:0 4px 20px rgba(0,0,0,0.1); }"
                + ".header { background:linear-gradient(135deg,#0f2b46,#1a3a5c); padding:24px; color:white; }"
                + ".header h1 { font-size:22px; margin-bottom:4px; }"
                + ".header p { opacity:0.8; font-size:13px; }"
                + ".body { padding:24px; }"
                + ".row { display:flex; justify-content:space-between; padding:14px 0; border-bottom:1px solid #f1f5f9; }"
                + ".row:last-child { border:none; }"
                + ".label { font-weight:600; color:#334155; font-size:14px; }"
                + ".value { color:#475569; font-size:14px; text-align:right; max-width:60%; }"
                + ".badge { display:inline-block; padding:4px 14px; border-radius:20px; font-size:12px; font-weight:600; }"
                + ".badge-en_cours { background:#dbeafe; color:#1d4ed8; }"
                + ".badge-terminee { background:#d1fae5; color:#047857; }"
                + ".badge-annulee { background:#fee2e2; color:#b91c1c; }"
                + ".badge-programmee { background:#fef3c7; color:#92400e; }"
                + ".section { margin-top:20px; }"
                + ".section h3 { font-size:15px; color:#0f2b46; margin-bottom:10px; padding-left:12px; border-left:3px solid #3b82f6; }"
                + ".section p { color:#475569; font-size:14px; padding:12px; background:#f8fafc; border-radius:8px; line-height:1.6; }"
                + ".footer { text-align:center; padding:16px; color:#94a3b8; font-size:12px; }"
                + ".logo { font-size:18px; font-weight:800; color:#60a5fa; letter-spacing:2px; }"
                + "</style></head><body>"
                + "<div class='card'>"
                + "<div class='header'>"
                + "<span class='logo'>SANTER</span>"
                + "<h1>Consultation #" + c.getId() + "</h1>"
                + "<p>Détails de la consultation médicale</p>"
                + "</div>"
                + "<div class='body'>"
                + "<div class='row'><span class='label'>Patient</span><span class='value'>" + (c.getPatientNom() != null ? c.getPatientNom() : "#" + c.getPatientId()) + "</span></div>"
                + "<div class='row'><span class='label'>Date</span><span class='value'>" + (c.getDateConsultation() != null ? c.getDateConsultation().toString().replace("T", " à ") : "—") + "</span></div>"
                + "<div class='row'><span class='label'>Statut</span><span class='value'><span class='badge badge-" + c.getStatut() + "'>" + c.getStatut() + "</span></span></div>"
                + "<div class='row'><span class='label'>Pathologie</span><span class='value'>" + (c.getPathologieNom() != null ? c.getPathologieNom() : "Aucune") + "</span></div>"
                + "<div class='section'><h3>Motif</h3><p>" + (c.getMotif() != null ? c.getMotif() : "—") + "</p></div>"
                + "<div class='section'><h3>Diagnostic</h3><p>" + (c.getDiagnostic() != null ? c.getDiagnostic() : "—") + "</p></div>"
                + "<div class='section'><h3>Observations</h3><p>" + (c.getObservations() != null && !c.getObservations().isEmpty() ? c.getObservations() : "Aucune") + "</p></div>"
                + "<div class='section'><h3>Ordonnance</h3><p>" + (c.getOrdonnance() != null && !c.getOrdonnance().isEmpty() ? c.getOrdonnance() : "Aucune") + "</p></div>"
                + "</div>"
                + "<div class='footer'>Santer Health System — MyUpskilly</div>"
                + "</div></body></html>";
    }

    private String buildPaymentSuccessPage(Consultation c) {
        return "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<title>Paiement réussi</title>"
                + "<style>"
                + "* { margin:0; padding:0; box-sizing:border-box; }"
                + "body { font-family:-apple-system,'Segoe UI',sans-serif; background:#f0f4f8; display:flex; justify-content:center; align-items:center; min-height:100vh; }"
                + ".card { background:white; border-radius:16px; max-width:500px; width:90%; overflow:hidden; box-shadow:0 4px 20px rgba(0,0,0,0.1); text-align:center; }"
                + ".header { background:linear-gradient(135deg,#059669,#047857); padding:40px 24px; color:white; }"
                + ".check { font-size:60px; margin-bottom:12px; }"
                + ".header h1 { font-size:24px; }"
                + ".header p { opacity:0.9; margin-top:8px; font-size:14px; }"
                + ".body { padding:30px; }"
                + ".amount { font-size:36px; font-weight:800; color:#059669; margin:16px 0; }"
                + ".info { color:#475569; font-size:14px; margin-bottom:8px; }"
                + ".info strong { color:#0f2b46; }"
                + ".badge { display:inline-block; padding:6px 18px; border-radius:20px; font-size:13px; font-weight:600; background:#d1fae5; color:#047857; margin-top:16px; }"
                + ".footer { padding:16px; color:#94a3b8; font-size:12px; border-top:1px solid #f1f5f9; }"
                + "</style></head><body>"
                + "<div class='card'>"
                + "<div class='header'>"
                + "<div class='check'>&#10003;</div>"
                + "<h1>Paiement réussi !</h1>"
                + "<p>Votre paiement a été traité avec succès</p>"
                + "</div>"
                + "<div class='body'>"
                + (c != null ? (
                "<div class='amount'>" + String.format("%.2f", calculateAmount(c) / 100.0) + " €</div>"
                + "<div class='info'><strong>Consultation #" + c.getId() + "</strong></div>"
                + "<div class='info'>Patient: " + (c.getPatientNom() != null ? c.getPatientNom() : "N/A") + "</div>"
                + "<div class='info'>Motif: " + (c.getMotif() != null ? c.getMotif() : "N/A") + "</div>"
                + "<div class='badge'>Statut: Terminée ✓</div>"
        ) : "<p>Consultation introuvable</p>")
                + "</div>"
                + "<div class='footer'>Santer Health System — MyUpskilly</div>"
                + "</div></body></html>";
    }

    private String buildErrorPage(String message) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<title>Erreur</title>"
                + "<style>body{font-family:sans-serif;display:flex;justify-content:center;align-items:center;height:100vh;background:#f0f4f8;}"
                + ".card{background:white;padding:40px;border-radius:16px;text-align:center;box-shadow:0 4px 20px rgba(0,0,0,0.1);}"
                + "h1{color:#ef4444;margin-bottom:10px;}p{color:#64748b;}</style></head>"
                + "<body><div class='card'><h1>Erreur</h1><p>" + message + "</p></div></body></html>";
    }

    private long calculateAmount(Consultation c) {
        if (c.getPathologieNom() != null) {
            String path = c.getPathologieNom().toLowerCase();
            if (path.contains("cancer") || path.contains("hiv")) return 15000;
            if (path.contains("diabete")) return 10000;
        }
        return 5000;
    }

    public void stop() {
        if (server != null) server.stop(0);
    }
}