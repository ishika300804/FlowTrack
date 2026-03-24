# FlowTrack Business Onboarding - Implementation Tracking

**Last Updated**: 2026-02-25  
**Current Phase**: Completing Prompt 1  
**Next Phase**: Prompt 2 (WebClient Integration)

---

## 📋 PROMPT 1: Schema Evolution for Business Onboarding

### ✅ COMPLETED

#### Database Layer
- [x] **V1_0__baseline.sql** - Baseline migration with 15 existing tables
- [x] **V1_1__business_profile_extension.sql** - Three new tables:
  - `business_profiles` (with indexes on gstin, pan_number, user_id)
  - `bank_details` (with primary account enforcement triggers)
  - `verification_logs` (with JSON payload columns)
- [x] **Foreign Keys** - All with ON DELETE CASCADE
- [x] **Indexes** - gstin, pan_number, user_id, verification_status
- [x] **Triggers** - Primary bank account enforcement
- [x] **Flyway Configuration** - Added to pom.xml and application.properties
- [x] **Backward Compatibility** - No existing tables modified

#### Entity Layer
- [x] **BusinessProfile.java** - Complete with validation, relationships, encryption
- [x] **BankDetails.java** - Encrypted account numbers, primary designation
- [x] **VerificationLog.java** - Audit trail with JSON storage

#### Enums (All 5)
- [x] **BusinessType.java** - PROPRIETORSHIP, LLP, PRIVATE_LIMITED, PARTNERSHIP
- [x] **VerificationStatus.java** - DRAFT, PENDING, VERIFIED, REJECTED
- [x] **OnboardingStage.java** - TIER1_COMPLETE, TIER2_COMPLETE, ACTIVE
- [x] **BankVerificationStatus.java** - UNVERIFIED, VERIFIED, FAILED
- [x] **VerificationType.java** - GST, PAN, BANK, CIN, UDYAM

#### Repository Layer
- [x] **BusinessProfileRepository.java** - Tenant-scoped queries
- [x] **BankDetailsRepository.java** - Primary account management
- [x] **VerificationLogRepository.java** - Audit trail queries

#### Context & Utilities
- [x] **ActiveBusinessProfileContext.java** - Session-scoped multi-tenancy
- [x] **SensitiveDataEncryptionConverter.java** - AES encryption (⚠️ ECB mode - needs upgrade)

#### Documentation
- [x] **BUSINESS_PROFILE_IMPLEMENTATION.md** - Technical implementation summary
- [x] **BUSINESS_PROFILE_DEV_GUIDE.md** - Developer quick reference
- [x] **BUSINESS_PROFILE_SQL_GUIDE.md** - Migration procedures, SQL operations
- [x] **BUSINESS_PROFILE_SECURITY_GUIDE.md** - Security, compliance, DPDP Act

#### Build Validation
- [x] **Maven Compilation** - BUILD SUCCESS (95 source files compiled)
- [x] **No Breaking Changes** - Existing code untouched

---

### 🚧 IN PROGRESS (Critical Gaps from Prompt 1)

#### Service Layer with Enforcement (HIGH PRIORITY)
- [ ] **BusinessProfileService.java**
  - [ ] createBusinessProfile() - with validation
  - [ ] updateBusinessProfile() - with authorization
  - [ ] switchActiveProfile() - with context update
  - [ ] validateGSTIN() - format and uniqueness
  - [ ] canTransact() - **enforcement: verification_status == VERIFIED**
  - [ ] canAccessInventory() - **enforcement: onboarding_stage == ACTIVE**
  
- [ ] **BankDetailsService.java**
  - [ ] addBankAccount() - with profile ownership check
  - [ ] setPrimaryAccount() - atomic operation
  - [ ] verifyBankAccount() - external API integration hook
  - [ ] getPrimaryVerifiedAccount() - settlement account retrieval
  - [ ] **enforcement: block settlements if no verified primary account**

- [ ] **VerificationService.java**
  - [ ] logVerificationAttempt() - audit trail
  - [ ] recordVerificationResult() - update entity + log
  - [ ] getVerificationHistory() - for compliance
  - [ ] **enforcement: rate limiting on verification attempts**

#### Encryption Upgrade (CRITICAL SECURITY)
- [ ] **Upgrade SensitiveDataEncryptionConverter**
  - [ ] Change from AES/ECB → AES/GCM
  - [ ] Implement IV (Initialization Vector) randomization
  - [ ] Add authentication tag validation
  - [ ] Update database storage format (prepend IV to ciphertext)
  - [ ] **Must complete before external API integration**

