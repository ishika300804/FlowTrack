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

import com.example.IMS.dto.FineDto;
import com.example.IMS.model.Borrower;
import com.example.IMS.service.BorrowerService;
import com.example.IMS.service.EmailService;
import com.example.IMS.service.ItemIssuanceService;

@Controller
public class FineController {

	@Autowired
	private BorrowerService borrowerService;

	@Autowired
	private ItemIssuanceService itemIssuanceService;

	@Autowired
	private EmailService emailService;

	@GetMapping("/FineView")
	public String Index(Model model) {
		model.addAttribute("itemsWithFineList", itemIssuanceService.getItemsWithFine());
		return "Fine/View";
	}

	@GetMapping("/FineCreate")
	public String Create(Model model) {
		FineDto fineDto = new FineDto();
		model.addAttribute("fineDto", fineDto);
		return "Fine/Create";
	}

	@PostMapping("/FineDetails")
	public String Details(@Valid @ModelAttribute("fineDto") FineDto fineDto, BindingResult result) {
		Borrower borrower = borrowerService.getBorrowerById(fineDto.getBorrowerId());
		double totalFine = borrower.totalFine();
		fineDto.setTotalFine(totalFine);
		return "Fine/Details";
	}

	@PostMapping("/FinePayment/{borrowerId}")
	public String FinePayment(@PathVariable(value = "borrowerId") long borrowerId,
			@Valid @ModelAttribute("fineDto") FineDto fineDto, BindingResult result) {
		Borrower borrower = null;
		String err = borrowerService.validateBorrowerId(fineDto.getBorrowerId());
		if (!err.isEmpty()) {
			ObjectError error = new ObjectError("globalError", err);
			result.addError(error);
		} else {
			borrower = borrowerService.getBorrowerById(fineDto.getBorrowerId());
		}
		if (result.hasErrors()) {
			return "Fine/Create";
		}
		try {
			double fineBefore = borrower.totalFine();
			borrower.updateFine(fineDto.getFinePaid());
			double fineAfter = Math.max(0, fineBefore - fineDto.getFinePaid());
			borrowerService.updateBorrower(borrower);

			// PE-05: send fine payment receipt
			try {
				if (borrower.getEmail() != null && !borrower.getEmail().isBlank()) {
					emailService.sendFinePaymentReceiptEmail(
							borrower.getEmail(),
							borrower.getFirstName() + " " + borrower.getLastName(),
							fineDto.getFinePaid(),
							fineAfter);
				}
			} catch (Exception mailEx) {
				System.err.println("PE-05 fine receipt email failed: " + mailEx.getMessage());
			}
		} catch (Exception e) {
			System.out.println("Exception caught in Fine Controller.");
			err = "Unable to update borrower fine details.";
			ObjectError error = new ObjectError("globalError", err);
			result.addError(error);
		}
		return "redirect:/FineView";
	}
}
