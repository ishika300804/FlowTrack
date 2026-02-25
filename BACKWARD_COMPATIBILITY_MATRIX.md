# Backward Compatibility Matrix - FlowTrack Onboarding

## Overview

This document ensures that the business onboarding system maintains backward compatibility with existing FlowTrack functionality across all rollout phases.

---

## 🎯 Compatibility Principles

1. **No Breaking Changes**: Existing APIs and workflows continue to function
2. **Additive Only**: New features added, nothing removed
3. **Graceful Degradation**: System works even if onboarding incomplete
4. **Data Integrity**: Existing data never modified or deleted

---

## 📊 Compatibility Matrix

### Database Schema

| Component | Phase 1 | Phase 2 | Phase 3 | Phase 4 | Breaking Change? |
|-----------|---------|---------|---------|---------|------------------|
| **Existing Tables** |
| `users` | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ❌ No |
| `roles` | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ❌ No |
| `inventory_item` | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ❌ No |
| `vendor` | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ❌ No |
| `loan` | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ❌ No |
| `borrower` | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ❌ No |
| **New Tables** |
| `business_profiles` | ✅ Added | ✅ Used | ✅ Used | ✅ Used | ❌ No |
| `bank_details` | ✅ Added | ✅ Used | ✅ Used | ✅ Used | ❌ No |
| `verification_logs` | ✅ Added | ✅ Used | ✅ Used | ✅ Used | ❌ No |
| `business_profile_roles` | ✅ Added | ✅ Used | ✅ Used | ✅ Used | ❌ No |

**Verification:**
```sql
-- Verify no columns added to existing tables
SELECT TABLE_NAME, COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'ims' 
  AND TABLE_NAME IN ('users', 'roles', 'inventory_item', 'vendor', 'loan', 'borrower')
ORDER BY TABLE_NAME, ORDINAL_POSITION;

-- Compare with baseline schema
```

---

### API Endpoints

| Endpoint | Phase 1 | Phase 2 | Phase 3 | Phase 4 | Breaking Change? |
|----------|---------|---------|---------|---------|------------------|
| **Existing Endpoints** |
| `POST /login` | ✅ Works | ✅ Works | ✅ Works | ✅ Works | ❌ No |
| `POST /register` | ✅ Works | ✅ Works | ✅ Works | ✅ Works | ❌ No |
| `GET /items` | ✅ Works | ✅ Works | ⚠️ Conditional* | ⚠️ Conditional* | ⚠️ Conditional |
| `POST /items/add` | ✅ Works | ✅ Works | ⚠️ Conditional* | ⚠️ Conditional* | ⚠️ Conditional |
| `GET /vendors` | ✅ Works | ✅ Works | ⚠️ Conditional* | ⚠️ Conditional* | ⚠️ Conditional |
| `POST /loans/issue` | ✅ Works | ✅ Works | ⚠️ Conditional* | ⚠️ Conditional* | ⚠️ Conditional |
| **New Endpoints** |
| `POST /business-profile/create` | ✅ Added | ✅ Works | ✅ Works | ✅ Works | ❌ No |
| `GET /business-profile/status` | ✅ Added | ✅ Works | ✅ Works | ✅ Works | ❌ No |
| `POST /verification/gst` | ✅ Added | ✅ Works | ✅ Works | ✅ Works | ❌ No |
| `GET /admin/onboarding/dashboard` | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Added | ❌ No |

**\*Conditional Access:**
- Phase 3+: Requires `onboarding_stage = ACTIVE` (if hard enforcement enabled)
- Grace period applies to existing users
- Admin override available

**Verification:**
```bash
# Test existing endpoints still work
curl -X POST http://localhost:8087/login -d "username=admin&password=admin123"
curl -X GET http://localhost:8087/items -H "Cookie: JSESSIONID=..."

# Verify new endpoints accessible
curl -X GET http://localhost:8087/business-profile/status -H "Cookie: JSESSIONID=..."
```

---

### User Workflows