#### Business-Level RBAC (Multi-User Management)
- [ ] **V1_2__business_profile_roles.sql** - New migration
  - [ ] Create `business_profile_roles` table
  - [ ] Fields: id, business_profile_id, user_id, role, granted_at, granted_by
  - [ ] Unique constraint: (user_id, business_profile_id)
  - [ ] Foreign keys with CASCADE

- [ ] **BusinessProfileRole.java** - Entity
- [ ] **BusinessRole.java** - Enum (OWNER, ADMIN, FINANCE, OPERATIONS, AUDITOR)
- [ ] **BusinessProfileRoleRepository.java** - Repository
- [ ] **Permission checking logic** - Annotation-based or manual

#### DTOs (Data Transfer Objects)
- [ ] **BusinessProfileDTO.java** - API responses (no sensitive data)
- [ ] **BankDetailsDTO.java** - Masked account numbers
- [ ] **VerificationLogDTO.java** - Sanitized payloads
- [ ] **CreateBusinessProfileRequest.java**
- [ ] **UpdateBusinessProfileRequest.java**
- [ ] **AddBankAccountRequest.java**

---

## 📋 PROMPT 2: WebClient + Validation Integration

### ⏳ NOT STARTED (Next Phase)

#### Strategy Pattern Design
- [ ] **VerificationProvider.java** - Interface
- [ ] **VerificationRequest.java** - Common request DTO
- [ ] **VerificationResult.java** - Common response DTO

#### Verification Service Implementations
- [ ] **GstVerificationService.java**
  - [ ] WebClient integration
  - [ ] GSTN API endpoint configuration
  - [ ] Response parsing
  - [ ] Error handling (4xx, 5xx)
  - [ ] Retry with exponential backoff
  - [ ] Logging to verification_logs table

- [ ] **PanVerificationService.java**
  - [ ] NSDL/Protean API integration
  - [ ] PAN format validation
  - [ ] Name matching logic

- [ ] **BankVerificationService.java**
  - [ ] Penny drop integration (Razorpay/Cashfree)
  - [ ] Account holder name matching
  - [ ] IFSC validation

- [ ] **CinVerificationService.java**
  - [ ] MCA API integration
  - [ ] Director validation

#### WebClient Configuration
- [ ] **WebClientConfig.java** - Global bean configuration
  - [ ] Connection timeout: 10s
  - [ ] Read timeout: 30s
  - [ ] Connection pool size: 50
  - [ ] Retry filter with exponential backoff

#### Properties Externalization
- [ ] **application.properties** entries:
  ```properties
  verification.gst.api.url=
  verification.gst.api.key=
  verification.pan.api.url=
  verification.pan.api.key=
  verification.bank.api.url=
  verification.bank.api.key=
  verification.cin.api.url=
  verification.cin.api.key=
  ```

#### Error Handling
- [ ] **VerificationException.java** - Custom exception
- [ ] **VerificationErrorHandler.java** - Global handler
- [ ] **Logging strategy** - Mask sensitive data

---

## 📋 PROMPT 3: Testing Focused

### ⏳ NOT STARTED

#### Unit Tests
- [ ] **BusinessProfileServiceTest.java**
  - [ ] Test createBusinessProfile()
  - [ ] Test updateBusinessProfile()
  - [ ] Test enforcement: canTransact()
  - [ ] Test enforcement: canAccessInventory()
  - [ ] Test authorization (user can't update other's profile)

- [ ] **BankDetailsServiceTest.java**
  - [ ] Test addBankAccount()
  - [ ] Test setPrimaryAccount() - atomic operation
  - [ ] Test primary account enforcement (only one primary)

- [ ] **VerificationServiceTest.java**
  - [ ] Test logVerificationAttempt()
  - [ ] Test recordVerificationResult()

- [ ] **SensitiveDataEncryptionConverterTest.java**
  - [ ] Test encryption/decryption roundtrip
  - [ ] Test null handling
  - [ ] Test empty string handling
  - [ ] Test IV randomization (after GCM upgrade)

#### Integration Tests
- [ ] **BusinessOnboardingFlowTest.java**
  - [ ] Full flow: Create profile → Add bank → Verify GST → Verify PAN → Verify Bank → ACTIVE
  - [ ] Mock WebClient responses
  - [ ] Verify database persistence
  - [ ] Verify audit logs

- [ ] **VerificationApiIntegrationTest.java**
  - [ ] Mock GST API success/failure
  - [ ] Mock PAN API success/failure
  - [ ] Mock Bank API success/failure
  - [ ] Test retry logic
  - [ ] Test timeout handling

