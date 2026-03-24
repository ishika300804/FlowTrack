# Staged Rollout Strategy - FlowTrack Business Onboarding

## Overview

This document outlines the phased deployment strategy for the business onboarding system, designed to minimize risk and ensure zero downtime during rollout.

---

## 🎯 Rollout Objectives

1. **Zero Downtime**: Existing users continue uninterrupted
2. **Gradual Adoption**: New features introduced incrementally
3. **Risk Mitigation**: Instant rollback capability via feature flags
4. **Data Safety**: No destructive schema changes
5. **User Experience**: Smooth transition with clear communication

---

## 📋 Phase Overview

| Phase | Duration | Enforcement | Rollback Risk | User Impact |
|-------|----------|-------------|---------------|-------------|
| **Phase 1** | 1 week | None | Low | Zero |
| **Phase 2** | 2-4 weeks | Soft (banners) | Very Low | Minimal |
| **Phase 3** | Ongoing | Hard (blocking) | Low | Moderate |
| **Phase 4** | Ongoing | Admin tools | None | Admin only |

---

## 🚀 Phase 1: Schema Addition (Zero Impact)

### Goal
Deploy database schema and backend code without affecting existing users.

### Actions

1. **Database Migration**
   ```bash
   # Flyway automatically runs migrations on startup
   # V1_1__business_profile_extension.sql creates:
   # - business_profiles table
   # - bank_details table
   # - verification_logs table
   ```

2. **Feature Flag Configuration**
   ```properties
   # application.properties
   feature.onboarding.enabled=true
   feature.onboarding.soft-enforcement=false
   feature.onboarding.hard-enforcement=false
   ```

3. **Deployment Steps**
   - Deploy new code to staging environment
   - Run integration tests
   - Deploy to production during low-traffic window
   - Monitor logs for migration errors
   - Verify new tables created successfully

### Verification Checklist

- [ ] Database migration completed without errors
- [ ] New tables exist: `business_profiles`, `bank_details`, `verification_logs`
- [ ] Existing tables untouched (verify row counts)
- [ ] Application starts successfully
- [ ] Existing user workflows functional (login, inventory, etc.)
- [ ] No errors in application logs
- [ ] Performance metrics unchanged

### Rollback Plan

**If issues detected:**
```sql
-- Drop new tables (no data loss for existing workflows)
DROP TABLE IF EXISTS verification_logs;
DROP TABLE IF EXISTS bank_details;
DROP TABLE IF EXISTS business_profiles;
```

**Redeploy previous version:**
```bash
git checkout <previous-release-tag>
mvn clean package
# Deploy previous WAR/JAR
```

### Success Criteria
- ✅ Zero errors in logs for 24 hours
- ✅ Existing user workflows unaffected
- ✅ New tables accessible via admin tools

---

## 📢 Phase 2: Soft Enforcement (Banners/Notifications)

### Goal
Encourage users to complete onboarding without blocking access.

### Actions

1. **Enable Soft Enforcement**
   ```properties
   # application.properties
   feature.onboarding.soft-enforcement=true
   ```

2. **Banner Types**
   - **No Profile**: "Complete your business profile to unlock all features"
   - **Draft Profile**: "Submit your profile for verification"
   - **Pending Verification**: "Verification in progress (24-48 hours)"
   - **Rejected**: "Verification failed - review and resubmit"
   - **Verified but Inactive**: "Complete remaining steps to activate"

3. **User Communication**
   - Email campaign: "New feature - Complete your business profile"
   - In-app notifications
   - Dashboard widget: "Onboarding progress: 30% complete"

4. **Monitoring**
   - Track banner dismissal rate
   - Track onboarding completion rate
   - Track time-to-completion
   - Identify drop-off points

### Verification Checklist

- [ ] Banners display correctly for each user state
- [ ] Banners can be dismissed
- [ ] Banners reappear after session expiry
- [ ] No performance degradation
- [ ] Analytics tracking functional

### Metrics to Monitor

