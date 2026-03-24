package com.example.IMS.service;

import com.example.IMS.dto.AddBankAccountRequest;
import com.example.IMS.dto.BankDetailsDTO;
import com.example.IMS.dto.verification.VerificationRequest;
import com.example.IMS.dto.verification.VerificationResult;
import com.example.IMS.model.enums.BankVerificationStatus;
import com.example.IMS.model.enums.VerificationType;
import com.example.IMS.model.BankDetails;
import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.BusinessProfileRole;
import com.example.IMS.repository.BankDetailsRepository;
import com.example.IMS.repository.BusinessProfileRepository;
import com.example.IMS.repository.BusinessProfileRoleRepository;
import com.example.IMS.service.verification.VerificationOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for Bank Account management
 * 
 * ENFORCEMENT RULES:
 * 1. Block settlements if no verified primary account exists
 * 2. Only OWNER, ADMIN, or FINANCE roles can add/modify bank accounts
 * 3. Primary account designation is atomic (triggers handle uniqueness)
 * 4. All operations enforce tenant isolation
 */
@Service
@Transactional
public class BankDetailsService {
    
    private static final Logger logger = LoggerFactory.getLogger(BankDetailsService.class);
    
    @Autowired
    private BankDetailsRepository bankDetailsRepository;
    
    @Autowired
    private BusinessProfileRepository businessProfileRepository;
    
    @Autowired
    private BusinessProfileRoleRepository roleRepository;
    
    @Autowired
    private VerificationOrchestrator verificationOrchestrator;
    
    // ========================================================================
    // CREATE OPERATIONS
    // ========================================================================
    
    /**
     * Add a new bank account to a business profile
     * Requires FINANCE permission or higher
     * 
     * @param businessProfileId business profile ID
     * @param request bank account details
     * @param userId user adding the account
     * @return created bank account DTO
     * @throws AccessDeniedException if user lacks permission
     */
    public BankDetailsDTO addBankAccount(Long businessProfileId, AddBankAccountRequest request, Long userId) {
        // Verify user has permission (FINANCE or higher)
        if (!hasFinancePermission(businessProfileId, userId)) {
            throw new AccessDeniedException("You do not have permission to manage bank accounts");
        }
        
        // Get business profile
        BusinessProfile profile = businessProfileRepository.findById(businessProfileId)
            .orElseThrow(() -> new EntityNotFoundException("Business profile not found: " + businessProfileId));
        
        // Create bank details entity
        BankDetails bankDetails = new BankDetails();
        bankDetails.setBusinessProfile(profile);
        bankDetails.setAccountHolderName(request.getAccountHolderName());
        bankDetails.setAccountNumber(request.getAccountNumber());  // Will be encrypted
        bankDetails.setIfscCode(request.getIfscCode());
        bankDetails.setBankName(request.getBankName());
        bankDetails.setBranchName(request.getBranchName());
        bankDetails.setPrimary(request.getIsPrimary());
        bankDetails.setBankVerificationStatus(BankVerificationStatus.UNVERIFIED);
        
        // Save (trigger will handle primary account uniqueness)
        BankDetails saved = bankDetailsRepository.save(bankDetails);
        logger.info("Added bank account {} to business profile {}", saved.getId(), businessProfileId);
        
        // Trigger bank verification (penny drop) if verification is enabled
        try {
            VerificationRequest verificationRequest = VerificationRequest.forBank(
                businessProfileId,
                saved.getId(),
                saved.getAccountNumber(),  // Decrypted by @Convert annotation
                saved.getIfscCode(),
                saved.getAccountHolderName()
            );
            
            VerificationResult result = verificationOrchestrator.executeVerification(verificationRequest);
            
            if (result.isSuccess()) {
                logger.info("Bank verification successful for account {}", saved.getId());
                // Note: VerificationLog is saved in orchestrator, status update can be done via scheduled job
            } else {
                logger.warn("Bank verification failed for account {}: {}", 
                    saved.getId(), result.getMessage());
            }
        } catch (Exception e) {
            // Log error but don't fail account creation
            logger.error("Failed to trigger bank verification for account {}", saved.getId(), e);
        }
        
        return convertToDTO(saved);
    }
    
