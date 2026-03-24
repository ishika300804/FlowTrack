package com.example.IMS.controller;

import com.example.IMS.chatbot.service.GeminiChatService;
import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.User;
import com.example.IMS.repository.BusinessProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private GeminiChatService geminiChatService;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    // ---------------------------------------------------------------
    // GET /api/chatbot/status
    // Tells the frontend: is this user logged in, subscribed, and what
    // role/business context do they have?  Always returns 200 so the
    // JS widget can render correctly without crashing on 401/403.
    // ---------------------------------------------------------------
    @GetMapping("/status")
    public ResponseEntity<ChatStatusResponse> status(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.ok(ChatStatusResponse.notLoggedIn());
        }

        User user = (User) auth.getPrincipal();
        String role = user.getAuthorities().stream()
                .findFirst().map(a -> a.getAuthority()).orElse("UNKNOWN");

        // Platform admins always have full chatbot access – no subscription needed
        if ("ROLE_PLATFORM_ADMIN".equals(role)) {
            return ResponseEntity.ok(
                    new ChatStatusResponse(true, true, role, user.getUsername(), "FlowTrack Platform", "ADMIN")
            );
        }

        // Retailers must have an active subscription
        if ("ROLE_RETAILER".equals(role)) {
            List<BusinessProfile> profiles = businessProfileRepository.findByUserId(user.getId());
            boolean subscribed = profiles.stream().anyMatch(BusinessProfile::hasActiveSubscription);
            String businessName = profiles.isEmpty() ? "" : profiles.get(0).getLegalBusinessName();
            String plan = profiles.stream()
                    .filter(BusinessProfile::hasActiveSubscription)
                    .map(BusinessProfile::getSubscriptionPlan)
                    .findFirst().orElse("");
            return ResponseEntity.ok(
                    new ChatStatusResponse(true, subscribed, role, user.getUsername(), businessName, plan)
            );
        }

        // All other roles (VENDOR, INVESTOR, etc.) – not subscribed to chatbot
        return ResponseEntity.ok(ChatStatusResponse.notSubscribed(user.getUsername(), role));
    }

    // ---------------------------------------------------------------
    // POST /api/chatbot/chat
    // Subscription-gated: RETAILER must have an active plan.
    // PLATFORM_ADMIN bypasses subscription check.
    // Context (role, business name, plan) is forwarded to the AI so it
    // gives role-appropriate responses.
    // ---------------------------------------------------------------
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request, Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401)
                    .body(new ChatResponse("Please log in to use the FlowTrack AI Assistant."));
        }

        User user = (User) auth.getPrincipal();
        String role = user.getAuthorities().stream()
                .findFirst().map(a -> a.getAuthority()).orElse("UNKNOWN");

        // Subscription check for retailers
        String businessName = "";
        String plan = "";

        if ("ROLE_RETAILER".equals(role)) {
            List<BusinessProfile> profiles = businessProfileRepository.findByUserId(user.getId());
            boolean subscribed = profiles.stream().anyMatch(BusinessProfile::hasActiveSubscription);
            if (!subscribed) {
                return ResponseEntity.status(403).body(new ChatResponse(
                        "🔒 The AI Assistant is a premium feature. " +
                        "Please subscribe to a FlowTrack plan to unlock it."
                ));
            }
            if (!profiles.isEmpty()) {
                businessName = profiles.get(0).getLegalBusinessName();
                plan = profiles.stream()
                        .filter(BusinessProfile::hasActiveSubscription)
                        .map(BusinessProfile::getSubscriptionPlan)
                        .findFirst().orElse("");
            }
        } else if ("ROLE_PLATFORM_ADMIN".equals(role)) {
            businessName = "FlowTrack Platform";
            plan = "ADMIN";
        }

        GeminiChatService.ChatUserContext ctx = new GeminiChatService.ChatUserContext(
                user.getId(), user.getUsername(), role, businessName, plan
        );

        String response = geminiChatService.chat(request.getMessage(), ctx);
        return ResponseEntity.ok(new ChatResponse(response));
    }

    @GetMapping("/test")
    public String test() {
        return "✅ Chatbot API is working!";
    }

    // ===== DTOs =====

    public static class ChatRequest {
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ChatResponse {
        private String response;
        public ChatResponse(String response) { this.response = response; }
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
    }

    public static class ChatStatusResponse {
        private boolean loggedIn;
        private boolean subscribed;
        private String role;
        private String userName;
        private String businessName;
        private String plan;

        public ChatStatusResponse(boolean loggedIn, boolean subscribed, String role,
                                  String userName, String businessName, String plan) {
            this.loggedIn     = loggedIn;
            this.subscribed   = subscribed;
            this.role         = role;
            this.userName     = userName;
            this.businessName = businessName;
            this.plan         = plan;
        }

        public static ChatStatusResponse notLoggedIn() {
            return new ChatStatusResponse(false, false, "ANONYMOUS", "", "", "");
        }
        public static ChatStatusResponse notSubscribed(String user, String role) {
            return new ChatStatusResponse(true, false, role, user, "", "");
        }

        public boolean isLoggedIn()      { return loggedIn; }
        public boolean isSubscribed()    { return subscribed; }
        public String getRole()          { return role; }
        public String getUserName()      { return userName; }
        public String getBusinessName()  { return businessName; }
        public String getPlan()          { return plan; }
    }
}
