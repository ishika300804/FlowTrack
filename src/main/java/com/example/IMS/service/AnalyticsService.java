package com.example.IMS.service;

import com.example.IMS.model.Item;
import com.example.IMS.model.Loan;
import com.example.IMS.repository.IItemRepository;
import com.example.IMS.repository.IItemIssuanceRepository;
import com.example.IMS.repository.IVendorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Provides business analytics computed from Inventory, Loan, and Vendor data.
 */
@Service
public class AnalyticsService {

    @Autowired private IItemRepository itemRepository;
    @Autowired private IItemIssuanceRepository loanRepository;
    @Autowired private IVendorRepository vendorRepository;

    /** Full analytics snapshot for the retailer dashboard */
    public RetailerStats getRetailerStats() {
        List<Item> items = itemRepository.findAll();
        List<Loan> loans = loanRepository.findAll();

        long totalItems     = items.size();
        long inStock        = items.stream().filter(i -> i.getQuantity() > 0).count();
        long lowStock       = items.stream().filter(i -> i.getQuantity() > 0 && i.getQuantity() <= 5).count();
        long outOfStock     = items.stream().filter(i -> i.getQuantity() == 0).count();
        double stockValue   = items.stream().mapToDouble(i -> i.getQuantity() * i.getPrice()).sum();
        long activeVendors  = vendorRepository.count();
        long totalIssued    = loans.size();
        double fineCollected= loans.stream().mapToDouble(Loan::getTotalFine).sum();

        // Top 5 items by stock value
        List<ItemValueEntry> topItems = new ArrayList<>();
        items.stream()
             .sorted(Comparator.comparingDouble((Item i) -> i.getQuantity() * i.getPrice()).reversed())
             .limit(5)
             .forEach(i -> topItems.add(new ItemValueEntry(i.getName(), i.getQuantity(), i.getPrice())));

        // Stock distribution for chart (item name → value)
        Map<String, Double> stockDistribution = new LinkedHashMap<>();
        topItems.forEach(e -> stockDistribution.put(e.name, e.quantity * e.unitPrice));

        RetailerStats stats = new RetailerStats();
        stats.totalItems     = totalItems;
        stats.inStock        = inStock;
        stats.lowStock       = lowStock;
        stats.outOfStock     = outOfStock;
        stats.stockValue     = stockValue;
        stats.activeVendors  = activeVendors;
        stats.totalIssued    = totalIssued;
        stats.fineCollected  = fineCollected;
        stats.topItems       = topItems;
        stats.stockDistribution = stockDistribution;
        return stats;
    }

    // ── Inner DTOs ─────────────────────────────────────────────────────────────

    public static class RetailerStats {
        public long   totalItems;
        public long   inStock;
        public long   lowStock;
        public long   outOfStock;
        public double stockValue;
        public long   activeVendors;
        public long   totalIssued;
        public double fineCollected;
        public List<ItemValueEntry>   topItems;
        public Map<String, Double>    stockDistribution;
    }

    public static class ItemValueEntry {
        public String name;
        public int    quantity;
        public double unitPrice;
        public double totalValue;

        public ItemValueEntry(String name, int quantity, double unitPrice) {
            this.name       = name;
            this.quantity   = quantity;
            this.unitPrice  = unitPrice;
            this.totalValue = quantity * unitPrice;
        }
    }
}
