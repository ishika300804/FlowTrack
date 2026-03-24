package com.example.IMS.service;

import com.example.IMS.context.ActiveBusinessProfileContext;
import com.example.IMS.dto.BusinessProfileDTO;
import com.example.IMS.dto.CreateBusinessProfileRequest;
import com.example.IMS.dto.verification.VerificationResult;
import com.example.IMS.model.enums.BusinessRole;
import com.example.IMS.model.enums.OnboardingStage;
import com.example.IMS.model.enums.VerificationStatus;
import com.example.IMS.model.enums.VerificationType;
import com.example.IMS.model.BusinessProfile;
import com.example.IMS.model.BusinessProfileRole;
import com.example.IMS.model.User;
import com.example.IMS.repository.BusinessProfileRepository;
import com.example.IMS.repository.BusinessProfileRoleRepository;
import com.example.IMS.repository.IUserRepository;
import com.example.IMS.service.verification.VerificationOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for Business Profile management
 * 
 * ENFORCEMENT RULES:
 * 1. verification_status != VERIFIED → block settlements
 * 2. onboarding_stage != ACTIVE → block inventory operations
 * 3. All operations must enforce tenant isolation (user can only access their businesses)
 * 4. Role-based access control for multi-user businesses
 */
@Service
@Transactional
public class BusinessProfileService {
    
    private static final Logger logger = LoggerFactory.getLogger(BusinessProfileService.class);
    
    @Autowired
    private BusinessProfileRepository businessProfileRepository;
    
    @Autowired
    private BusinessProfileRoleRepository roleRepository;
    
    @Autowired
    private IUserRepository userRepository;
    
    @Autowired
    private ActiveBusinessProfileContext activeContext;
    
    @Autowired
    private VerificationOrchestrator verificationOrchestrator;
    
    // ========================================================================
    // CREATE OPERATIONS
    // ========================================================================
    
    /**
     * Create a new business profile
     * Automatically grants OWNER role to creator
     * 
     * @param request business profile creation request
     * @param userId user creating the profile
     * @return created business profile DTO
     * @throws IllegalArgumentException if GSTIN already exists
     */
    public BusinessProfileDTO createBusinessProfile(CreateBusinessProfileRequest request, Long userId) {
        logger.info("Creating business profile for user: {}", userId);
        
        // Validate GSTIN uniqueness
        if (businessProfileRepository.findByGstin(request.getGstin()).isPresent()) {
            throw new IllegalArgumentException("Business with this GSTIN already exists");
        }
        
        // Get user entity
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        
        // Create business profile entity
        BusinessProfile profile = new BusinessProfile();
        profile.setUser(user);
        profile.setLegalBusinessName(request.getLegalBusinessName());
        profile.setBusinessType(request.getBusinessType());
        profile.setGstin(request.getGstin());
        profile.setPanNumber(request.getPanNumber());  // Will be encrypted by converter
        profile.setCinNumber(request.getCinNumber());
        profile.setUdyamNumber(request.getUdyamNumber());
        profile.setRegisteredAddress(request.getRegisteredAddress());
        profile.setState(request.getState());
        profile.setPincode(request.getPincode());
        
        // Initial status
        profile.setVerificationStatus(VerificationStatus.DRAFT);
        profile.setOnboardingStage(null);  // Will progress through tiers
        
        // Save profile
        BusinessProfile savedProfile = businessProfileRepository.save(profile);
        logger.info("Created business profile: {} with ID: {}", savedProfile.getLegalBusinessName(), savedProfile.getId());
        
        // Auto-grant OWNER role (handled by V1_2 migration trigger, but also create explicitly)
        BusinessProfileRole ownerRole = new BusinessProfileRole();
        ownerRole.setBusinessProfile(savedProfile);
        ownerRole.setUser(user);
        ownerRole.setRole(BusinessRole.OWNER);
        ownerRole.setIsActive(true);
        ownerRole.setNotes("Auto-granted during profile creation");
        roleRepository.save(ownerRole);
        
        // Set as active business profile
        activeContext.setActiveBusinessProfile(savedProfile.getId(), savedProfile.getLegalBusinessName(), userId);
        
        // Trigger automatic verifications (GST, PAN, CIN) if enabled
        try {
            Map<VerificationType, VerificationResult> verificationResults = 
                verificationOrchestrator.autoTriggerVerifications(savedProfile.getId());
            
            if (!verificationResults.isEmpty()) {
                logger.info("Auto-triggered {} verifications for business profile {}", 
                    verificationResults.size(), savedProfile.getId());
            }
        } catch (Exception e) {
            // Log error but don't fail profile creation
            logger.error("Failed to auto-trigger verifications for business profile {}", 
                savedProfile.getId(), e);
        }
        
        return convertToDTO(savedProfile, userId);
    }
    
