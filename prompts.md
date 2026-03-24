1. Schema Evolution for Business Onboarding
"We are evolving an existing Spring Boot 2.5.1 + MySQL application called FlowTrack.

The system already contains:
- User entity
- Role entity
- Vendor entity (used by inventory)
- Loan, Item, Borrower entities
- JPA/Hibernate ORM
- Spring Security with RBAC

We must extend the schema without breaking existing tables or relationships.

DO NOT:
- Drop existing tables
- Modify primary keys
- Change current relationships
- Break foreign key constraints

We are introducing structured business onboarding.

Create migration scripts using Flyway (V1_1__business_profile_extension.sql).

Add the following tables:

1️⃣ business_profile
- id (PK)
- user_id (FK to users)
- legal_business_name
- business_type ENUM (PROPRIETORSHIP, LLP, PRIVATE_LIMITED, PARTNERSHIP)
- gstin
- pan_number (encrypted storage column)
- cin_number (nullable)
- udyam_number (nullable)
- registered_address
- state
- pincode
- verification_status ENUM (DRAFT, PENDING, VERIFIED, REJECTED)
- onboarding_stage ENUM (TIER1_COMPLETE, TIER2_COMPLETE, ACTIVE)
- created_at
- updated_at

2️⃣ bank_details
- id (PK)
- business_profile_id (FK)
- account_holder_name
- account_number (encrypted)
- ifsc_code
- bank_verification_status ENUM (UNVERIFIED, VERIFIED, FAILED)
- last_verified_at

3️⃣ verification_log
- id (PK)
- business_profile_id (FK)
- verification_type ENUM (GST, PAN, BANK, CIN, UDYAM)
- request_payload (JSON)
- response_payload (JSON)
- verification_result
- created_at

Ensure:
- Index on gstin
- Index on pan_number
- Index on business_profile.user_id
- All FKs with ON DELETE CASCADE
- Backward compatibility maintained

Generate:
- Flyway migration file
- Updated JPA entities
- Enums
- Encryption converter placeholder class"

2. PROMPT 2 — WEBCLIENT + VALIDATION INTEGRATION
"We now need to integrate external validation services into FlowTrack.

The architecture is Spring Boot 2.5.1, using WebClient (NOT RestTemplate).

Create pluggable verification modules using Strategy Pattern.

Implement:

1️⃣ GstVerificationService
2️⃣ PanVerificationService
3️⃣ BankVerificationService
4️⃣ CinVerificationService

Design:

- Interface: VerificationProvider
- Method: VerificationResult verify(VerificationRequest request)

Each service must:
- Use WebClient
- Externalize API keys via application.properties
- Log every request to verification_log table
- Mask sensitive data
- Handle HTTP timeouts
- Implement retry with backoff
- Gracefully handle 4xx and 5xx responses

Do NOT hardcode API keys.

Add:
- Global WebClientConfig
- Timeout configuration
- Exception handler

Ensure:
- Services are transactional safe
- No blocking calls
- Compatible with current service layer
- No modification to existing inventory workflows

Provide:
- DTOs
- Request builders
- Error handling strategy
- Service wiring"

3. 🧪 PROMPT 3 — TESTING FOCUSED
"Now create comprehensive testing for the new onboarding system.

We need:

1️⃣ Unit tests for:
- BusinessProfileService
- Verification services
- Encryption converter

2️⃣ Integration tests:
- Full onboarding flow
- Mock WebClient responses
- Mock external API failure
- Database persistence verification

3️⃣ Security tests:
- Ensure unauthorized users cannot access onboarding endpoints
- Ensure inventory endpoints blocked if onboarding_stage != ACTIVE

4️⃣ Validation tests:
- Invalid GSTIN format
- Invalid PAN format
- Invalid IFSC
- Duplicate GSTIN

Use:
- JUnit 5
- Mockito
- @SpringBootTest
- Testcontainers (optional if helpful)

Ensure:
- Tests do not hit real external APIs
- Use WebClient mock
- High coverage for onboarding services

Do not modify existing test suite."

4. 🚀 PROMPT 4 — STAGED ROLLOUT STRATEGY
"Design a staged rollout strategy for the new onboarding system.

Context:
- Production system is currently live
- Existing users already exist
- Inventory workflows must not break

Design:

Phase 1:
- Add new schema
- No enforcement
- Allow existing users to continue

Phase 2:
- Soft enforcement (show banner: "Complete Business Profile")

Phase 3:
- Hard enforcement (block certain features until onboarding_stage == ACTIVE)

Phase 4:
- Admin dashboard for reviewing legacy accounts

Provide:
- Feature flag strategy
- DB migration sequencing
- Rollback plan
- Risk mitigation
- Zero downtime deployment steps
- Backward compatibility matrix

Do not suggest destructive schema changes."

5. 🛡 PROMPT 5 — RISK MINIMIZATION PRODUCTION CHECKLIST
"Generate a risk minimization checklist for FlowTrack onboarding system.

Cover:

1️⃣ Security
- Encryption validation
- Masking validation
- HTTPS enforcement
- Role misconfiguration detection

2️⃣ Performance
- API rate limiting
- WebClient connection pool tuning
- DB index validation

3️⃣ Compliance
- Data retention policy
- Audit logging completeness
- Sensitive data masking in logs

4️⃣ Monitoring
- Add Micrometer metrics
- Add alerting for verification failures
- Add health checks for external APIs

5️⃣ Rollback
- Rollback DB plan
- Feature flag disable strategy

6️⃣ Stress testing
- Concurrent onboarding submissions
- External API downtime simulation

Return checklist structured in actionable format."