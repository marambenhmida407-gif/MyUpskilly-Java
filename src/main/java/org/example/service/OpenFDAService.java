package org.example.service;

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

    // Dictionnaire français → anglais pour les pathologies courantes
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

    public List<String> searchMedicaments(String pathologieNom) {
        // Traduire en anglais si possible
        String searchTerm = translateToEnglish(pathologieNom);

        List<String> results = new ArrayList<>();

        // Essayer d'abord avec indications_and_usage
        results = searchByField("indications_and_usage", searchTerm);

        // Si rien trouvé, essayer avec purpose
        if (results.isEmpty()) {
            results = searchByField("purpose", searchTerm);
        }

        // Si toujours rien, essayer avec description
        if (results.isEmpty()) {
            results = searchByField("description", searchTerm);
        }

        return results;
    }

    private List<String> searchByField(String field, String term) {
        List<String> results = new ArrayList<>();
        try {
            String query = URLEncoder.encode(term, StandardCharsets.UTF_8);
            String url = BASE_URL + "?search=" + field + ":" + query + "&limit=15";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                results = parseResults(response.body());
            }
        } catch (Exception e) {
            // Silently handle — try next field
        }
        return results;
    }

    public String getMedicamentDetails(String medicamentName) {
        try {
            String query = URLEncoder.encode("\"" + medicamentName + "\"", StandardCharsets.UTF_8);
            String url = BASE_URL + "?search=openfda.brand_name:" + query + "&limit=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseDetail(response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Aucune information détaillée disponible.";
    }

    private String translateToEnglish(String frenchName) {
        if (frenchName == null) return "";
        String lower = frenchName.toLowerCase().trim();

        // Vérifier le dictionnaire
        if (TRADUCTIONS.containsKey(lower)) {
            return TRADUCTIONS.get(lower);
        }

        // Vérifier si un mot du nom est dans le dictionnaire
        for (Map.Entry<String, String> entry : TRADUCTIONS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Sinon retourner tel quel (certains termes médicaux sont identiques en anglais)
        return frenchName;
    }

    private List<String> parseResults(String json) {
        List<String> names = new ArrayList<>();
        try {
            int idx = 0;
            while ((idx = json.indexOf("\"brand_name\"", idx)) != -1) {
                int start = json.indexOf("[\"", idx);
                int end = json.indexOf("\"]", start);
                if (start != -1 && end != -1 && (end - start) < 200) {
                    String name = json.substring(start + 2, end);
                    if (!names.contains(name) && name.length() < 100 && !name.contains("{")) {
                        names.add(name);
                    }
                }
                idx = end + 1;
                if (names.size() >= 10) break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return names;
    }

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
            if (route != null) sb.append("💉 Voie d'administration: ").append(route).append("\n\n");

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
            int idx = json.indexOf("\"" + field + "\"");
            if (idx == -1) return null;
            int start = json.indexOf("[\"", idx);
            int end = json.indexOf("\"]", start);
            if (start != -1 && end != -1 && (end - start) < 500) {
                return json.substring(start + 2, end);
            }
        } catch (Exception e) {}
        return null;
    }

    private String extractSection(String json, String section) {
        try {
            int idx = json.indexOf("\"" + section + "\"");
            if (idx == -1) return null;
            int start = json.indexOf("[\"", idx);
            int end = json.indexOf("\"]", start);
            if (start != -1 && end != -1) {
                return json.substring(start + 2, Math.min(end, start + 1000));
            }
        } catch (Exception e) {}
        return null;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        s = s.replace("\\n", "\n").replace("\\t", " ").replace("\\u2022", "•");
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}