    // ========================================================================
    // READ OPERATIONS (with tenant isolation)
    // ========================================================================
    
    /**
     * Get business profile by ID with tenant isolation
     * 
     * @param profileId business profile ID
     * @param userId user requesting the profile
     * @return business profile DTO
     * @throws AccessDeniedException if user doesn't have access
     */
    public BusinessProfileDTO getBusinessProfile(Long profileId, Long userId) {
        BusinessProfile profile = getBusinessProfileWithAccessCheck(profileId, userId);
        return convertToDTO(profile, userId);
    }
    
    /**
     * Get all business profiles accessible to a user
     * Includes businesses where user is OWNER or has been granted a role
     * 
     * @param userId user ID
     * @return list of business profiles
     */
    public List<BusinessProfileDTO> getAccessibleBusinessProfiles(Long userId) {
        // Get all roles for this user
        List<BusinessProfileRole> roles = roleRepository.findActiveRolesByUser(userId);
        
        return roles.stream()
            .map(role -> convertToDTO(role.getBusinessProfile(), userId))
            .collect(Collectors.toList());
    }
    
    /**
     * Get currently active business profile from session context
     * 
     * @param userId user ID (for validation)
     * @return active business profile DTO
     * @throws IllegalStateException if no active profile
     */
    public BusinessProfileDTO getActiveBusinessProfile(Long userId) {
        if (!activeContext.hasActiveBusinessProfile()) {
            throw new IllegalStateException("No active business profile selected");
        }
        
        activeContext.validateUser(userId);
        Long profileId = activeContext.getActiveBusinessProfileId();
        
        return getBusinessProfile(profileId, userId);
    }
    
    // ========================================================================
    // UPDATE OPERATIONS (with role-based authorization)
    // ========================================================================
    
    /**
     * Update business profile
     * Requires OWNER or ADMIN role
     * 
     * @param profileId business profile ID
     * @param request update request
     * @param userId user performing update
     * @return updated business profile DTO
     * @throws AccessDeniedException if user lacks permission
     */
    public BusinessProfileDTO updateBusinessProfile(Long profileId, CreateBusinessProfileRequest request, Long userId) {
        BusinessProfile profile = getBusinessProfileWithAccessCheck(profileId, userId);
        
        // Check permission: requires OWNER or ADMIN
        if (!hasRoleWithModifyPermission(profileId, userId)) {
            throw new AccessDeniedException("You do not have permission to modify this business profile");
        }
        
        // Update allowed fields
        profile.setLegalBusinessName(request.getLegalBusinessName());
        profile.setRegisteredAddress(request.getRegisteredAddress());
        profile.setState(request.getState());
        profile.setPincode(request.getPincode());
        
        // Note: GSTIN, PAN, CIN, Udyam cannot be modified after creation (regulatory requirement)
        // If user needs to change these, they should create a new business profile
        
        BusinessProfile updated = businessProfileRepository.save(profile);
        logger.info("Updated business profile: {}", updated.getId());
        
        return convertToDTO(updated, userId);
    }

    /**
     * Submit a DRAFT or REJECTED business profile for verification (DRAFT/REJECTED → PENDING)
     *
     * @param profileId business profile to submit
     * @param userId    user making the request (must be OWNER or ADMIN of the profile)
     * @throws IllegalStateException if the profile is not in a submittable state
     */
    public void submitForVerification(Long profileId, Long userId) {
        BusinessProfile profile = getBusinessProfileWithAccessCheck(profileId, userId);

        if (profile.getVerificationStatus() != VerificationStatus.DRAFT
                && profile.getVerificationStatus() != VerificationStatus.REJECTED) {
            throw new IllegalStateException(
                    "Only DRAFT or REJECTED profiles can be submitted for verification. "
                            + "Current status: " + profile.getVerificationStatus());
        }

        profile.setVerificationStatus(VerificationStatus.PENDING);
        businessProfileRepository.save(profile);
        logger.info("Business profile {} submitted for verification by user {}", profileId, userId);
    }
     /** User must have access to the target profile
     * 
     * @param profileId target business profile ID
     * @param userId user switching profile
     */
    public void switchActiveProfile(Long profileId, Long userId) {
        BusinessProfile profile = getBusinessProfileWithAccessCheck(profileId, userId);
        
        activeContext.setActiveBusinessProfile(
            profile.getId(),
            profile.getLegalBusinessName(),
            userId
        );
        
        logger.info("User {} switched to business profile: {}", userId, profile.getId());
    }
    
