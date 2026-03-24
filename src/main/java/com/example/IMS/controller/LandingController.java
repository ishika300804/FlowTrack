package com.example.IMS.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LandingController {
    
    @GetMapping({"/", "/landing"})
    public String showLandingPage() {
        return "landing";
    }
    
    @GetMapping("/home")
    public String showHomePage() {
        return "redirect:/landing";
    }
    
    @GetMapping("/get-started")
    public String showRoleSelectionPage() {
        return "choose-role";
    }
}