| Metric | Target | Action if Below Target |
|--------|--------|------------------------|
| Onboarding start rate | >30% | Improve banner messaging |
| Completion rate | >50% | Simplify onboarding flow |
| Time to complete | <10 min | Reduce form fields |
| Dismissal rate | <70% | Make banners less intrusive |

### Rollback Plan

**Instant rollback (no deployment):**
```properties
# application.properties
feature.onboarding.soft-enforcement=false
```

Restart application or wait for config refresh (if using Spring Cloud Config).

### Success Criteria
- ✅ 30%+ users start onboarding within 2 weeks
- ✅ 50%+ completion rate for started profiles
- ✅ No user complaints about banner intrusiveness
- ✅ System performance stable

---

## 🔒 Phase 3: Hard Enforcement (Conditional Blocking)

### Goal
Block protected features for users without complete onboarding.

### Actions

1. **Enable Hard Enforcement**
   ```properties
   # application.properties
   feature.onboarding.hard-enforcement=true
   feature.onboarding.grace-period-days=30
   ```

2. **Protected Operations**

   **Inventory Operations (require `onboarding_stage = ACTIVE`):**
   - Add/Edit/Delete items
   - Add/Edit/Delete vendors
   - Issue/Return loans

   **Financial Operations (require `verification_status = VERIFIED`):**
   - Initiate payments
   - Settle transactions
   - Request investments
   - Accept investment offers

3. **Grace Period**
   - Existing users: 30 days from Phase 3 deployment
   - New users: No grace period
   - Calculation: `days_since_profile_creation < grace_period_days`

4. **Admin Override**
   - Platform admins can bypass enforcement
   - Useful for testing and emergency access
   - Logged for audit purposes

5. **User Communication**
   - Email: "Action required - Complete onboarding by [date]"
   - In-app countdown: "15 days remaining to complete onboarding"
   - Blocked page: Clear instructions on how to complete onboarding

### Verification Checklist

- [ ] Protected paths correctly blocked
- [ ] Grace period calculation accurate
- [ ] Admin override functional
- [ ] Redirect to onboarding page works
- [ ] Error messages clear and actionable
- [ ] No false positives (verified users blocked)

### Monitoring

| Alert | Threshold | Action |
|-------|-----------|--------|
| Blocked requests | >100/hour | Review enforcement logic |
| Support tickets | >50/day | Improve messaging |
| Onboarding completion | <80% | Extend grace period |

### Rollback Plan

**Instant rollback (no deployment):**
```properties
# application.properties
feature.onboarding.hard-enforcement=false
```

**Partial rollback (disable specific protections):**
- Comment out paths in `OnboardingEnforcementInterceptor.PROTECTED_INVENTORY_PATHS`
- Redeploy

### Success Criteria
- ✅ 80%+ users complete onboarding within grace period
- ✅ <10 support tickets/day related to enforcement
- ✅ No false positives reported
- ✅ System performance stable

---

## 🛠️ Phase 4: Admin Dashboard

### Goal
Provide platform admins with tools to manage onboarding and legacy users.

### Features

1. **Onboarding Dashboard** (`/admin/onboarding/dashboard`)
   - Total users vs profiles created
   - Conversion rate (profiles / users)
   - Activation rate (active profiles / users)
   - Pending approvals count
   - Legacy users count

2. **Legacy Users Report** (`/admin/onboarding/legacy-users`)
   - List of users without business profiles
   - User details (username, email, roles)
   - Days since registration
   - Bulk email action

3. **Status Report** (`/admin/onboarding/status-report`)
   - Verification status breakdown (DRAFT, PENDING, VERIFIED, REJECTED)
   - Onboarding stage breakdown (TIER1, TIER2, ACTIVE)
   - Conversion funnel visualization

4. **Pending Approvals** (`/admin/onboarding/pending-approvals`)
   - List of profiles awaiting manual review
   - Approve/Reject actions
   - View uploaded documents
   - Add rejection reason

### Verification Checklist

