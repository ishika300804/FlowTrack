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

import com.example.IMS.convertor.ItemRepairConvertor;
import com.example.IMS.dto.ItemRepairDto;
import com.example.IMS.model.ItemRepair;
import com.example.IMS.service.EmailService;
import com.example.IMS.service.ItemRepairService;
import com.example.IMS.service.ItemService;
import com.example.IMS.service.VendorService;

@Controller
public class ItemRepairController {

	@Autowired
	private ItemRepairService itemRepairService;
	@Autowired
	private VendorService vendorService;
	@Autowired
	private ItemService itemService;
	@Autowired
	private ItemRepairConvertor itemRepairConvertor;

	@Autowired
	private EmailService emailService;

	@GetMapping("/ItemRepairView")
	public String View(Model model) {
		model.addAttribute("ItemRepairDtoList", itemRepairConvertor.modelToDto(itemRepairService.getAllRepairItems()));
		return "/Item Repair/View";
	}

	@GetMapping("/ItemRepairCreate")
	public String Create(Model model) {
		ItemRepairDto itemRepairDto = new ItemRepairDto();
		model.addAttribute("itemRepairDto", itemRepairDto);
		return "/Item Repair/Create";
	}

	@PostMapping("/ItemRepairCreate")
	public String Create(@Valid @ModelAttribute("itemRepairDto") ItemRepairDto itemRepairDto, BindingResult result) {
		String err = vendorService.validateVendorId(itemRepairDto.getVendorId());
		if (!err.isEmpty()) {
			ObjectError error = new ObjectError("globalError", err);
			result.addError(error);
		}
		err = itemService.validateItemId(itemRepairDto.getItemId());
		if (!err.isEmpty()) {
			ObjectError error = new ObjectError("globalError", err);
			result.addError(error);
		}
		if (result.hasErrors()) {
			return "/Item Repair/Create";
		}

		ItemRepair savedRepair = itemRepairConvertor.DtoToModel(itemRepairDto);
		itemRepairService.saveItemRepair(savedRepair);

		// E-17: notify vendor of repair request
		try {
			if (savedRepair.getVendor() != null
					&& savedRepair.getVendor().getEmail() != null
					&& !savedRepair.getVendor().getEmail().isBlank()) {
				emailService.sendRepairRequestEmail(
						savedRepair.getVendor().getEmail(),
						savedRepair.getVendor().getName(),
						savedRepair.getItem() != null ? savedRepair.getItem().getName() : "Unknown item",
						savedRepair.getCost());
			}
		} catch (Exception mailEx) {
			System.err.println("E-17 email failed: " + mailEx.getMessage());
		}

		return "redirect:/ItemRepairView";

	}

	@GetMapping("/ItemRepairEdit/{id}")
	public String Edit(@PathVariable(value = "id") long id, Model model) {
		ItemRepair itemRepair = itemRepairService.findItemRepairById(id);
		model.addAttribute("itemRepairDto", itemRepairConvertor.modelToDto(itemRepair));
		return "/Item Repair/Edit";
	}

	@GetMapping("/ItemRepairDelete/{id}")
	public String Delete(@PathVariable(value = "id") long id, Model model) {
		ItemRepair itemRepair = itemRepairService.findItemRepairById(id);
		model.addAttribute("itemRepairDto", itemRepairConvertor.modelToDto(itemRepair));
		return "/Item Repair/Delete";
	}

	@PostMapping("/ItemRepairDelete/{id}")
	public String Delete(@PathVariable(value = "id") long id,
			@ModelAttribute("itemRepairDto") ItemRepairDto itemRepairDto) {
		itemRepairService.deleteItemRepairById(id);
		return "redirect:/ItemRepairView";
	}

}
