package com.example.IMS.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

public class EmailRequest {
    
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String to;
    
    @NotBlank(message = "Subject is required")
    private String subject;
    
    @NotBlank(message = "Body is required")
    private String body;
    
    private boolean isHtml;
    
    // Constructors
    public EmailRequest() {
        this.isHtml = false;
    }
    
    public EmailRequest(String to, String subject, String body) {
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.isHtml = false;
    }
    
    public EmailRequest(String to, String subject, String body, boolean isHtml) {
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.isHtml = isHtml;
    }
    
    // Getters and Setters
    public String getTo() {
        return to;
    }
    
    public void setTo(String to) {
        this.to = to;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getBody() {
        return body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }
    
    public boolean isHtml() {
        return isHtml;
    }
    
    public void setHtml(boolean isHtml) {
        this.isHtml = isHtml;
    }
}