- [ ] Dashboard loads without errors
- [ ] Metrics accurate (cross-check with DB queries)
- [ ] Approve/Reject actions work
- [ ] Only PLATFORM_ADMIN can access
- [ ] Responsive design (mobile-friendly)

### Success Criteria
- ✅ Admins can review pending profiles in <5 minutes
- ✅ Approval/rejection workflow smooth
- ✅ Metrics help identify bottlenecks

---

## 🔄 Migration Sequencing

### Recommended Timeline

```
Week 1: Phase 1 (Schema Addition)
├─ Day 1: Deploy to staging
├─ Day 2-3: Integration testing
├─ Day 4: Deploy to production (Friday evening)
├─ Day 5-7: Monitor for errors
└─ End of Week 1: Verify success criteria

Week 2-5: Phase 2 (Soft Enforcement)
├─ Week 2 Day 1: Enable soft enforcement
├─ Week 2-3: Monitor metrics
├─ Week 4: Analyze conversion rate
├─ Week 5: Optimize messaging if needed
└─ End of Week 5: Decide on Phase 3 timing

Week 6-9: Phase 3 (Hard Enforcement)
├─ Week 6 Day 1: Announce grace period (30 days)
├─ Week 6-8: Grace period active
├─ Week 9 Day 1: Enable hard enforcement
├─ Week 9-10: Monitor support tickets
└─ End of Week 10: Verify success criteria

Week 10+: Phase 4 (Admin Dashboard)
├─ Deploy admin tools
├─ Train admins on approval workflow
└─ Ongoing: Monitor and optimize
```

---

## 🚨 Rollback Procedures

### Instant Rollback (Feature Flags)

**Disable all enforcement:**
```properties
feature.onboarding.enabled=false
feature.onboarding.soft-enforcement=false
feature.onboarding.hard-enforcement=false
```

**Restart application:**
```bash
# If using systemd
sudo systemctl restart flowtrack

# If using Docker
docker restart flowtrack-app
```

**Verification:**
- Banners disappear
- Protected paths accessible
- No onboarding-related errors

### Database Rollback

**Create rollback script:**
```sql
-- db/migration/rollback/R__rollback_business_profile.sql

-- Drop foreign key constraints first
ALTER TABLE bank_details DROP FOREIGN KEY fk_bank_business_profile;
ALTER TABLE verification_logs DROP FOREIGN KEY fk_verification_business_profile;
ALTER TABLE business_profile_roles DROP FOREIGN KEY fk_role_business_profile;

-- Drop tables in reverse order
DROP TABLE IF EXISTS verification_logs;
DROP TABLE IF EXISTS bank_details;
DROP TABLE IF EXISTS business_profile_roles;
DROP TABLE IF EXISTS business_profiles;

-- Verify existing tables untouched
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM roles;
SELECT COUNT(*) FROM inventory_item;
```

**Execute rollback:**
```bash
mysql -u root -p ims < db/migration/rollback/R__rollback_business_profile.sql
```

### Code Rollback

**Revert to previous release:**
```bash
# Tag current release before deployment
git tag v2.0.0-onboarding

# If rollback needed
git checkout v1.9.0-stable
mvn clean package
# Deploy previous version
```

---

## 📊 Monitoring & Alerting

### Key Metrics

1. **Onboarding Funnel**
   - Registration → Profile creation: Target >30%
   - Profile creation → Submission: Target >80%
   - Submission → Verification: Target >90%
   - Verification → Active: Target >95%

2. **Performance**
   - Page load time: <2 seconds
   - API response time: <500ms
   - Database query time: <100ms

3. **Errors**
   - Verification API failures: <5%
   - Database errors: 0
   - Application errors: <10/hour

### Alerts

**Critical (immediate action):**
- Database migration failure
- Application startup failure
- Verification API downtime >5 minutes

**High (action within 1 hour):**
- Error rate >50/hour
- Onboarding completion rate <20%
- Support tickets >100/day