    // ========================================================================
    // ENFORCEMENT LOGIC (Critical Security Methods)
    // ========================================================================
    
    /**
     * Check if user can perform financial transactions/settlements
     * ENFORCEMENT: verification_status must be VERIFIED
     * 
     * @param profileId business profile ID
     * @param userId user ID
     * @return true if user can transact
     * @throws AccessDeniedException if cannot transact
     */
    public boolean canTransact(Long profileId, Long userId) {
        BusinessProfile profile = getBusinessProfileWithAccessCheck(profileId, userId);
        
        if (profile.getVerificationStatus() != VerificationStatus.VERIFIED) {
            logger.warn("Transaction blocked - Business not verified. Profile: {}", profileId);
            throw new AccessDeniedException(
                "Business profile must be VERIFIED to perform transactions. Current status: " +
                profile.getVerificationStatus().getDisplayName()
            );
        }
        
        return true;
    }
    
    /**
     * Check if user can access inventory operations
     * ENFORCEMENT: onboarding_stage must be ACTIVE
     * 
     * @param profileId business profile ID
     * @param userId user ID
     * @return true if user can access inventory
     * @throws AccessDeniedException if cannot access inventory
     */
    public boolean canAccessInventory(Long profileId, Long userId) {
        BusinessProfile profile = getBusinessProfileWithAccessCheck(profileId, userId);
        
        if (profile.getOnboardingStage() != OnboardingStage.ACTIVE) {
            logger.warn("Inventory access blocked - Onboarding not complete. Profile: {}", profileId);
            throw new AccessDeniedException(
                "Complete business onboarding to access inventory. Current stage: " +
                (profile.getOnboardingStage() != null ? profile.getOnboardingStage().getDisplayName() : "Not Started")
            );
        }
        
        return true;
    }
    
    /**
     * Check if business profile can be used for operations
     * General check: verification completed + onboarding complete
     * 
     * @param profileId business profile ID
     * @param userId user ID
     * @return true if profile is fully operational
     */
    public boolean isOperational(Long profileId, Long userId) {
        BusinessProfile profile = getBusinessProfileWithAccessCheck(profileId, userId);
        return profile.canTransact();
    }
    
    // ========================================================================
    // VALIDATION METHODS
    // ========================================================================
    
    /**
     * Validate GSTIN format and uniqueness
     * 
     * @param gstin GSTIN to validate
     * @return true if valid and unique
     * @throws IllegalArgumentException if invalid or duplicate
     */
    public boolean validateGSTIN(String gstin) {
        // Format validation (already done by @Pattern in DTO, but double-check)
        if (!gstin.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")) {
            throw new IllegalArgumentException("Invalid GSTIN format");
        }
        
        // Uniqueness check
        if (businessProfileRepository.findByGstin(gstin).isPresent()) {
            throw new IllegalArgumentException("GSTIN already registered");
        }
        
        return true;
    }
    
    // ========================================================================
    // HELPER METHODS (private)
    // ========================================================================
    
    /**
     * Get business profile with tenant isolation check
     * Verifies user has access to the profile
     * 
     * @param profileId business profile ID
     * @param userId user requesting access
     * @return business profile entity
     * @throws EntityNotFoundException if profile doesn't exist
     * @throws AccessDeniedException if user doesn't have access
     */
    private BusinessProfile getBusinessProfileWithAccessCheck(Long profileId, Long userId) {
        // Check if user has any role for this business
        if (!roleRepository.existsActiveRoleByUserAndBusinessProfile(userId, profileId)) {
            logger.warn("Access denied - User {} attempted to access profile {}", userId, profileId);
            throw new AccessDeniedException("You do not have access to this business profile");
        }
        
        return businessProfileRepository.findById(profileId)
            .orElseThrow(() -> new EntityNotFoundException("Business profile not found: " + profileId));
    }
    
    /**
     * Check if user has permission to modify business details
     * Requires OWNER or ADMIN role
     * 
     * @param profileId business profile ID
     * @param userId user ID
     * @return true if user can modify
     */
    private boolean hasRoleWithModifyPermission(Long profileId, Long userId) {
        Optional<BusinessProfileRole> role = roleRepository.findActiveRoleByUserAndBusinessProfile(userId, profileId);
        
        return role.isPresent() && role.get().getRole().canModifyBusinessDetails();
    }
    
