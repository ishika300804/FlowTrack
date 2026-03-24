package com.example.IMS.chatbot.service;

import com.example.IMS.chatbot.model.FunctionDeclaration;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Service
public class GeminiChatService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Autowired
    private List<FunctionDeclaration> chatbotTools;

    @Autowired
    private ChatbotDatabaseService databaseService;

    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ---------------------------------------------------------------
    // Immutable user context carried from the controller into every
    // Gemini call so the AI knows who it is talking to.
    // ---------------------------------------------------------------
    public static class ChatUserContext {
        private final Long   userId;
        private final String userName;
        private final String role;         // ROLE_RETAILER | ROLE_PLATFORM_ADMIN | ...
        private final String businessName;
        private final String plan;         // BASIC | STANDARD | PREMIUM | ADMIN | ""

        public ChatUserContext(Long userId, String userName, String role,
                               String businessName, String plan) {
            this.userId       = userId;
            this.userName     = userName;
            this.role         = role;
            this.businessName = businessName;
            this.plan         = plan;
        }

        public Long   getUserId()       { return userId; }
        public String getUserName()     { return userName; }
        public String getRole()         { return role; }
        public String getBusinessName() { return businessName; }
        public String getPlan()         { return plan; }

        public boolean isAdmin() {
            return "ROLE_PLATFORM_ADMIN".equals(role);
        }
        public boolean isRetailer() {
            return "ROLE_RETAILER".equals(role);
        }
    }

    // ---------------------------------------------------------------
    // Build a role-aware system instruction so Gemini never leaks
    // cross-user data or responds outside the caller's scope.
    // ---------------------------------------------------------------
    private String buildSystemInstruction(ChatUserContext ctx) {
        if (ctx == null) {
            return "You are a helpful FlowTrack assistant.";
        }

        if (ctx.isAdmin()) {
            return "You are FlowTrack AI, a platform administration assistant. " +
                   "You are helping " + ctx.getUserName() + ", who is a FlowTrack platform administrator. " +
                   "You have access to system-wide data and can assist with: " +
                   "user management, retailer onboarding, business profile verification, " +
                   "compliance monitoring, platform analytics, and support queries. " +
                   "Always use Rs. (Indian Rupees) for monetary values. " +
                   "Be concise, data-driven, and professional.";
        }

        if (ctx.isRetailer()) {
            String biz = (ctx.getBusinessName() != null && !ctx.getBusinessName().isEmpty())
                    ? ctx.getBusinessName() : "their business";
            return "You are FlowTrack AI, a business intelligence assistant exclusively for " + biz + ". " +
                   "You are helping " + ctx.getUserName() + ", who is a retailer with an active " +
                   (ctx.getPlan() != null && !ctx.getPlan().isEmpty() ? ctx.getPlan() + " Plan" : "subscription") + ". " +
                   "Your scope is STRICTLY limited to: inventory management, stock levels, " +
                   "supplier/vendor information, business analytics, sales tracking, and " +
                   "fine/issuance records for " + biz + ". " +
                   "Do NOT provide information about other retailers, other businesses, or any " +
                   "administrative/platform-level data. " +
                   "If asked about something outside this scope, politely decline and redirect " +
                   "the user to relevant inventory or business questions. " +
                   "Always use Rs. (Indian Rupees) for all prices and monetary values. " +
                   "Be friendly, helpful, and business-focused.";
        }

        // Fallback for other roles
        return "You are a FlowTrack assistant helping " + ctx.getUserName() + ". " +
               "Answer only questions relevant to FlowTrack inventory and business management. " +
               "Use Rs. (Indian Rupees) for monetary values.";
    }

    // ---------------------------------------------------------------
    // Main chat method - accepts user context
    // ---------------------------------------------------------------
    public String chat(String userMessage, ChatUserContext ctx) {
        try {
            if (apiKey == null || apiKey.equals("your_api_key_here")) {
                return "Chatbot is not configured. Please set a valid Gemini API key.";
            }

            List<Map<String, Object>> contents = new ArrayList<>();
            contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", userMessage))
            ));

            int maxIterations = 5;
            for (int i = 0; i < maxIterations; i++) {
                JsonObject response = callGeminiApi(contents, ctx);

                if (response == null) {
                    return "Error: Received null response from Gemini API. Please check your API key.";
                }
                if (!response.has("candidates") || response.getAsJsonArray("candidates").size() == 0) {
                    System.err.println("API Response: " + response.toString());
                    return "Error: No response candidates from API. The API key might be invalid or the request was blocked.";
                }

                JsonArray candidates = response.getAsJsonArray("candidates");
                JsonObject candidate = candidates.get(0).getAsJsonObject();

                if (!candidate.has("content")) {
                    System.err.println("Candidate: " + candidate.toString());
                    return "Error: No content in API response. Please try again.";
                }

                JsonObject content = candidate.getAsJsonObject("content");
                if (!content.has("parts") || content.getAsJsonArray("parts").size() == 0) {
                    return "Error: No parts in response content.";
                }

                JsonArray parts = content.getAsJsonArray("parts");
                JsonObject firstPart = parts.get(0).getAsJsonObject();

                if (firstPart.has("functionCall")) {
                    JsonObject functionCall = firstPart.getAsJsonObject("functionCall");
                    String functionName = functionCall.get("name").getAsString();
                    Map<String, Object> args = functionCall.has("args")
                            ? gson.fromJson(functionCall.getAsJsonObject("args"), Map.class)
                            : new HashMap<>();

                    // Pass user context so DB service can apply data-isolation if needed
                    String result = databaseService.executeFunction(functionName, args, ctx);

                    contents.add(Map.of(
                            "role", "model",
                            "parts", List.of(Map.of("functionCall", gson.fromJson(gson.toJson(functionCall), Map.class)))
                    ));
                    contents.add(Map.of(
                            "role", "user",
                            "parts", List.of(Map.of(
                                    "functionResponse", Map.of(
                                            "name", functionName,
                                            "response", Map.of("content", result)
                                    )
                            ))
                    ));

                } else if (firstPart.has("text")) {
                    return firstPart.get("text").getAsString();
                }
            }

            return "Sorry, I couldn't complete that request.";

        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("429")) {
                return "Chatbot quota exceeded. The free tier limit has been reached. " +
                       "Please try again later or upgrade your API plan at https://ai.google.dev/pricing";
            }
            if (errorMsg != null && errorMsg.contains("API returned status")) {
                return "API Error: " + errorMsg.substring(0, Math.min(200, errorMsg.length())) + "...";
            }
            return "Error: " + errorMsg;
        }
    }

    // Kept for backward compatibility
    public String chat(String userMessage) {
        return chat(userMessage, null);
    }

    private JsonObject callGeminiApi(List<Map<String, Object>> contents, ChatUserContext ctx) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "contents", contents,
                "tools", List.of(Map.of("functionDeclarations", chatbotTools)),
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", buildSystemInstruction(ctx)))
                )
        );

        String jsonBody = gson.toJson(requestBody);
        String urlWithKey = apiUrl + "?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlWithKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("API returned status " + response.statusCode() + ": " + response.body());
        }

        return gson.fromJson(response.body(), JsonObject.class);
    }
}