| Workflow | Phase 1 | Phase 2 | Phase 3 | Phase 4 | Breaking Change? |
|----------|---------|---------|---------|---------|------------------|
| **Existing Workflows** |
| Login | ✅ Works | ✅ Works | ✅ Works | ✅ Works | ❌ No |
| Registration | ✅ Works | ✅ Works | ✅ Works | ✅ Works | ❌ No |
| View inventory | ✅ Works | ✅ Works + Banner | ⚠️ Conditional* | ⚠️ Conditional* | ⚠️ Conditional |
| Add item | ✅ Works | ✅ Works + Banner | ⚠️ Conditional* | ⚠️ Conditional* | ⚠️ Conditional |
| Manage vendors | ✅ Works | ✅ Works + Banner | ⚠️ Conditional* | ⚠️ Conditional* | ⚠️ Conditional |
| Issue loan | ✅ Works | ✅ Works + Banner | ⚠️ Conditional* | ⚠️ Conditional* | ⚠️ Conditional |
| Chatbot | ✅ Works | ✅ Works | ✅ Works | ✅ Works | ❌ No |
| **New Workflows** |
| Create business profile | ✅ Available | ✅ Available | ✅ Available | ✅ Available | ❌ No |
| Submit for verification | ✅ Available | ✅ Available | ✅ Available | ✅ Available | ❌ No |
| Admin approval | ❌ N/A | ❌ N/A | ❌ N/A | ✅ Available | ❌ No |

**\*Conditional Access:**
- Phase 2: Banner shown but access allowed
- Phase 3: Access blocked if `onboarding_stage != ACTIVE` (after grace period)
- Existing users: 30-day grace period
- New users: No grace period

---

### User Roles

| Role | Phase 1 | Phase 2 | Phase 3 | Phase 4 | Breaking Change? |
|------|---------|---------|---------|---------|------------------|
| **Existing Roles** |
| `ROLE_ADMIN` | ✅ Works | ✅ Works | ✅ Works | ✅ Works | ❌ No |
| `ROLE_MANAGER` | ✅ Works | ✅ Works | ✅ Works | ✅ Works | ❌ No |
| `ROLE_STAFF` | ✅ Works | ✅ Works | ✅ Works | ✅ Works | ❌ No |
| `ROLE_USER` | ✅ Works | ✅ Works | ✅ Works | ✅ Works | ❌ No |
| **New Roles** |
| `ROLE_PLATFORM_ADMIN` | ✅ Added | ✅ Works | ✅ Works | ✅ Works | ❌ No |
| `ROLE_RETAILER` | ✅ Added | ✅ Works | ✅ Works | ✅ Works | ❌ No |
| `ROLE_VENDOR` | ✅ Added | ✅ Works | ✅ Works | ✅ Works | ❌ No |
| `ROLE_INVESTOR` | ✅ Added | ✅ Works | ✅ Works | ✅ Works | ❌ No |

**Note:** Old roles remain functional for backward compatibility. New users should use new roles.

---

### Security Configuration

| Security Feature | Phase 1 | Phase 2 | Phase 3 | Phase 4 | Breaking Change? |
|------------------|---------|---------|---------|---------|------------------|
| Authentication | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ❌ No |
| Authorization | ✅ Unchanged | ✅ Unchanged | ⚠️ Enhanced* | ⚠️ Enhanced* | ⚠️ Enhanced |
| Password encryption | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ❌ No |
| Session management | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ❌ No |
| CSRF protection | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ✅ Unchanged | ❌ No |

**\*Enhanced Authorization:**
- Phase 3+: Additional checks for onboarding status
- Existing authorization rules still apply
- New rules are additive, not replacing

---

### Data Migration

| Data Type | Phase 1 | Phase 2 | Phase 3 | Phase 4 | Data Loss Risk? |
|-----------|---------|---------|---------|---------|-----------------|
| User accounts | ✅ Preserved | ✅ Preserved | ✅ Preserved | ✅ Preserved | ❌ No |
| Inventory items | ✅ Preserved | ✅ Preserved | ✅ Preserved | ✅ Preserved | ❌ No |
| Vendors | ✅ Preserved | ✅ Preserved | ✅ Preserved | ✅ Preserved | ❌ No |
| Loans | ✅ Preserved | ✅ Preserved | ✅ Preserved | ✅ Preserved | ❌ No |
| Business profiles | ➕ New data | ➕ New data | ➕ New data | ➕ New data | ❌ No |