    /**
     * Convert BusinessProfile entity to DTO with user-specific permissions
     * 
     * @param profile business profile entity
     * @param userId user ID (for permission calculation)
     * @return business profile DTO
     */
    private BusinessProfileDTO convertToDTO(BusinessProfile profile, Long userId) {
        BusinessProfileDTO dto = new BusinessProfileDTO();
        
        dto.setId(profile.getId());
        dto.setLegalBusinessName(profile.getLegalBusinessName());
        dto.setBusinessType(profile.getBusinessType());
        dto.setGstin(profile.getGstin());
        dto.setMaskedPanNumber(maskPAN(profile.getPanNumber()));
        dto.setCinNumber(profile.getCinNumber());
        dto.setUdyamNumber(profile.getUdyamNumber());
        dto.setRegisteredAddress(profile.getRegisteredAddress());
        dto.setState(profile.getState());
        dto.setPincode(profile.getPincode());
        dto.setVerificationStatus(profile.getVerificationStatus());
        dto.setOnboardingStage(profile.getOnboardingStage());
        dto.setCreatedAt(profile.getCreatedAt());
        dto.setUpdatedAt(profile.getUpdatedAt());
        
        dto.setUserId(profile.getUser().getId());
        dto.setUserName(profile.getUser().getUsername());
        
        // Calculate statistics
        dto.setTotalBankAccounts(profile.getBankAccounts().size());
        dto.setVerifiedBankAccounts((int) profile.getBankAccounts().stream()
            .filter(bd -> bd.getBankVerificationStatus() == com.example.IMS.model.enums.BankVerificationStatus.VERIFIED)
            .count());
        dto.setHasActivePrimaryAccount(profile.getBankAccounts().stream()
            .anyMatch(bd -> bd.isPrimary() && 
                           bd.getBankVerificationStatus() == com.example.IMS.model.enums.BankVerificationStatus.VERIFIED));
        
        // Get user's role and permissions
        Optional<BusinessProfileRole> userRole = roleRepository.findActiveRoleByUserAndBusinessProfile(userId, profile.getId());
        if (userRole.isPresent()) {
            dto.setUserRole(userRole.get().getRole().getDisplayName());
            dto.setCanEdit(userRole.get().getRole().canModifyBusinessDetails());
            dto.setCanDelete(userRole.get().getRole().canDeleteBusiness());
            dto.setCanManageTeam(userRole.get().getRole().canManageRole(BusinessRole.FINANCE));
        }
        
        return dto;
    }
    
    /**
     * Mask PAN number for display
     * Format: ABCDE****F
     * 
     * @param pan PAN number (decrypted)
     * @return masked PAN
     */
    private String maskPAN(String pan) {
        if (pan == null || pan.length() != 10) {
            return "****";
        }
        return pan.substring(0, 5) + "****" + pan.substring(9);
    }
    
    // ========================================================================
    // VERIFICATION OPERATIONS (Manual Retry)
    // ========================================================================
    
    /**
     * Manually trigger GST verification for a business profile
     * Can be used to retry failed verifications or re-verify after GSTIN update
     * 
     * @param profileId business profile ID
     * @param userId user requesting verification
     * @return verification result
     * @throws AccessDeniedException if user doesn't have access
     */
    public VerificationResult verifyGst(Long profileId, Long userId) {
        // Verify user has access to this profile
        getBusinessProfileWithAccessCheck(profileId, userId);
        
        logger.info("User {} manually triggered GST verification for business profile {}", userId, profileId);
        return verificationOrchestrator.retryVerification(profileId, VerificationType.GST);
    }
    
    /**
     * Manually trigger PAN verification for a business profile
     * Can be used to retry failed verifications or re-verify after PAN update
     * 
     * @param profileId business profile ID
     * @param userId user requesting verification
     * @return verification result
     * @throws AccessDeniedException if user doesn't have access
     */
    public VerificationResult verifyPan(Long profileId, Long userId) {
        // Verify user has access to this profile
        getBusinessProfileWithAccessCheck(profileId, userId);
        
        logger.info("User {} manually triggered PAN verification for business profile {}", userId, profileId);
        return verificationOrchestrator.retryVerification(profileId, VerificationType.PAN);
    }
    
    /**
     * Manually trigger CIN verification for a business profile
     * Can be used to retry failed verifications or re-verify after CIN update
     * 
     * @param profileId business profile ID
     * @param userId user requesting verification
     * @return verification result
     * @throws AccessDeniedException if user doesn't have access
     */
    public VerificationResult verifyCin(Long profileId, Long userId) {
        // Verify user has access to this profile
        getBusinessProfileWithAccessCheck(profileId, userId);
        
        logger.info("User {} manually triggered CIN verification for business profile {}", userId, profileId);
        return verificationOrchestrator.retryVerification(profileId, VerificationType.CIN);
    }
}