**Medium (action within 24 hours):**
- Conversion rate <30%
- Banner dismissal rate >80%
- Grace period expiring for >100 users

---

## ✅ Success Criteria Summary

### Phase 1
- ✅ Zero errors for 24 hours
- ✅ Existing workflows unaffected
- ✅ New tables accessible

### Phase 2
- ✅ 30%+ onboarding start rate
- ✅ 50%+ completion rate
- ✅ No performance degradation

### Phase 3
- ✅ 80%+ completion within grace period
- ✅ <10 support tickets/day
- ✅ No false positives

### Phase 4
- ✅ Admin tools functional
- ✅ Approval workflow smooth
- ✅ Metrics actionable

---

## 🔐 Risk Mitigation

### Risk Matrix

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Database migration failure | Low | High | Test in staging, backup before migration |
| Existing workflows broken | Low | Critical | Comprehensive testing, instant rollback |
| User confusion | Medium | Medium | Clear messaging, support documentation |
| Performance degradation | Low | High | Load testing, monitoring |
| False positives (blocking verified users) | Medium | High | Thorough testing, admin override |

### Contingency Plans

**If onboarding completion <30% after 2 weeks:**
- Simplify onboarding form
- Reduce required fields
- Improve banner messaging
- Extend grace period

**If support tickets >50/day:**
- Add FAQ page
- Improve error messages
- Provide live chat support
- Temporarily disable hard enforcement

**If verification API downtime:**
- Queue verification requests
- Retry with exponential backoff
- Manual approval fallback
- Email notification to admins

---

## 📝 Communication Plan

### Internal (Team)

**Before Phase 1:**
- Technical briefing on new features
- Rollback procedures training
- Monitoring dashboard walkthrough

**Before Phase 2:**
- Soft enforcement announcement
- Expected user behavior changes
- Support ticket handling guidelines

**Before Phase 3:**
- Hard enforcement announcement
- Grace period communication strategy
- Escalation procedures

### External (Users)

**Phase 1:**
- No communication (zero impact)

**Phase 2:**
- Email: "Introducing Business Profiles"
- Blog post: "Why complete your profile?"
- In-app banner: "New feature available"

**Phase 3:**
- Email: "Action required - 30 days to complete onboarding"
- Email reminder: 15 days, 7 days, 1 day before grace period ends
- In-app countdown timer
- Support article: "How to complete onboarding"

---

## 🎓 Lessons Learned (Post-Rollout)

**Document after each phase:**
- What went well?
- What could be improved?
- Unexpected issues encountered?
- User feedback summary
- Metrics vs targets

**Example template:**
```markdown
## Phase 2 Retrospective

### What Went Well
- Banner messaging clear and actionable
- Conversion rate exceeded target (35% vs 30%)

### What Could Be Improved
- Banner dismissal rate high (75%)
- Mobile UI needs optimization

### Unexpected Issues
- Verification API timeout on weekends (low traffic)

### User Feedback
- "Form too long" (5 complaints)
- "Not sure why I need this" (3 complaints)

### Action Items
- Reduce form fields from 15 to 10
- Add explainer video
- Optimize verification API connection pool
```

---

## 📞 Support & Escalation

### Support Tiers

**Tier 1 (User Support):**
- Handle onboarding questions
- Guide users through form
- Escalate technical issues

**Tier 2 (Technical Support):**
- Investigate verification failures
- Debug enforcement issues
- Escalate to engineering

**Tier 3 (Engineering):**
- Fix bugs
- Adjust feature flags
- Database interventions

### Escalation Criteria

**Immediate escalation:**
- Application down
- Database corruption
- Security breach

**1-hour escalation:**
- Verification API down >5 minutes
- Error rate >100/hour
- False positives reported

**24-hour escalation:**
- Conversion rate <20%
- Support tickets >50/day

---

**End of Staged Rollout Guide**

*Last Updated: 2026-02-26*  
*Version: 1.0*  
*Owner: FlowTrack Engineering Team*
