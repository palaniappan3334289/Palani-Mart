package com.palani.palanimart.service.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.palani.palanimart.dto.ChatMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

/**
 * Server-side Google Gemini AI client for PalaniMart.
 * Uses standard Java 17 HttpClient with strict timeouts and error isolation.
 * API keys are never exposed to the client browser.
 */
public class GeminiAiClient {

    private static final Logger logger = LoggerFactory.getLogger(GeminiAiClient.class);
    private static final String DEFAULT_MODEL = "gemini-3.8-flash";
    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final Duration TIMEOUT = Duration.ofSeconds(12);

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final Gson gson;

    public GeminiAiClient() {
        this(resolveApiKey(), resolveModel());
    }

    public GeminiAiClient(String apiKey, String model) {
        this.apiKey = apiKey != null ? apiKey.trim() : null;
        this.model = (model != null && !model.trim().isEmpty()) ? model.trim() : DEFAULT_MODEL;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.gson = new Gson();
    }

    /**
     * Checks if a valid API key is present in environment or configuration.
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !"mock".equalsIgnoreCase(apiKey);
    }

    /**
     * Generates a conversational response from Google Gemini grounded with catalog context.
     *
     * @param systemInstruction Persona, factual policies, and catalog summary
     * @param history           Optional prior conversation turns
     * @param userMessage       Current customer prompt
     * @return AI generated text response
     * @throws Exception on API quota error, network timeout, or HTTP non-200
     */
    public String generateResponse(String systemInstruction, List<ChatMessageDTO> history, String userMessage) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }

        String endpoint = GEMINI_API_BASE + model + ":generateContent?key=" + apiKey;

        JsonObject root = new JsonObject();

        // System instructions for persona, ground truth and accuracy
        if (systemInstruction != null && !systemInstruction.trim().isEmpty()) {
            JsonObject sysInstObj = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", systemInstruction);
            parts.add(part);
            sysInstObj.add("parts", parts);
            root.add("system_instruction", sysInstObj);
        }

        // Contents array
        JsonArray contents = new JsonArray();

        // Conversation history (limit to last 6 turns for token sanity)
        if (history != null && !history.isEmpty()) {
            int startIdx = Math.max(0, history.size() - 6);
            for (int i = startIdx; i < history.size(); i++) {
                ChatMessageDTO turn = history.get(i);
                if (turn == null || turn.getContent() == null || turn.getContent().trim().isEmpty()) {
                    continue;
                }
                JsonObject turnObj = new JsonObject();
                String role = "assistant".equalsIgnoreCase(turn.getRole()) || "model".equalsIgnoreCase(turn.getRole())
                        ? "model" : "user";
                turnObj.addProperty("role", role);
                JsonArray turnParts = new JsonArray();
                JsonObject textPart = new JsonObject();
                textPart.addProperty("text", turn.getContent().trim());
                turnParts.add(textPart);
                turnObj.add("parts", turnParts);
                contents.add(turnObj);
            }
        }

        // Latest customer message
        JsonObject currentTurn = new JsonObject();
        currentTurn.addProperty("role", "user");
        JsonArray currentParts = new JsonArray();
        JsonObject currentPart = new JsonObject();
        currentPart.addProperty("text", userMessage);
        currentParts.add(currentPart);
        currentTurn.add("parts", currentParts);
        contents.add(currentTurn);

        root.add("contents", contents);

        // Generation config
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("temperature", 0.4);
        genConfig.addProperty("maxOutputTokens", 600);
        root.add("generationConfig", genConfig);

        String requestJson = gson.toJson(root);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        logger.debug("Dispatching request to Gemini API model: {}", model);
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.warn("Gemini API call failed with HTTP status {}: {}", response.statusCode(), response.body());
            throw new RuntimeException("Gemini API error (HTTP " + response.statusCode() + "): " + response.body());
        }

        JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
        if (responseJson == null || !responseJson.has("candidates")) {
            throw new RuntimeException("Empty or malformed candidate response from Gemini");
        }

        JsonArray candidates = responseJson.getAsJsonArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("No candidates returned by Gemini");
        }

        JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
        JsonObject content = firstCandidate.getAsJsonObject("content");
        if (content != null && content.has("parts")) {
            JsonArray resParts = content.getAsJsonArray("parts");
            StringBuilder replyText = new StringBuilder();
            for (JsonElement p : resParts) {
                if (p.isJsonObject() && p.getAsJsonObject().has("text")) {
                    replyText.append(p.getAsJsonObject().get("text").getAsString());
                }
            }
            return replyText.toString().trim();
        }

        throw new RuntimeException("No text parts in Gemini response");
    }

    private static String resolveApiKey() {
        String key = System.getenv("GEMINI_API_KEY");
        if (key != null && !key.trim().isEmpty()) {
            return key.trim();
        }
        key = System.getProperty("gemini.api.key");
        if (key != null && !key.trim().isEmpty()) {
            return key.trim();
        }
        // Fallback to config.properties
        try (InputStream in = GeminiAiClient.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                String propKey = props.getProperty("gemini.api.key");
                if (propKey != null && !propKey.trim().isEmpty()) {
                    return propKey.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String resolveModel() {
        String model = System.getenv("GEMINI_MODEL");
        if (model != null && !model.trim().isEmpty()) {
            return model.trim();
        }
        model = System.getProperty("gemini.model");
        if (model != null && !model.trim().isEmpty()) {
            return model.trim();
        }
        try (InputStream in = GeminiAiClient.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                String propModel = props.getProperty("gemini.model");
                if (propModel != null && !propModel.trim().isEmpty()) {
                    return propModel.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return DEFAULT_MODEL;
    }
}