#### Security Tests
- [ ] **BusinessProfileSecurityTest.java**
  - [ ] Test unauthorized user cannot access profile
  - [ ] Test unauthorized user cannot update profile
  - [ ] Test tenant isolation (user A can't see user B's profiles)

- [ ] **InventoryAccessControlTest.java**
  - [ ] Test: onboarding_stage != ACTIVE → block inventory
  - [ ] Test: onboarding_stage == ACTIVE → allow inventory

#### Validation Tests
- [ ] **BusinessProfileValidationTest.java**
  - [ ] Invalid GSTIN format
  - [ ] Invalid PAN format
  - [ ] Invalid IFSC format
  - [ ] Duplicate GSTIN
  - [ ] Missing required fields

#### Test Configuration
- [ ] **TestContainers** - MySQL container for integration tests (optional)
- [ ] **WireMock** - Mock external APIs
- [ ] **@SpringBootTest** configuration
- [ ] **Test profile** (application-test.properties)

---

## 📋 PROMPT 4: Staged Rollout Strategy

### ⏳ NOT STARTED

#### Feature Flags
- [ ] **FeatureToggleService.java**
  - [ ] ONBOARDING_ENABLED (Phase 1)
  - [ ] ONBOARDING_SOFT_ENFORCEMENT (Phase 2)
  - [ ] ONBOARDING_HARD_ENFORCEMENT (Phase 3)

- [ ] **application.properties** entries:
  ```properties
  feature.onboarding.enabled=true
  feature.onboarding.soft-enforcement=false
  feature.onboarding.hard-enforcement=false
  ```

#### Phase 1: Schema Addition (Zero Impact)
- [ ] Deploy V1_1 and V1_2 migrations
- [ ] No enforcement
- [ ] Existing users continue as-is
- [ ] **Rollback plan**: Drop new tables (no data loss for existing workflows)

#### Phase 2: Soft Enforcement (Banner/Notification)
- [ ] **OnboardingBannerInterceptor.java**
  - [ ] Show banner: "Complete your business profile"
  - [ ] Allow dismissal
  - [ ] Track dismissal count in session

- [ ] **Dashboard widget** - "Complete onboarding to unlock features"
- [ ] **Email reminders** - Weekly nudge for incomplete profiles
- [ ] **Metrics** - Track conversion rate

#### Phase 3: Hard Enforcement (Conditional Blocking)
- [ ] **OnboardingEnforcementInterceptor.java**
  - [ ] Block inventory operations if onboarding_stage != ACTIVE
  - [ ] Block settlement operations if verification_status != VERIFIED
  - [ ] Allow read-only access to existing data

- [ ] **Grace period** - 30 days before enforcement
- [ ] **Override mechanism** - Admin can grant temporary exemptions

#### Phase 4: Admin Dashboard
- [ ] **Legacy user report** - List users without business profiles
- [ ] **Onboarding status dashboard** - Conversion funnel
- [ ] **Manual approval interface** - Admins review pending verifications

#### Migration Sequencing
1. Deploy Phase 1 (migrations only)
2. Monitor for 1 week (no errors)
3. Deploy Phase 2 (soft enforcement) - 2 weeks
4. Analyze metrics (onboarding completion rate)
5. Deploy Phase 3 (hard enforcement) - with grace period
6. Deploy Phase 4 (admin tools)

#### Rollback Plan
- [ ] **Feature flag disable** - Instant rollback without code deployment
- [ ] **Database rollback** - Script to drop new tables
- [ ] **Backward compatibility matrix** - Document safe rollback points

#### Zero Downtime Deployment
- [ ] Blue-green deployment strategy
- [ ] Database migrations run separately (before app deployment)
- [ ] Health check endpoints updated

---

## 📋 PROMPT 5: Risk Minimization Production Checklist

### ⏳ NOT STARTED

#### Security Checklist
- [ ] ✅ Encryption validation (AES/GCM implemented)
- [ ] ✅ Masking validation (PAN, account numbers never logged)
- [ ] ✅ HTTPS enforcement (verify in production)
- [ ] ✅ Role misconfiguration detection (automated tests)
- [ ] ✅ API key rotation policy documented
- [ ] ✅ Secrets manager integration (AWS SSM/Azure Key Vault)