    // ========================================================================
    // READ OPERATIONS (with tenant isolation)
    // ========================================================================
    
    /**
     * Get all bank accounts for a business profile
     * 
     * @param businessProfileId business profile ID
     * @param userId user requesting accounts
     * @return list of bank account DTOs
     */
    public List<BankDetailsDTO> getBankAccountsByBusinessProfile(Long businessProfileId, Long userId) {
        // Verify access
        if (!hasAccessToBusinessProfile(businessProfileId, userId)) {
            throw new AccessDeniedException("You do not have access to this business profile");
        }
        
        List<BankDetails> accounts = bankDetailsRepository.findByBusinessProfileId(businessProfileId);
        return accounts.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Get primary verified bank account (for settlements)
     * 
     * @param businessProfileId business profile ID
     * @param userId user requesting account
     * @return primary verified account DTO
     * @throws IllegalStateException if no primary verified account exists
     */
    public BankDetailsDTO getPrimaryVerifiedAccount(Long businessProfileId, Long userId) {
        if (!hasAccessToBusinessProfile(businessProfileId, userId)) {
            throw new AccessDeniedException("You do not have access to this business profile");
        }
        
        BankDetails account = bankDetailsRepository.findPrimaryVerifiedAccount(businessProfileId)
            .orElseThrow(() -> new IllegalStateException(
                "No verified primary bank account found. Add and verify a bank account first."
            ));
        
        return convertToDTO(account);
    }
    
    // ========================================================================
    // UPDATE OPERATIONS
    // ========================================================================
    
    /**
     * Set a bank account as primary
     * Automatically unmarks other accounts as primary (handled by trigger)
     * 
     * @param accountId bank account ID
     * @param businessProfileId business profile ID (for tenant validation)
     * @param userId user performing operation
     * @return updated bank account DTO
     */
    public BankDetailsDTO setPrimaryAccount(Long accountId, Long businessProfileId, Long userId) {
        if (!hasFinancePermission(businessProfileId, userId)) {
            throw new AccessDeniedException("You do not have permission to manage bank accounts");
        }
        
        BankDetails account = bankDetailsRepository.findByIdAndBusinessProfileId(accountId, businessProfileId)
            .orElseThrow(() -> new EntityNotFoundException("Bank account not found"));
        
        account.setPrimary(true);
        BankDetails updated = bankDetailsRepository.save(account);
        logger.info("Set bank account {} as primary for business profile {}", accountId, businessProfileId);
        
        return convertToDTO(updated);
    }
    
    /**
     * Mark bank account as verified (after successful verification)
     * Typically called by verification service after external API confirmation
     * 
     * @param accountId bank account ID
     * @param businessProfileId business profile ID
     * @param userId user performing operation
     * @return updated bank account DTO
     */
    public BankDetailsDTO markAsVerified(Long accountId, Long businessProfileId, Long userId) {
        if (!hasFinancePermission(businessProfileId, userId)) {
            throw new AccessDeniedException("You do not have permission to manage bank accounts");
        }
        
        BankDetails account = bankDetailsRepository.findByIdAndBusinessProfileId(accountId, businessProfileId)
            .orElseThrow(() -> new EntityNotFoundException("Bank account not found"));
        
        account.markAsVerified("MANUAL_VERIFICATION");
        BankDetails updated = bankDetailsRepository.save(account);
        logger.info("Marked bank account {} as VERIFIED", accountId);
        
        return convertToDTO(updated);
    }
    
    // ========================================================================
    // DELETE OPERATIONS
    // ========================================================================
    
    /**
     * Remove a bank account
     * Cannot remove primary verified account if it's the only one
     * 
     * @param accountId bank account ID
     * @param businessProfileId business profile ID
     * @param userId user performing operation
     */
    public void removeBankAccount(Long accountId, Long businessProfileId, Long userId) {
        if (!hasFinancePermission(businessProfileId, userId)) {
            throw new AccessDeniedException("You do not have permission to manage bank accounts");
        }
        
        BankDetails account = bankDetailsRepository.findByIdAndBusinessProfileId(accountId, businessProfileId)
            .orElseThrow(() -> new EntityNotFoundException("Bank account not found"));
        
        // Check if this is the only primary verified account
        if (account.isPrimary() && account.getBankVerificationStatus() == BankVerificationStatus.VERIFIED) {
            long verifiedCount = bankDetailsRepository.findByBusinessProfileId(businessProfileId).stream()
                .filter(bd -> bd.getBankVerificationStatus() == BankVerificationStatus.VERIFIED)
                .count();
            
            if (verifiedCount == 1) {
                throw new IllegalStateException("Cannot remove the only verified bank account. Add another verified account first.");
            }
        }
        
        bankDetailsRepository.delete(account);
        logger.info("Removed bank account {} from business profile {}", accountId, businessProfileId);
    }
    
    // ========================================================================
    // ENFORCEMENT LOGIC
    // ========================================================================
    
    /**
     * Check if business has a verified primary account for settlements
     * ENFORCEMENT: Block settlement operations if no verified primary account
     * 
     * @param businessProfileId business profile ID
     * @param userId user ID
     * @return true if verified primary account exists
     * @throws IllegalStateException if no verified primary account
     */
    public boolean canUseForSettlement(Long businessProfileId, Long userId) {
        if (!hasAccessToBusinessProfile(businessProfileId, userId)) {
            throw new AccessDeniedException("You do not have access to this business profile");
        }
        
        Optional<BankDetails> primaryAccount = bankDetailsRepository.findPrimaryVerifiedAccount(businessProfileId);
        
        if (primaryAccount.isEmpty()) {
            throw new IllegalStateException(
                "No verified primary bank account found. Settlements are blocked until a bank account is added and verified."
            );
        }
        
        return true;
    }
    
    // ========================================================================
    // HELPER METHODS (private)
    // ========================================================================
    
    /**
     * Check if user has access to business profile
     * 
     * @param businessProfileId business profile ID
     * @param userId user ID
     * @return true if user has access
     */
    private boolean hasAccessToBusinessProfile(Long businessProfileId, Long userId) {
        return roleRepository.existsActiveRoleByUserAndBusinessProfile(userId, businessProfileId);
    }
    
    /**
     * Check if user has FINANCE permission or higher (OWNER, ADMIN, FINANCE)
     * 
     * @param businessProfileId business profile ID
     * @param userId user ID
     * @return true if user has finance permission
     */
    private boolean hasFinancePermission(Long businessProfileId, Long userId) {
        Optional<BusinessProfileRole> role = roleRepository.findActiveRoleByUserAndBusinessProfile(userId, businessProfileId);
        
        return role.isPresent() && role.get().getRole().canManageFinances();
    }
    
    /**
     * Convert BankDetails entity to DTO with masked account number
     * 
     * @param bankDetails bank details entity
     * @return bank details DTO
     */
    private BankDetailsDTO convertToDTO(BankDetails bankDetails) {
        BankDetailsDTO dto = new BankDetailsDTO();
        
        dto.setId(bankDetails.getId());
        dto.setBusinessProfileId(bankDetails.getBusinessProfile().getId());
        dto.setAccountHolderName(bankDetails.getAccountHolderName());
        dto.setMaskedAccountNumber(bankDetails.getMaskedAccountNumber());  // Uses entity method
        dto.setIfscCode(bankDetails.getIfscCode());
        dto.setBankName(bankDetails.getBankName());
        dto.setBranchName(bankDetails.getBranchName());
        dto.setIsPrimary(bankDetails.isPrimary());
        dto.setBankVerificationStatus(bankDetails.getBankVerificationStatus());
        dto.setLastVerifiedAt(bankDetails.getLastVerifiedAt());
        dto.setCreatedAt(bankDetails.getCreatedAt());
        dto.setUpdatedAt(bankDetails.getUpdatedAt());
        
        // Calculate derived fields
        dto.setCanUseForSettlement(bankDetails.canUseForSettlement());
        dto.setStatusDisplayColor(getStatusColor(bankDetails.getBankVerificationStatus()));
        
        return dto;
    }
    
    /**
     * Get UI display color for bank verification status
     * 
     * @param status verification status
     * @return color code
     */
    private String getStatusColor(BankVerificationStatus status) {
        switch (status) {
            case VERIFIED: return "success";
            case FAILED: return "danger";
            case UNVERIFIED:
            default: return "warning";
        }
    }
}
