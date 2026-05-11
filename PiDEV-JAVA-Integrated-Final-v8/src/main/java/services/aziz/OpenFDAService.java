package services.aziz;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenFDAService {

    private static final String BASE_URL = "https://api.fda.gov/drug/label.json";
    private final HttpClient client = HttpClient.newHttpClient();

    private static final Map<String, String> TRADUCTIONS = new HashMap<>();
    static {
        TRADUCTIONS.put("cancer", "cancer");
        TRADUCTIONS.put("diabète", "diabetes");
        TRADUCTIONS.put("diabete", "diabetes");
        TRADUCTIONS.put("hypertension", "hypertension");
        TRADUCTIONS.put("grippe", "influenza");
        TRADUCTIONS.put("asthme", "asthma");
        TRADUCTIONS.put("bronchite", "bronchitis");
        TRADUCTIONS.put("pneumonie", "pneumonia");
        TRADUCTIONS.put("allergie", "allergy");
        TRADUCTIONS.put("infection", "infection");
        TRADUCTIONS.put("inflammation", "inflammation");
        TRADUCTIONS.put("douleur", "pain");
        TRADUCTIONS.put("fièvre", "fever");
        TRADUCTIONS.put("fievre", "fever");
        TRADUCTIONS.put("migraine", "migraine");
        TRADUCTIONS.put("arthrite", "arthritis");
        TRADUCTIONS.put("anémie", "anemia");
        TRADUCTIONS.put("anemie", "anemia");
        TRADUCTIONS.put("dépression", "depression");
        TRADUCTIONS.put("depression", "depression");
        TRADUCTIONS.put("anxiété", "anxiety");
        TRADUCTIONS.put("anxiete", "anxiety");
        TRADUCTIONS.put("insomnie", "insomnia");
        TRADUCTIONS.put("épilepsie", "epilepsy");
        TRADUCTIONS.put("epilepsie", "epilepsy");
        TRADUCTIONS.put("tuberculose", "tuberculosis");
        TRADUCTIONS.put("hépatite", "hepatitis");
        TRADUCTIONS.put("hepatite", "hepatitis");
        TRADUCTIONS.put("hiv", "hiv");
        TRADUCTIONS.put("sida", "hiv");
        TRADUCTIONS.put("cholestérol", "cholesterol");
        TRADUCTIONS.put("cholesterol", "cholesterol");
        TRADUCTIONS.put("acl", "knee pain");
        TRADUCTIONS.put("otite", "ear infection");
        TRADUCTIONS.put("angine", "sore throat");
        TRADUCTIONS.put("gastrite", "gastritis");
        TRADUCTIONS.put("ulcère", "ulcer");
        TRADUCTIONS.put("ulcere", "ulcer");
        TRADUCTIONS.put("eczéma", "eczema");
        TRADUCTIONS.put("eczema", "eczema");
        TRADUCTIONS.put("psoriasis", "psoriasis");
        TRADUCTIONS.put("rhumatisme", "rheumatism");
        TRADUCTIONS.put("sinusite", "sinusitis");
        TRADUCTIONS.put("cystite", "cystitis");
        TRADUCTIONS.put("conjonctivite", "conjunctivitis");
        TRADUCTIONS.put("varicelle", "chickenpox");
        TRADUCTIONS.put("rougeole", "measles");
        TRADUCTIONS.put("malaria", "malaria");
        TRADUCTIONS.put("paludisme", "malaria");
    }

    // ── SEARCH ────────────────────────────────────────────────────────────────

    public List<String> searchMedicaments(String pathologieNom) {
        String searchTerm = translateToEnglish(pathologieNom);
        System.out.println("🔍 FDA search: '" + pathologieNom + "' → '" + searchTerm + "'");

        List<String> results = searchByField("indications_and_usage", searchTerm);
        if (results.isEmpty()) results = searchByField("purpose", searchTerm);
        if (results.isEmpty()) results = searchByField("description", searchTerm);

        System.out.println("✅ FDA found " + results.size() + " results");
        return results;
    }

    private List<String> searchByField(String field, String term) {
        List<String> results = new ArrayList<>();
        try {
            String query = URLEncoder.encode(term, StandardCharsets.UTF_8);
            String url   = BASE_URL + "?search=" + field + ":" + query + "&limit=15";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET().build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("FDA HTTP " + response.statusCode() + " [" + field + "]");

            if (response.statusCode() == 200) {
                String body = response.body();
                System.out.println("FDA preview: " + body.substring(0, Math.min(200, body.length())));
                results = parseResults(body);
            }
        } catch (Exception e) {
            System.err.println("❌ FDA searchByField error: " + e.getMessage());
        }
        return results;
    }

    // ── PARSE RESULTS — Version robuste ───────────────────────────────────────

    private List<String> parseResults(String json) {
        List<String> names = new ArrayList<>();

        // Essai 1 : brand_name
        extractAllValues(json, "brand_name", names);

        // Essai 2 : generic_name
        if (names.isEmpty()) extractAllValues(json, "generic_name", names);

        // Essai 3 : substance_name
        if (names.isEmpty()) extractAllValues(json, "substance_name", names);

        System.out.println("📋 Parsed " + names.size() + " drug names");
        return names;
    }

    private void extractAllValues(String json, String field, List<String> results) {
        int idx = 0;
        while ((idx = json.indexOf("\"" + field + "\"", idx)) != -1) {
            int arrStart   = json.indexOf("[", idx);
            int nextField  = json.indexOf("\"", idx + field.length() + 3);

            if (arrStart == -1 || (nextField != -1 && nextField < arrStart)) {
                idx++;
                continue;
            }

            int arrEnd = json.indexOf("]", arrStart);
            if (arrEnd == -1) { idx++; continue; }

            String arrContent = json.substring(arrStart + 1, arrEnd);
            int vIdx = 0;
            while ((vIdx = arrContent.indexOf("\"", vIdx)) != -1) {
                int vEnd = arrContent.indexOf("\"", vIdx + 1);
                if (vEnd == -1) break;
                String val = arrContent.substring(vIdx + 1, vEnd).trim();
                if (!val.isEmpty() && val.length() < 100
                        && !val.contains("{") && !results.contains(val)) {
                    results.add(val);
                }
                vIdx = vEnd + 1;
            }

            idx = arrEnd + 1;
            if (results.size() >= 10) break;
        }
    }

    // ── DETAILS ───────────────────────────────────────────────────────────────

    public String getMedicamentDetails(String medicamentName) {
        try {
            String query = URLEncoder.encode("\"" + medicamentName + "\"",
                    StandardCharsets.UTF_8);
            String url = BASE_URL + "?search=openfda.brand_name:" + query + "&limit=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET().build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseDetail(response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Aucune information détaillée disponible.";
    }

    // ── TRANSLATE ─────────────────────────────────────────────────────────────

    private String translateToEnglish(String frenchName) {
        if (frenchName == null) return "";
        String lower = frenchName.toLowerCase().trim();
        if (TRADUCTIONS.containsKey(lower)) return TRADUCTIONS.get(lower);
        for (Map.Entry<String, String> e : TRADUCTIONS.entrySet()) {
            if (lower.contains(e.getKey())) return e.getValue();
        }
        return frenchName;
    }

    // ── PARSE DETAIL ──────────────────────────────────────────────────────────

    private String parseDetail(String json) {
        StringBuilder sb = new StringBuilder();
        try {
            String brandName = extractField(json, "brand_name");
            if (brandName != null) sb.append("💊 Nom: ").append(brandName).append("\n\n");

            String genericName = extractField(json, "generic_name");
            if (genericName != null) sb.append("🔬 Nom générique: ").append(genericName).append("\n\n");

            String manufacturer = extractField(json, "manufacturer_name");
            if (manufacturer != null) sb.append("🏭 Fabricant: ").append(manufacturer).append("\n\n");

            String route = extractField(json, "route");
            if (route != null) sb.append("💉 Voie: ").append(route).append("\n\n");

            String purpose = extractSection(json, "purpose");
            if (purpose != null) sb.append("🎯 Utilisation: ").append(truncate(purpose, 400)).append("\n\n");

            String warnings = extractSection(json, "warnings");
            if (warnings != null) sb.append("⚠️ Avertissements: ").append(truncate(warnings, 400)).append("\n\n");

            String dosage = extractSection(json, "dosage_and_administration");
            if (dosage != null) sb.append("📋 Posologie: ").append(truncate(dosage, 400));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.length() > 0 ? sb.toString() : "Aucune information détaillée disponible.";
    }

    private String extractField(String json, String field) {
        try {
            int idx   = json.indexOf("\"" + field + "\"");
            if (idx == -1) return null;
            int start = json.indexOf("[\"", idx);
            int end   = json.indexOf("\"]", start);
            if (start != -1 && end != -1 && (end - start) < 500)
                return json.substring(start + 2, end);
        } catch (Exception ignored) {}
        return null;
    }

    private String extractSection(String json, String section) {
        try {
            int idx   = json.indexOf("\"" + section + "\"");
            if (idx == -1) return null;
            int start = json.indexOf("[\"", idx);
            int end   = json.indexOf("\"]", start);
            if (start != -1 && end != -1)
                return json.substring(start + 2, Math.min(end, start + 1000));
        } catch (Exception ignored) {}
        return null;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        s = s.replace("\\n", "\n").replace("\\t", " ").replace("\\u2022", "•");
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}