**Verification:**
```sql
-- Before migration
SELECT 'users' AS table_name, COUNT(*) AS before_count FROM users;
SELECT 'inventory_item' AS table_name, COUNT(*) AS before_count FROM inventory_item;

-- After migration
SELECT 'users' AS table_name, COUNT(*) AS after_count FROM users;
SELECT 'inventory_item' AS table_name, COUNT(*) AS after_count FROM inventory_item;

-- Counts should match
```

---

## 🔍 Testing Strategy

### Regression Testing

**Phase 1 (Schema Addition):**
```bash
# Test existing workflows
1. Login with existing user
2. View inventory list
3. Add new item
4. Edit existing item
5. Delete item
6. Manage vendors
7. Issue loan
8. Return loan
9. Use chatbot

# Expected: All workflows work without errors
```

**Phase 2 (Soft Enforcement):**
```bash
# Test existing workflows + banners
1. Login with existing user
2. Verify banner appears
3. Dismiss banner
4. Verify banner reappears after session expiry
5. Complete all Phase 1 tests
6. Verify no functionality blocked

# Expected: Banners shown but no blocking
```

**Phase 3 (Hard Enforcement):**
```bash
# Test with incomplete onboarding
1. Login with user without business profile
2. Attempt to add item → Should redirect to onboarding
3. Attempt to issue loan → Should redirect to onboarding

# Test with complete onboarding
1. Login with user with ACTIVE profile
2. All workflows should work normally

# Test grace period
1. Login with user created <30 days ago
2. All workflows should work (grace period active)

# Test admin override
1. Login as PLATFORM_ADMIN
2. All workflows should work (override active)

# Expected: Enforcement works as designed
```

**Phase 4 (Admin Dashboard):**
```bash
# Test admin tools
1. Login as PLATFORM_ADMIN
2. Access /admin/onboarding/dashboard
3. View metrics
4. Access legacy users report
5. Approve/reject pending profiles

# Expected: Admin tools functional
```

---

### Compatibility Test Matrix

| Test Case | Phase 1 | Phase 2 | Phase 3 | Phase 4 | Pass Criteria |
|-----------|---------|---------|---------|---------|---------------|
| **Existing User Login** | ✅ | ✅ | ✅ | ✅ | Login successful |
| **New User Registration** | ✅ | ✅ | ✅ | ✅ | Registration successful |
| **View Inventory (No Profile)** | ✅ | ✅ | ⚠️ | ⚠️ | Phase 1-2: Works, Phase 3-4: Redirects |
| **Add Item (No Profile)** | ✅ | ✅ | ⚠️ | ⚠️ | Phase 1-2: Works, Phase 3-4: Blocked |
| **Add Item (ACTIVE Profile)** | ✅ | ✅ | ✅ | ✅ | Always works |
| **Chatbot Query** | ✅ | ✅ | ✅ | ✅ | Always works |
| **Admin Access** | ✅ | ✅ | ✅ | ✅ | Always works |
| **API Endpoint (Existing)** | ✅ | ✅ | ⚠️ | ⚠️ | Phase 1-2: Works, Phase 3-4: Conditional |
| **API Endpoint (New)** | ✅ | ✅ | ✅ | ✅ | Always works |

---

## 🚨 Breaking Change Scenarios

### Scenario 1: User Without Business Profile (Phase 3+)

**Before Phase 3:**
- User can add items, manage vendors, issue loans

**After Phase 3 (Hard Enforcement Enabled):**
- User redirected to onboarding page
- Cannot access protected features until onboarding complete

**Mitigation:**
- 30-day grace period for existing users
- Clear communication before Phase 3
- Email reminders during grace period
- Admin override for exceptions

**Rollback:**
```properties
feature.onboarding.hard-enforcement=false
```

---

### Scenario 2: API Client Without Onboarding (Phase 3+)

**Before Phase 3:**
- API client can call `/items/add` with valid credentials

**After Phase 3 (Hard Enforcement Enabled):**
- API call returns 403 Forbidden if user lacks ACTIVE profile

**Mitigation:**
- API documentation updated with new requirements
- Error response includes onboarding URL
- Separate API key for system integrations (bypass enforcement)

**Example Error Response:**
```json
{
  "error": "ONBOARDING_REQUIRED",
  "message": "Business profile verification required",
  "onboarding_url": "/business-profile/create",
  "user_status": {
    "has_profile": false,
    "verification_status": null,
    "onboarding_stage": null
  }
}
```

