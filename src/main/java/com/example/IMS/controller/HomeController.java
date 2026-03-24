package com.example.IMS.controller;

import com.example.IMS.model.Item;
import com.example.IMS.model.Loan;
import com.example.IMS.service.ItemService;
import com.example.IMS.service.ItemIssuanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

@Controller
public class HomeController {
	
	@Autowired
	private ItemService itemService;
	
	@Autowired
	private ItemIssuanceService itemIssuanceService;
	
	@GetMapping("/dashboard")
	public String Index(){
		return "index";
	}
	
	@Autowired
	private com.example.IMS.service.DashboardTrackingService dashboardTrackingService;
	
	@GetMapping("/api/dashboard/stats")
	@ResponseBody
	public Map<String, Object> getDashboardStats() {
		Map<String, Object> stats = new HashMap<>();
		
		// Get all items and loans
		List<Item> allItems = itemService.getAllItems();
		List<Loan> allLoans = itemIssuanceService.getAllIssuedItems();
		
		// Items Borrowed (currently active loans - no return date)
		long itemsBorrowed = allLoans.stream()
				.filter(loan -> loan.getReturnDate() == null || loan.getReturnDate().isEmpty())
				.count();
		
		// Items Returned (loans with return date)
		long itemsReturned = allLoans.stream()
				.filter(loan -> loan.getReturnDate() != null && !loan.getReturnDate().isEmpty())
				.count();
		
		// Total inventory remaining (sum of all item quantities)
		int inventoryRemaining = allItems.stream()
				.mapToInt(Item::getQuantity)
				.sum();
		
		// Total items issued (all loans)
		long itemsIssued = allLoans.size();
		
		// Item distribution by type
		Map<String, Long> itemsByType = allItems.stream()
				.collect(Collectors.groupingBy(
					item -> item.getItemType() != null ? item.getItemType().getTypeName() : "Unknown",
					Collectors.counting()
				));
		
		// Low stock items (quantity < 10)
		long lowStockCount = allItems.stream()
				.filter(item -> item.getQuantity() < 10)
				.count();
		
		stats.put("itemsBorrowed", itemsBorrowed);
		stats.put("itemsReturned", itemsReturned);
		stats.put("inventoryRemaining", inventoryRemaining);
		stats.put("itemsIssued", itemsIssued);
		stats.put("itemsByType", itemsByType);
		stats.put("lowStockCount", lowStockCount);
		stats.put("totalItems", allItems.size());
		stats.put("totalLoans", allLoans.size());
		
		return stats;
	}
	
	@GetMapping("/api/dashboard/history")
	@ResponseBody
	public List<Map<String, Object>> getDashboardHistory() {
		List<com.example.IMS.model.DashboardSnapshot> snapshots = dashboardTrackingService.getRecentSnapshots();
		List<Map<String, Object>> history = new ArrayList<>();
		
		// Reverse to get chronological order
		Collections.reverse(snapshots);
		
		// Group by date and get last snapshot of each day
		Map<String, com.example.IMS.model.DashboardSnapshot> dailySnapshots = new LinkedHashMap<>();
		java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("MMM d");
		
		for (com.example.IMS.model.DashboardSnapshot snapshot : snapshots) {
			String dateKey = snapshot.getTimestamp().format(dateFormatter);
			// Keep the latest snapshot for each day
			dailySnapshots.put(dateKey, snapshot);
		}
		
		// Convert to list
		for (Map.Entry<String, com.example.IMS.model.DashboardSnapshot> entry : dailySnapshots.entrySet()) {
			com.example.IMS.model.DashboardSnapshot snapshot = entry.getValue();
			Map<String, Object> point = new HashMap<>();
			point.put("date", entry.getKey());
			point.put("timestamp", snapshot.getTimestamp().toString());
			point.put("itemsBorrowed", snapshot.getItemsBorrowed());
			point.put("itemsReturned", snapshot.getItemsReturned());
			point.put("inventoryRemaining", snapshot.getInventoryRemaining());
			point.put("itemsIssued", snapshot.getItemsIssued());
			point.put("eventType", snapshot.getEventType());
			history.add(point);
		}
		
		return history;
	}
}