#### Performance Checklist
- [ ] API rate limiting implemented (per user, per IP)
- [ ] WebClient connection pool tuned (50 connections)
- [ ] Database index validation (EXPLAIN queries run)
- [ ] Query performance benchmarks (< 100ms for profile retrieval)
- [ ] Load testing results documented (1000 concurrent users)

#### Compliance Checklist
- [ ] Data retention policy implemented (verification_logs archived after 1 year)
- [ ] Audit logging completeness verified (all CRUD operations logged)
- [ ] Sensitive data masking in logs verified (no PAN/account numbers)
- [ ] DPDP Act compliance:
  - [ ] Data export endpoint tested
  - [ ] Data deletion endpoint tested
  - [ ] Privacy policy updated
  - [ ] Data breach notification plan documented

#### Monitoring Checklist
- [ ] **Micrometer metrics** added:
  - [ ] `business_profile.created.count`
  - [ ] `verification.attempts.count` (by type)
  - [ ] `verification.success.rate` (by type)
  - [ ] `onboarding.completion.rate`

- [ ] **Alerting** configured:
  - [ ] Verification failure rate > 50% → Alert
  - [ ] External API downtime → Alert
  - [ ] Encryption errors → Critical alert

- [ ] **Health checks**:
  - [ ] GST API health endpoint
  - [ ] PAN API health endpoint
  - [ ] Bank API health endpoint

#### Rollback Checklist
- [ ] Rollback DB script tested (V1_2 → V1_1 → V1_0)
- [ ] Feature flag disable tested (zero downtime)
- [ ] Data backup strategy (automated daily backups)

#### Stress Testing Checklist
- [ ] Concurrent onboarding submissions (100 users simultaneously)
- [ ] External API downtime simulation (circuit breaker tested)
- [ ] Database connection pool exhaustion tested
- [ ] Race condition testing (two users setting primary bank simultaneously)

---

## 🎯 CRITICAL PATH (Before External API Integration)

### Must Complete (In Order):
1. ✅ **Encryption Upgrade** - AES/ECB → AES/GCM (BLOCKER)
2. ✅ **Service Layer** - Enforcement logic implemented
3. ✅ **Business RBAC** - Multi-user management
4. ✅ **DTOs** - Sanitized API responses
5. → **WebClient Config** (Prompt 2)
6. → **Verification Services** (Prompt 2)
7. → **Integration Tests** (Prompt 3)
8. → **Feature Flags** (Prompt 4)

---

## 📊 Progress Summary

| Prompt | Status | Completion | Blockers |
|--------|--------|------------|----------|
| **Prompt 1** | 🟡 In Progress | 75% | Encryption upgrade, Service layer, RBAC |
| **Prompt 2** | ⚪ Not Started | 0% | Prompt 1 must complete first |
| **Prompt 3** | ⚪ Not Started | 0% | Prompt 2 must complete first |
| **Prompt 4** | ⚪ Not Started | 0% | Prompt 3 must complete first |
| **Prompt 5** | ⚪ Not Started | 0% | Continuous (runs in parallel with 1-4) |

---

## 🔐 Security Posture

| Component | Current State | Target State | Priority |
|-----------|---------------|--------------|----------|
| Encryption Mode | AES/ECB ⚠️ | AES/GCM ✅ | **CRITICAL** |
| Tenant Isolation | Implemented ✅ | Implemented ✅ | Done |
| Business RBAC | Not Implemented ❌ | Implemented ✅ | High |
| Audit Logging | Partial ⚠️ | Complete ✅ | Medium |
| Rate Limiting | Not Implemented ❌ | Implemented ✅ | High |
| DPDP Compliance | Documented ✅ | Tested ✅ | Medium |

---

## 📝 Notes for Next Implementation Session

### When Implementing Prompt 2 (WebClient Integration):
- **DO NOT** recreate entities, enums, repositories (already done)
- **DO NOT** recreate encryption converter (will be upgraded in this session)
- **DO** focus on verification service implementations
- **DO** use existing `VerificationLog` entity for logging

### When Implementing Prompt 3 (Testing):
- **DO NOT** test entity validation (already covered by JPA annotations)
- **DO** focus on service layer enforcement logic
- **DO** mock WebClient responses (no real API calls)

### When Implementing Prompt 4 (Rollout):
- **DO NOT** recreate migration files
- **DO** focus on feature flags and interceptors

### When Implementing Prompt 5 (Checklist):
- **DO** integrate Micrometer (not yet in pom.xml)
- **DO** add Spring Boot Actuator endpoints

---

**Last Reviewed**: 2026-02-25  
**Next Review**: After completing Prompt 1 gaps  
**Owner**: Development Team