---

### Scenario 3: Database Rollback

**Before Rollback:**
- Business profile data exists in database

**After Rollback:**
- Business profile tables dropped
- Application reverted to previous version

**Mitigation:**
- Backup business profile data before rollback
- Export to CSV for recovery if needed
- Verify existing data intact after rollback

**Backup Script:**
```sql
-- Backup before rollback
SELECT * INTO OUTFILE '/tmp/business_profiles_backup.csv'
FIELDS TERMINATED BY ',' ENCLOSED BY '"'
LINES TERMINATED BY '\n'
FROM business_profiles;

-- Repeat for other tables
```

---

## ✅ Compatibility Checklist

### Pre-Deployment (Each Phase)

- [ ] All existing unit tests pass
- [ ] All existing integration tests pass
- [ ] Regression test suite executed
- [ ] Database migration tested in staging
- [ ] Rollback procedure tested
- [ ] Existing data verified intact
- [ ] API endpoints tested (existing + new)
- [ ] User workflows tested (existing + new)
- [ ] Performance benchmarks met

### Post-Deployment (Each Phase)

- [ ] Monitor error logs (0 errors expected)
- [ ] Monitor performance metrics (no degradation)
- [ ] Verify existing user workflows functional
- [ ] Verify new features accessible
- [ ] Check support ticket volume (<10/day)
- [ ] Verify database integrity
- [ ] Test rollback procedure (in staging)

---

## 📊 Compatibility Metrics

### Success Criteria

| Metric | Target | Measurement |
|--------|--------|-------------|
| Existing workflow success rate | 100% | No errors in logs |
| API endpoint availability | 100% | Uptime monitoring |
| Database query performance | <100ms | Query execution time |
| User complaint rate | <1% | Support tickets |
| Rollback time | <5 minutes | Timed test |

### Monitoring

**Automated Tests:**
```bash
# Run after each deployment
mvn test -Dtest=BackwardCompatibilityTest
```

**Manual Verification:**
```bash
# Test existing workflows
./scripts/test-existing-workflows.sh

# Test new workflows
./scripts/test-new-workflows.sh

# Compare results
diff baseline-results.txt current-results.txt
```

---

## 🔄 Version Compatibility

### Application Versions

| Version | Onboarding Phase | Database Schema | Compatible With |
|---------|------------------|-----------------|-----------------|
| v1.9.0 | None | Baseline | v1.8.x, v1.9.x |
| v2.0.0 | Phase 1 | V1_1 | v1.9.x, v2.0.x |
| v2.1.0 | Phase 2 | V1_1 | v2.0.x, v2.1.x |
| v2.2.0 | Phase 3 | V1_1 | v2.1.x, v2.2.x |
| v2.3.0 | Phase 4 | V1_1 | v2.2.x, v2.3.x |

**Upgrade Path:**
- v1.9.0 → v2.0.0: Safe (additive only)
- v2.0.0 → v2.1.0: Safe (feature flag change)
- v2.1.0 → v2.2.0: Safe (feature flag change)
- v2.2.0 → v2.3.0: Safe (new admin features)

**Downgrade Path:**
- v2.3.0 → v2.2.0: Safe (disable admin features)
- v2.2.0 → v2.1.0: Safe (disable hard enforcement)
- v2.1.0 → v2.0.0: Safe (disable soft enforcement)
- v2.0.0 → v1.9.0: Requires database rollback

---

## 📝 Documentation Updates

### User Documentation

- [ ] Update user guide with onboarding instructions
- [ ] Add FAQ: "Why do I need to complete onboarding?"
- [ ] Add troubleshooting guide
- [ ] Update API documentation with new endpoints

### Developer Documentation

- [ ] Update architecture diagram
- [ ] Document new database schema
- [ ] Update API reference
- [ ] Add migration guide for developers

### Operations Documentation

- [ ] Update deployment procedures
- [ ] Document rollback procedures
- [ ] Update monitoring dashboards
- [ ] Add runbook for common issues

---

**End of Backward Compatibility Matrix**

*Last Updated: 2026-02-26*  
*Version: 1.0*  
*Owner: FlowTrack Engineering Team*
