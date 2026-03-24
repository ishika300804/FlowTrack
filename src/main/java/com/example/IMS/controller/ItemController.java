package com.example.IMS.controller;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.IMS.convertor.ItemConvertor;
import com.example.IMS.dto.ItemDto;
import com.example.IMS.model.Item;
import com.example.IMS.model.ItemType;
import com.example.IMS.model.Vendor;
import com.example.IMS.service.ItemService;
import com.example.IMS.service.ItemTypeService;
import com.example.IMS.service.VendorService;

@Controller
public class ItemController {

	@Autowired
	private ItemService itemService;

	@Autowired
	private ItemTypeService itemTypeService;

	@Autowired
	private VendorService vendorService;

	@Autowired
	private ItemConvertor itemConvertor;
	
	@Autowired
	private com.example.IMS.service.DashboardTrackingService dashboardTrackingService;

	@GetMapping("/ItemView")
	public String View(Model model) {
		// Redirect to new modern inventory page
		return "redirect:/retailer/inventory";
	}

	/** New modern inventory list (retailer UI) */
	@GetMapping("/retailer/inventory")
	public String retailerInventory(Model model) {
		model.addAttribute("itemDtoList", itemConvertor.modelToDto(itemService.getAllItems()));
		return "retailer/item-view";
	}

	@GetMapping("/ItemCreate")
	public String Create(Model model) {
		// Redirect to new modern item creation page
		return "redirect:/retailer/inventory/add";
	}

	/** New modern Add Item form (retailer UI) */
	@GetMapping("/retailer/inventory/add")
	public String retailerAddItem(Model model) {
		model.addAttribute("itemDto", new ItemDto());
		model.addAttribute("itemTypeList", itemTypeService.getAllItemTypes());
		return "retailer/item-create";
	}

	@PostMapping("/ItemCreate")
	public String Create(@Valid @ModelAttribute("itemDto") ItemDto itemDto, BindingResult result, Model model) {
		Vendor vendor = null;
		ItemType itemType = null;
		Item item = null;
		String err = vendorService.validateVendorName(itemDto.getVendorName());
		if (!err.isEmpty()) {
			ObjectError error = new ObjectError("globalError", err);
			result.addError(error);
		} else {
			vendor = vendorService.getVendorByName(itemDto.getVendorName());
		}
		err = itemTypeService.validateItemTypeByName(itemDto.getItemType());
		if (!err.isEmpty()) {
			ObjectError error = new ObjectError("globalError", err);
			result.addError(error);
		} else {
			itemType = itemTypeService.getItemTypeByName(itemDto.getItemType());
		}
		err = itemService.validateItemId(itemDto.getItemName(), itemDto.getItemType());
		if (!err.isEmpty()) {
			ObjectError error = new ObjectError("globalError", err);
			result.addError(error);
		}

		if (result.hasErrors()) {
			model.addAttribute("itemTypeList", itemTypeService.getAllItemTypes());
			return "retailer/item-create";
		}
		item = itemConvertor.dtoToModel(itemDto);
		item.setVendor(vendor);
		item.setItemType(itemType);
		itemService.saveItem(item);
		dashboardTrackingService.captureSnapshot("ITEM_ADDED");
		return "redirect:/retailer/inventory";
	}

	@GetMapping("/ItemEdit/{id}")
	public String Edit(@PathVariable(value = "id") long id, Model model) {
		Item item = itemService.getItemById(id);
		model.addAttribute("itemDto", itemConvertor.modelToDto(item));
		model.addAttribute("itemTypeList", itemTypeService.getAllItemTypes());
		return "/Item/Edit";
	}

	@PostMapping("/ItemEdit/{id}")
	public String Edit(@PathVariable(value = "id") long id, @Valid @ModelAttribute("itemDto") ItemDto itemDto, 
			BindingResult result, Model model) {
		Vendor vendor = null;
		ItemType itemType = null;
		Item item = null;
		
		String err = vendorService.validateVendorName(itemDto.getVendorName());
		if (!err.isEmpty()) {
			ObjectError error = new ObjectError("globalError", err);
			result.addError(error);
		} else {
			vendor = vendorService.getVendorByName(itemDto.getVendorName());
		}
		
		err = itemTypeService.validateItemTypeByName(itemDto.getItemType());
		if (!err.isEmpty()) {
			ObjectError error = new ObjectError("globalError", err);
			result.addError(error);
		} else {
			itemType = itemTypeService.getItemTypeByName(itemDto.getItemType());
		}
		
		// For edit, check if item exists with different ID (not the current item being edited)
		Item existingItem = itemService.getItemById(id);
		if (existingItem != null) {
			// Check if name/type changed and conflicts with another item
			if (!existingItem.getName().equalsIgnoreCase(itemDto.getItemName()) || 
				!existingItem.getItemType().getTypeName().equalsIgnoreCase(itemDto.getItemType())) {
				err = itemService.validateItemId(itemDto.getItemName(), itemDto.getItemType());
				if (!err.isEmpty()) {
					ObjectError error = new ObjectError("globalError", err);
					result.addError(error);
				}
			}
		}

		if (result.hasErrors()) {
			model.addAttribute("itemTypeList", itemTypeService.getAllItemTypes());
			return "/Item/Edit";
		}
		
		item = itemService.getItemById(id);
		item.setName(itemDto.getItemName());
		item.setPrice(itemDto.getItemPrice());
		item.setQuantity(itemDto.getItemQuantity());
		item.setFineRate(itemDto.getFineRate());
		item.setInvoiceNumber(itemDto.getInvoiceNumber());
		item.setVendor(vendor);
		item.setItemType(itemType);
		itemService.saveItem(item);
		dashboardTrackingService.captureSnapshot("ITEM_UPDATED");
		return "redirect:/retailer/inventory";
	}

	@GetMapping("/ItemDelete/{id}")
	public String Delete(@PathVariable(value = "id") long id, Model model) {
		Item item = itemService.getItemById(id);
		model.addAttribute("itemDto", itemConvertor.modelToDto(item));
		return "/Item/Delete";
	}

	@PostMapping("/ItemDelete/{id}")
	public String Delete(@PathVariable(value = "id") long id, @ModelAttribute("itemDto") ItemDto itemDto) {
		itemService.deleteItem(id);
		return "redirect:/retailer/inventory";
	}

}
