package services;

import models.StockMedicament;
import utils.MyDatabase;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceStock {

    // ════════════════════════════════════════════════════════════════════════
    //  LOCAL DB — CRUD  (unchanged)
    // ════════════════════════════════════════════════════════════════════════

    public List<StockMedicament> afficherAll() throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        List<StockMedicament> list = new ArrayList<>();
        String sql = "SELECT * FROM stock_medicament ORDER BY nom";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<StockMedicament> searchByName(String query) throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        List<StockMedicament> list = new ArrayList<>();
        String sql = "SELECT * FROM stock_medicament WHERE LOWER(nom) LIKE ? ORDER BY nom";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + query.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public void ajouter(StockMedicament s) throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        String sql = "INSERT INTO stock_medicament "
                + "(nom, forme, dosage, quantite, unite, seuil_alerte, prix, fournisseur, date_expiry) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getNom());
            ps.setString(2, s.getForme());
            ps.setString(3, s.getDosage());
            ps.setInt   (4, s.getQuantite());
            ps.setString(5, s.getUnite());
            ps.setInt   (6, s.getSeuilAlerte());
            ps.setDouble(7, s.getPrix());
            ps.setString(8, s.getFournisseur());
            ps.setDate  (9, s.getDateExpiry() != null
                    ? new java.sql.Date(s.getDateExpiry().getTime()) : null);
            ps.executeUpdate();
        }
    }

    public void updateQuantite(int id, int newQty) throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        String sql = "UPDATE stock_medicament SET quantite = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newQty);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void supprimer(int id) throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        String sql = "DELETE FROM stock_medicament WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RXNORM API — International Drug Verification
    //  Free · No API key · Works with French/Arabic/Generic names
    //  Docs: https://rxnav.nlm.nih.gov/RxNormAPIs.html
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Result returned by RxNorm check.
     * Replaces the old FdaResult — field names kept similar so controller
     * needs only small updates.
     */
    public static class RxResult {
        public final boolean found;
        public final String  rxcui;          // RxNorm unique concept ID
        public final String  name;           // official drug name
        public final String  synonym;        // matched synonym / brand name
        public final String  tty;            // term type: IN, BN, SCD, SBD…
        public final String  language;       // language of synonym

        public RxResult(boolean found, String rxcui, String name,
                        String synonym, String tty, String language) {
            this.found    = found;
            this.rxcui    = rxcui;
            this.name     = name;
            this.synonym  = synonym;
            this.tty      = tty;
            this.language = language;
        }

        public static RxResult notFound() {
            return new RxResult(false, "", "", "", "", "");
        }

        /** Human-readable term type label */
        public String getTtyLabel() {
            return switch (tty) {
                case "IN"  -> "Ingrédient générique";
                case "BN"  -> "Nom commercial";
                case "PIN" -> "Ingrédient précis";
                case "SCD" -> "Médicament (générique)";
                case "SBD" -> "Médicament (marque)";
                case "MIN" -> "Ingrédient multiple";
                default    -> tty.isEmpty() ? "Médicament" : tty;
            };
        }
    }

    /**
     * Verify a drug name using the RxNorm REST API.
     * Strategy:
     *   1. Approximate match search  → getApproximateMatch
     *   2. If found, fetch full concept details via rxcui
     *   3. Also try getDrugs for brand/generic resolution
     */
    public RxResult verifyWithRxNorm(String drugName) {
        try {
            // ── Step 1: approximate match (handles typos, French names) ──────
            String encoded = URLEncoder.encode(drugName, StandardCharsets.UTF_8);

            // Try getDrugs first (exact + brand names)
            RxResult result = queryRxDrugs(encoded, drugName);
            if (result.found) return result;

            // Fallback: approximate match (handles misspellings)
            return queryRxApproximate(encoded, drugName);

        } catch (Exception e) {
            System.err.println("RxNorm API error: " + e.getMessage());
            return RxResult.notFound();
        }
    }

    /**
     * RxNorm getDrugs — finds exact name, brand names, synonyms
     * GET https://rxnav.nlm.nih.gov/REST/drugs.json?name=paracetamol
     */
    private RxResult queryRxDrugs(String encoded, String originalName) {
        try {
            String urlStr = "https://rxnav.nlm.nih.gov/REST/drugs.json?name=" + encoded;
            String json   = httpGet(urlStr);
            if (json == null || json.contains("\"drugGroup\":{}")
                    || !json.contains("\"rxcui\"")) {
                return RxResult.notFound();
            }

            // Extract first rxcui and name found
            String rxcui  = extractJsonField(json, "rxcui");
            String name   = extractJsonField(json, "name");
            String tty    = extractJsonField(json, "tty");
            String synonym = extractJsonField(json, "synonym");

            boolean found = !rxcui.isEmpty();
            return new RxResult(found, rxcui, name, synonym, tty, "");

        } catch (Exception e) {
            return RxResult.notFound();
        }
    }

    /**
     * RxNorm approximateMatch — works even with typos and partial names
     * GET https://rxnav.nlm.nih.gov/REST/approximateTerm.json?term=doliprane&maxEntries=1
     */
    private RxResult queryRxApproximate(String encoded, String originalName) {
        try {
            String urlStr = "https://rxnav.nlm.nih.gov/REST/approximateTerm.json?term="
                    + encoded + "&maxEntries=1";
            String json = httpGet(urlStr);
            if (json == null || !json.contains("\"rxcui\"")) {
                return RxResult.notFound();
            }

            String rxcui = extractJsonField(json, "rxcui");
            String name  = extractJsonField(json, "name");
            String score = extractJsonField(json, "score");

            // Only accept high-confidence matches (score >= 70 out of 100)
            if (rxcui.isEmpty()) return RxResult.notFound();
            try {
                int scoreInt = Integer.parseInt(score);
                if (scoreInt < 70) return RxResult.notFound();
            } catch (NumberFormatException ignored) {}

            // Fetch full details using the rxcui
            return queryRxProperties(rxcui);

        } catch (Exception e) {
            return RxResult.notFound();
        }
    }

    /**
     * Fetch full drug properties from rxcui
     * GET https://rxnav.nlm.nih.gov/REST/rxcui/{rxcui}/properties.json
     */
    private RxResult queryRxProperties(String rxcui) {
        try {
            String urlStr = "https://rxnav.nlm.nih.gov/REST/rxcui/" + rxcui + "/properties.json";
            String json   = httpGet(urlStr);
            if (json == null) return RxResult.notFound();

            String name     = extractJsonField(json, "name");
            String tty      = extractJsonField(json, "tty");
            String language = extractJsonField(json, "language");

            return new RxResult(true, rxcui, name, "", tty, language);

        } catch (Exception e) {
            return RxResult.notFound();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  COMBINED CHECK — used by StockCheckerController
    // ════════════════════════════════════════════════════════════════════════

    public static class CheckResult {
        public final List<StockMedicament> localMatches;
        public final RxResult              rxResult;

        public CheckResult(List<StockMedicament> localMatches, RxResult rxResult) {
            this.localMatches = localMatches;
            this.rxResult     = rxResult;
        }
    }

    /** Full check: local DB search + RxNorm verify */
    public CheckResult fullCheck(String drugName) throws SQLException {
        List<StockMedicament> local = searchByName(drugName);
        RxResult rx = verifyWithRxNorm(drugName);
        return new CheckResult(local, rx);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HTTP HELPER
    // ════════════════════════════════════════════════════════════════════════

    private String httpGet(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.setConnectTimeout(7000);
            c.setReadTimeout(7000);
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("User-Agent",
                    "GestionTherapeutique/1.0 (student project)");

            if (c.getResponseCode() != 200) return null;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(c.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            return sb.toString();

        } catch (Exception e) {
            System.err.println("HTTP error for " + urlStr + ": " + e.getMessage());
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  JSON HELPER  (no external library needed)
    // ════════════════════════════════════════════════════════════════════════

    /** Extract first occurrence of "fieldName":"value" from JSON string */
    private String extractJsonField(String json, String fieldName) {
        try {
            String key = "\"" + fieldName + "\":\"";
            int start  = json.indexOf(key);
            if (start < 0) return "";
            start += key.length();
            int end = json.indexOf("\"", start);
            if (end < 0) return "";
            return json.substring(start, end).trim();
        } catch (Exception e) {
            return "";
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DB ROW MAPPER
    // ════════════════════════════════════════════════════════════════════════

    private StockMedicament map(ResultSet rs) throws SQLException {
        StockMedicament s = new StockMedicament();
        s.setId         (rs.getInt   ("id"));
        s.setNom        (rs.getString("nom"));
        s.setForme      (rs.getString("forme"));
        s.setDosage     (rs.getString("dosage"));
        s.setQuantite   (rs.getInt   ("quantite"));
        s.setUnite      (rs.getString("unite"));
        s.setSeuilAlerte(rs.getInt   ("seuil_alerte"));
        s.setPrix       (rs.getDouble("prix"));
        s.setFournisseur(rs.getString("fournisseur"));
        Date exp = rs.getDate("date_expiry");
        if (exp != null) s.setDateExpiry(new java.util.Date(exp.getTime()));
        return s;
    }
}
