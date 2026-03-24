package com.example.IMS.chatbot.service;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ChatbotDatabaseService {

    // Use your ACTUAL repository names
    @Autowired
    private com.example.IMS.repository.IItemRepository itemRepository;  // Changed!

    @Autowired
    private com.example.IMS.repository.IVendorRepository vendorRepository;  // Changed!

    @Autowired
    private com.example.IMS.repository.IBorrowerRepository borrowerRepository;

    @Autowired
    private com.example.IMS.repository.ILoanRepository loanRepository;

    private final Gson gson = new Gson();

    /**
     * Execute a Gemini function-call with user context.
     * ctx is used to scope data appropriately (e.g. retailer vs admin).
     */
    public String executeFunction(String functionName, Map<String, Object> arguments,
                                  GeminiChatService.ChatUserContext ctx) {
        try {
            switch (functionName) {
                case "getAllInventoryItems":
                    return getAllInventoryItems();

                case "getAllVendors":
                    return getAllVendors();

                case "getAllBorrowers":
                    return getAllBorrowers();

                case "getAllLoans":
                    return getAllLoans();

                case "getItemById":
                    return getItemById(arguments);

                case "getLowStockItems":
                    return getLowStockItems(arguments);

                default:
                    return gson.toJson(Map.of("error", "Unknown function: " + functionName));
            }
        } catch (Exception e) {
            return gson.toJson(Map.of("error", e.getMessage()));
        }
    }

    private String getAllInventoryItems() {
        var items = itemRepository.findAll();
        StringBuilder result = new StringBuilder();
        result.append("Found ").append(items.size()).append(" inventory items:\n\n");
        
        for (var item : items) {
            result.append("• ").append(item.getName())
                  .append(" (ID: ").append(item.getId())
                  .append(", Quantity: ").append(item.getQuantity())
                  .append(", Price: $").append(item.getPrice())
                  .append(")\n");
        }
        
        return result.toString();
    }

    private String getAllVendors() {
        var vendors = vendorRepository.findAll();
        return gson.toJson(Map.of("vendors", vendors, "count", vendors.size()));
    }

    private String getAllBorrowers() {
        var borrowers = borrowerRepository.findAll();
        return gson.toJson(Map.of("borrowers", borrowers, "count", borrowers.size()));
    }

    private String getAllLoans() {
        var loans = loanRepository.findAll();
        return gson.toJson(Map.of("loans", loans, "count", loans.size()));
    }

    private String getItemById(Map<String, Object> arguments) {
        if (!arguments.containsKey("itemId")) {
            return gson.toJson(Map.of("error", "itemId parameter is required"));
        }
        
        Long itemId = ((Number) arguments.get("itemId")).longValue();
        var item = itemRepository.findById(itemId);
        
        if (item.isPresent()) {
            return gson.toJson(Map.of("item", item.get()));
        } else {
            return gson.toJson(Map.of("error", "Item not found with id: " + itemId));
        }
    }

    private String getLowStockItems(Map<String, Object> arguments) {
        final int threshold;
        if (arguments.containsKey("threshold")) {
            threshold = ((Number) arguments.get("threshold")).intValue();
        } else {
            threshold = 10;
        }
        
        var allItems = itemRepository.findAll();
        var lowStockItems = allItems.stream()
                .filter(item -> item.getQuantity() < threshold)
                .collect(java.util.stream.Collectors.toList());
        
        StringBuilder result = new StringBuilder();
        result.append("Found ").append(lowStockItems.size())
              .append(" items with stock below ").append(threshold).append(":\n\n");
        
        if (lowStockItems.isEmpty()) {
            result.append("No items are currently low in stock.");
        } else {
            for (var item : lowStockItems) {
                result.append("• ").append(item.getName())
                      .append(" (ID: ").append(item.getId())
                      .append(", Quantity: ").append(item.getQuantity())
                      .append(", Price: $").append(item.getPrice())
                      .append(")\n");
            }
        }
        
        return result.toString();
    }
}
