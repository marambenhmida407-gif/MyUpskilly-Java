package com.yessinmedqa.services;

import com.yessinmedqa.config.DBConnection;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AIService {

    // ── GROQ — text, suggestions, moderation, similar cases ──
    private static final String groqApiKey = System.getenv("GROQ_API_KEY");
    private static final String GROQ_URL       = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL     = "llama-3.3-70b-versatile";

    // ── CLOUDFLARE — image analysis ──────────────────────────
    private static final String CF_ACCOUNT_ID  = "fc991d0c2f4a14d6a80e8d412781b85c";
    private static final String cloudflareToken = System.getenv("CLOUDFLARE_TOKEN");
    private static final String CF_IMAGE_MODEL = "@cf/llava-1.5-7b-hf";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    // ─────────────────────────────────────────────────────────
// FEATURE: Analyze image using Groq vision model
// ─────────────────────────────────────────────────────────
    public String analyzeImageWithGroq(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return null;

        File file = new File(imagePath);
        if (!file.exists()) return null;

        String lower = imagePath.toLowerCase();
        if (lower.endsWith(".pdf"))
            return "📄 Fichier PDF joint — le médecin pourra le consulter.";

        try {
            byte[] imageBytes = Files.readAllBytes(file.toPath());
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = lower.endsWith(".png") ? "image/png" : "image/jpeg";

            // Groq vision request format
            String jsonBody = "{"
                    + "\"model\": \"meta-llama/llama-4-scout-17b-16e-instruct\","
                    + "\"messages\": [{"
                    + "  \"role\": \"user\","
                    + "  \"content\": ["
                    + "    {"
                    + "      \"type\": \"image_url\","
                    + "      \"image_url\": {"
                    + "        \"url\": \"data:" + mimeType + ";base64," + base64Image + "\""
                    + "      }"
                    + "    },"
                    + "    {"
                    + "      \"type\": \"text\","
                    + "      \"text\": \"Tu es un assistant médical. Analyse cette image médicale et fournis en français:\\n1. DESCRIPTION: ce que tu observes médicalement (2-3 phrases)\\n2. CONDITIONS POSSIBLES: conditions potentielles liées à cette image (non définitif)\\n3. CONSEIL: que devrait vérifier le médecin en priorité\\n\\nPrécise que c'est indicatif seulement.\""
                    + "    }"
                    + "  ]"
                    + "}],"
                    + "\"max_tokens\": 500"
                    + "}";

            RequestBody requestBody = RequestBody.create(
                    jsonBody, MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(GROQ_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + GROQ_API_KEY)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();

                if (!response.isSuccessful()) {
                    System.err.println("❌ Groq vision failed: "
                            + response.code() + " — " + responseBody);
                    return null;
                }

                JSONObject json = new JSONObject(responseBody);
                return json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
            }

        } catch (Exception e) {
            System.err.println("❌ analyzeImageWithGroq error: " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────
    // FEATURE 1: Analyze patient uploaded image (Cloudflare)
    // ─────────────────────────────────────────────────────────
    public String analyzeImage(String imagePath) {
        // ✅ Use Groq vision for real image analysis
        return analyzeImageWithGroq(imagePath);
    }

    // ─────────────────────────────────────────────────────────
    // FEATURE 2: AI suggestions for doctor (Groq)
    // ─────────────────────────────────────────────────────────
    public String getDoctorAssistance(String questionTitre,
                                      String questionDesc,
                                      String aiImageAnalysis,
                                      String categorie) {
        String prompt =
                "Tu es un assistant pour médecins sur une plateforme médicale.\n" +
                        "Un patient a posé la question suivante:\n\n" +
                        "Titre: " + questionTitre + "\n" +
                        "Catégorie: " + (categorie != null ? categorie : "Non spécifiée") + "\n" +
                        "Description: " + questionDesc + "\n" +
                        (aiImageAnalysis != null ?
                                "Analyse de l'image fournie: " + aiImageAnalysis + "\n" : "") +
                        "\nSuggère en français:\n" +
                        "1. DIAGNOSTIC POSSIBLE: (1-2 hypothèses brèves)\n" +
                        "2. CONSEILS RECOMMANDÉS: (2-3 points pratiques)\n" +
                        "3. EXAMENS À ENVISAGER: (si pertinent)\n\n" +
                        "Sois concis. Le médecin reste décisionnaire final.";

        return callGroq(prompt);
    }

    // ─────────────────────────────────────────────────────────
    // FEATURE: Analyze patient question before posting (Groq)
    // ─────────────────────────────────────────────────────────
    public String analyzePatientQuestion(String titre,
                                         String description,
                                         String categorie) {
        String prompt =
                "Tu es un assistant médical pour patients.\n" +
                        "Un patient s'apprête à poser cette question:\n\n" +
                        "Titre: " + titre + "\n" +
                        "Catégorie: " + (categorie != null ? categorie : "Non spécifiée") + "\n" +
                        "Description: " + description + "\n\n" +
                        "Donne en français:\n" +
                        "1. ANALYSE: Ce que tu comprends du problème (1-2 phrases)\n" +
                        "2. CONSEIL IMMÉDIAT: Un conseil général en attendant le médecin\n" +
                        "3. URGENCE: Est-ce urgent? (OUI/NON + pourquoi en 1 phrase)\n\n" +
                        "Précise que ceci n'est pas un diagnostic médical.";

        return callGroq(prompt);
    }

    // ─────────────────────────────────────────────────────────
    // FEATURE 3: Similar past questions — STRICT matching
    // ─────────────────────────────────────────────────────────
    public List<String[]> findSimilarQuestions(String newQuestion) {
        List<String[]> result = new ArrayList<>();

        List<String[]> allQuestions = getRecentQuestions(50);
        if (allQuestions.isEmpty()) return result;

        StringBuilder qList = new StringBuilder();
        for (String[] q : allQuestions) {
            qList.append("ID:").append(q[0])
                    .append(" | TITRE: ").append(q[1])
                    .append(" | DESC: ").append(
                            q[2] != null ? q[2].substring(0,
                                    Math.min(q[2].length(), 80)) : "")
                    .append("\n");
        }

        // ✅ Much stricter prompt — only return truly related questions
        String prompt =
                "Tu es un système de recherche médicale sémantique.\n\n" +
                        "Nouvelle question du patient: \"" + newQuestion + "\"\n\n" +
                        "Règles STRICTES:\n" +
                        "- Retourne UNIQUEMENT les IDs des questions qui parlent du MÊME problème médical\n" +
                        "- Si aucune question n'est vraiment similaire, retourne: AUCUN\n" +
                        "- Maximum 3 IDs\n" +
                        "- Ne retourne PAS de questions juste vaguement liées\n" +
                        "- Retourne UNIQUEMENT les IDs séparés par des virgules ou le mot AUCUN\n\n" +
                        "Questions existantes:\n" + qList;

        String response = callGroq(prompt);
        if (response == null) return result;

        // ✅ If AI says no similar questions found — return empty
        if (response.trim().toUpperCase().contains("AUCUN")) return result;

        // Parse IDs
        String[] ids = response.trim().replaceAll("[^0-9,]", "").split(",");
        for (String idStr : ids) {
            String id = idStr.trim();
            if (id.isEmpty()) continue;
            for (String[] q : allQuestions) {
                if (q[0].equals(id)) {
                    result.add(q);
                    break;
                }
            }
        }
        return result;
    }

    private List<String[]> getRecentQuestions(int limit) {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT q.id, q.titre, q.description, r.contenu " +
                "FROM questions q " +
                "JOIN reponses r ON r.question_id = q.id " +
                "WHERE q.statut = 'repondu' " +
                "ORDER BY q.id DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                        rs.getString("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getString("contenu")
                });
            }
        } catch (SQLException e) {
            System.err.println("❌ getRecentQuestions error: " + e.getMessage());
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────
    // FEATURE 4: Toxic content detection
    // ─────────────────────────────────────────────────────────
    public boolean isToxic(String text) {
        if (text == null || text.trim().isEmpty()) return false;

        String prompt =
                "Analyse ce texte et réponds UNIQUEMENT par OUI ou NON.\n" +
                        "Le texte contient-il des insultes, du contenu toxique, " +
                        "ou du langage inapproprié pour une plateforme médicale?\n\n" +
                        "Texte: \"" + text + "\"\n\n" +
                        "Réponse (OUI ou NON uniquement):";

        String response = callGroq(prompt);
        return response != null &&
                response.trim().toUpperCase().contains("OUI");
    }

    // ─────────────────────────────────────────────────────────
    // Internal: Groq text call
    // ─────────────────────────────────────────────────────────
    private String callGroq(String prompt) {
        try {
            Thread.sleep(300);

            String jsonBody = "{"
                    + "\"model\": \"" + GROQ_MODEL + "\","
                    + "\"messages\": [{"
                    + "  \"role\": \"user\","
                    + "  \"content\": \""
                    + prompt.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "")
                    + "\"}],"
                    + "\"max_tokens\": 500"
                    + "}";

            RequestBody requestBody = RequestBody.create(
                    jsonBody, MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(GROQ_URL)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + GROQ_API_KEY)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                if (!response.isSuccessful()) {
                    System.err.println("❌ Groq API failed: "
                            + response.code() + " — " + responseBody);
                    return null;
                }
                JSONObject json = new JSONObject(responseBody);
                return json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
            }

        } catch (Exception e) {
            System.err.println("❌ callGroq error: " + e.getMessage());
            return null;
        }
    }
}