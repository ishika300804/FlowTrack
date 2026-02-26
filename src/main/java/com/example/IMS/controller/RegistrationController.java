package com.example.IMS.controller;

import com.example.IMS.dto.RetailerRegistrationDto;
import com.example.IMS.dto.VendorRegistrationDto;
import com.example.IMS.dto.InvestorRegistrationDto;
import com.example.IMS.dto.UserRegistrationDto;
import com.example.IMS.model.User;
import com.example.IMS.service.EmailService;
import com.example.IMS.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
@RequestMapping("/register")
public class RegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;
    
    // Retailer Registration
    @GetMapping("/retailer")
    public String showRetailerRegistrationForm(Model model) {
        model.addAttribute("retailerDto", new RetailerRegistrationDto());
        return "auth/register-retailer";
    }
    
    @PostMapping("/retailer")
    public String registerRetailer(
            @Valid @ModelAttribute("retailerDto") RetailerRegistrationDto dto,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "auth/register-retailer";
        }
        
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.retailerDto", "Passwords do not match");
            return "auth/register-retailer";
        }
        
        try {
            UserRegistrationDto userDto = new UserRegistrationDto();
            userDto.setUsername(dto.getUsername());
            userDto.setEmail(dto.getEmail());
            userDto.setPassword(dto.getPassword());
            userDto.setFirstName(dto.getFirstName());
            userDto.setLastName(dto.getLastName());

            User savedUser = userService.registerUserWithRole(userDto, "ROLE_RETAILER");

            // Persist business hints so business-profile/create can be pre-populated
            userService.saveRegistrationHints(
                savedUser.getId(),
                dto.getBusinessName(),
                dto.getBusinessType(),
                dto.getGstNumber(),
                dto.getPhoneNumber(),
                dto.getBusinessAddress()
            );

            // E-01: Welcome email
            try {
                emailService.sendWelcomeEmail(dto.getEmail(),
                        dto.getFirstName() + " " + dto.getLastName(), "Retailer");
            } catch (Exception mailEx) {
                logger.warn("Welcome email failed for retailer {}: {}", dto.getEmail(), mailEx.getMessage());
            }

            logger.info("Retailer registered: {}", dto.getUsername());

            redirectAttributes.addFlashAttribute("successMessage",
                "Registration submitted successfully! Please log in and complete your business profile.");
            return "redirect:/login";
        } catch (Exception e) {
            result.rejectValue("email", "error.retailerDto", e.getMessage());
            return "auth/register-retailer";
        }
    }
    
    // Vendor Registration
    @GetMapping("/vendor")
    public String showVendorRegistrationForm(Model model) {
        model.addAttribute("vendorDto", new VendorRegistrationDto());
        return "auth/register-vendor";
    }
    
    @PostMapping("/vendor")
    public String registerVendor(
            @Valid @ModelAttribute("vendorDto") VendorRegistrationDto dto,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "auth/register-vendor";
        }
        
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.vendorDto", "Passwords do not match");
            return "auth/register-vendor";
        }
        
        try {
            UserRegistrationDto userDto = new UserRegistrationDto();
            userDto.setUsername(dto.getUsername());
            userDto.setEmail(dto.getEmail());
            userDto.setPassword(dto.getPassword());
            userDto.setFirstName(dto.getFirstName());
            userDto.setLastName(dto.getLastName());

            User savedUser = userService.registerUserWithRole(userDto, "ROLE_VENDOR");

            // Persist business hints so business-profile/create can be pre-populated
            userService.saveRegistrationHints(
                savedUser.getId(),
                dto.getCompanyName(),
                dto.getBusinessType(),
                dto.getGstNumber(),
                dto.getPhoneNumber(),
                dto.getCompanyAddress()
            );

            // E-02: Welcome email
            try {
                emailService.sendWelcomeEmail(dto.getEmail(),
                        dto.getFirstName() + " " + dto.getLastName(), "Vendor");
            } catch (Exception mailEx) {
                logger.warn("Welcome email failed for vendor {}: {}", dto.getEmail(), mailEx.getMessage());
            }

            logger.info("Vendor registered: {}", dto.getUsername());

            redirectAttributes.addFlashAttribute("successMessage",
                "Registration submitted successfully! Please log in and complete your business profile.");
            return "redirect:/login";
        } catch (Exception e) {
            result.rejectValue("email", "error.vendorDto", e.getMessage());
            return "auth/register-vendor";
        }
    }
    
    // Investor Registration
    @GetMapping("/investor")
    public String showInvestorRegistrationForm(Model model) {
        model.addAttribute("investorDto", new InvestorRegistrationDto());
        return "auth/register-investor";
    }
    
    @PostMapping("/investor")
    public String registerInvestor(
            @Valid @ModelAttribute("investorDto") InvestorRegistrationDto dto,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "auth/register-investor";
        }
        
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.investorDto", "Passwords do not match");
            return "auth/register-investor";
        }
        
        try {
            UserRegistrationDto userDto = new UserRegistrationDto();
            userDto.setUsername(dto.getUsername());
            userDto.setEmail(dto.getEmail());
            userDto.setPassword(dto.getPassword());
            userDto.setFirstName(dto.getFirstName());
            userDto.setLastName(dto.getLastName());

            userService.registerUserWithRole(userDto, "ROLE_INVESTOR");

            // E-03: Welcome email
            try {
                emailService.sendWelcomeEmail(dto.getEmail(),
                        dto.getFirstName() + " " + dto.getLastName(), "Investor");
            } catch (Exception mailEx) {
                logger.warn("Welcome email failed for investor {}: {}", dto.getEmail(), mailEx.getMessage());
            }

            logger.info("Investor registered: {}", dto.getUsername());

            redirectAttributes.addFlashAttribute("successMessage",
                "Registration submitted successfully! Please log in and complete your business profile.");
            return "redirect:/login";
        } catch (Exception e) {
            result.rejectValue("email", "error.investorDto", e.getMessage());
            return "auth/register-investor";
        }
    }
}
