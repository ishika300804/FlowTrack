# FlowTrack Transformation Brief
## Project Evolution: From Internal IMS to Multi-Tenant SaaS Platform

---

## 📋 Executive Summary

**Project:** FlowTrack (formerly IMS-AP - Inventory Management System)  
**Date:** February 2026  
**Phase:** Phase 2 - SaaS Platform Transformation  
**Branch:** dev-Phase-2

### **The Pivot:**
FlowTrack is transitioning from a single-organization internal inventory management system to a **multi-tenant SaaS platform** that connects retailers, vendors, and investors in a supply chain and investment ecosystem.

---

## 🔄 Role Structure Transformation

### **OLD MODEL (Academic Project - Single Organization)**

| Role | Purpose | Problem |
|------|---------|---------|
| `ROLE_ADMIN` | System administrator for one organization | Assumes single-tenant usage |
| `ROLE_MANAGER` | Inventory manager with elevated privileges | Too granular for small businesses |
| `ROLE_STAFF` | Staff member with limited access | Unnecessary complexity for SMBs |
| `ROLE_USER` | Basic user with view-only access | Vague purpose, minimal functionality |

**Assumptions of Old Model:**
- FlowTrack is deployed **within** one organization (e.g., a university)
- Multiple employees have different access levels
- Designed for hierarchical organizational structure
- Items are lent out to borrowers (library-style system)

**Why This Failed:**
- ❌ Not scalable for multiple independent businesses
- ❌ Small-mid businesses don't need complex employee hierarchies
- ❌ No multi-tenancy support
- ❌ No business-to-business interactions
- ❌ No investment/financial ecosystem

---

### **NEW MODEL (SaaS Platform - Multi-Tenant)**

| Role | Purpose | Use Case | Account Type |
|------|---------|----------|--------------|
| `ROLE_PLATFORM_ADMIN` | FlowTrack team managing the entire platform | System management, user verification, dispute resolution | Internal (FlowTrack staff) |
| `ROLE_RETAILER` | Small-mid scale business owners | Inventory management, supplier connections, investment requests | Customer |
| `ROLE_VENDOR` | Product suppliers and distributors | Supply products to retailers, manage orders | Customer |
| `ROLE_INVESTOR` | Individual or institutional investors | Provide capital to retailers, track ROI | Customer |

**Key Architectural Changes:**
1. ✅ **Multi-tenancy:** Each retailer = separate account with isolated data
2. ✅ **B2B Transactions:** Retailers ↔ Vendors (product supply)
3. ✅ **Investment Ecosystem:** Retailers ↔ Investors (capital funding)
4. ✅ **Identity Verification:** All users must prove their identity before access
5. ✅ **Open Registration:** Users self-register, platform admin approves

---

## 👥 Detailed Role Analysis

### **1. ROLE_PLATFORM_ADMIN**

**Who are they?**
- The FlowTrack development/operations team
- System administrators of the SaaS platform

**Responsibilities:**
- Approve/reject new user registrations
- Verify identity documents submitted by retailers/vendors/investors
- Manage platform-wide settings
- Resolve disputes between parties
- Monitor system health and security
- Handle payment gateway issues (Razorpay integration)
- Access to all data for audit purposes

**Why we need this:**
- Someone must manage the platform itself
- Identity verification requires manual review
- Trust & safety for the ecosystem

**Dashboard Features:**
- Pending verification queue (with uploaded documents)
- User management (approve, suspend, ban)
- Platform analytics (total users, transactions, revenue)
- System configuration

---

### **2. ROLE_RETAILER**

**Who are they?**
- Small to mid-scale business owners (5-50 employees)
- Shop owners, e-commerce businesses, restaurants, pharmacies, etc.
- Target: Businesses needing inventory tracking & investment

**Business Problem They Face:**
1. **Inventory Management:** Tracking stock levels, reorders, product flow
2. **Supplier Relationships:** Finding reliable vendors, managing orders
3. **Capital Shortage:** Need investment for inventory expansion
4. **Cash Flow:** Tied up capital in inventory, need working capital loans

**What FlowTrack Solves:**
- ✅ Real-time inventory tracking
- ✅ Direct connection to verified suppliers (vendors)
- ✅ Access to investors willing to fund inventory
- ✅ Transaction management (orders, payments via Razorpay)
- ✅ Analytics (sales forecasting, stock optimization)

**Why Identity Verification Matters:**
- Investors need to know the business is legitimate
- Vendors need trust before extending credit
- Prevents fraud and fake accounts
- Legal compliance for financial transactions

**Registration Information Needed:**
- **Business Identity:**
  - Business name (as per registration)
  - Business type (Retail Store, E-commerce, Restaurant, Pharmacy, etc.)
  - Business registration number (mandatory in India)
  - GST number (for businesses > ₹20 lakh turnover)
  - Trademark (optional, for brand identity)
  
- **Owner Identity:**
  - Owner's full name (first + last)
  - Email (for account access)
  - Phone number (for OTP verification)
  - Username & password
  
- **Business Address:**
  - Full business address (for verification)
  
- **Documents (Proof):**
  - **Business License/Shop Act License** (proves business exists)
  - **Owner's ID Proof** (Aadhar/PAN/Passport)
  - Optional: GST Certificate, Bank Statement (last 3 months)

**Why Each Piece Matters:**
- **Business Registration Number:** Verifiable with government databases
- **GST Number:** Cross-check with GSTN portal, proves tax compliance
- **Address:** Physical verification possible if needed
- **ID Proof:** Links business to real person (accountability)
- **Bank Statement:** Proves financial activity, credibility for investors

---

### **3. ROLE_VENDOR**

**Who are they?**
- Manufacturers, wholesalers, distributors
- Product suppliers (stationary, electronics, furniture, etc.)
- B2B sellers looking for retailer customers

**Business Problem They Face:**
1. **Customer Acquisition:** Hard to find new retailers to supply
2. **Payment Delays:** Retailers don't pay on time
3. **Inventory Uncertainty:** Don't know what retailers need
4. **Trust Issues:** Fear of non-payment from unknown retailers

**What FlowTrack Solves:**
- ✅ Marketplace to find verified retailers
- ✅ Secure payment through Razorpay (escrow-like)
- ✅ Order management system
- ✅ Demand visibility (what retailers are searching for)
- ✅ Credit scoring (retailers with good payment history)

**Why Identity Verification Matters:**
- Retailers need verified, legitimate suppliers
- Payment disputes require legal entity identification
- Prevents counterfeit/illegal product supply
- Tax compliance (GST for B2B transactions)

**Registration Information Needed:**
- **Company Identity:**
  - Company name (official registered name)
  - Business type (Manufacturer, Wholesaler, Distributor, Agent)
  - Trade License Number
  - GST Number (mandatory for B2B in India)
  - PAN Number (for tax filing)
  
- **Contact Person:**
  - Authorized person's name
  - Email & phone
  - Username & password
  
- **Product Information:**
  - Product categories offered (Electronics, Furniture, Chemicals, etc.)
  - Company description
  
- **Banking Information (for payments):**
  - Bank account number
  - Bank name
  - IFSC code
  - Account holder name (must match company name)
  
- **Documents (Proof):**
  - **Trade License** (proves legal business operation)
  - **GST Certificate** (mandatory for B2B sales)
  - **Company Registration Certificate** (ROC document)
  - Optional: Product samples, certifications (ISO, etc.)

**Why Each Piece Matters:**
- **Trade License:** Legal authority to do business
- **GST Certificate:** Cross-verify with GSTN, enables invoicing
- **Bank Details:** Direct payment, no cash = transparency
- **Company Registration:** Proves legal entity, can be sued if fraud
- **Product Categories:** Helps retailers find right suppliers

---

### **4. ROLE_INVESTOR**

**Who are they?**
- Individual investors (angel investors, wealthy individuals)
- Institutional investors (venture capital, private equity firms)
- Family offices, investment clubs

**Business Problem They Face:**
1. **Deal Flow:** Hard to find good investment opportunities
2. **Due Diligence:** Expensive to verify businesses
3. **Monitoring:** Can't track how invested money is used
4. **Risk Management:** Fear of fraud or business failure

**What FlowTrack Solves:**
- ✅ Pre-verified retailer businesses (we do KYC)
- ✅ Transparent financials (inventory value, sales data)
- ✅ Investment tracking (see how money is deployed)
- ✅ ROI monitoring (real-time business performance)
- ✅ Diversification (invest in multiple retailers)

**Why Identity Verification Matters:**
- Regulatory compliance (SEBI regulations for investments)
- Anti-money laundering (AML) requirements
- Tax compliance (interest/capital gains reporting)
- Legal recourse if disputes arise

**Registration Information Needed:**
- **Investor Identity:**
  - Investor name (individual name or company name)
  - Investor type (Individual, Angel Investor, VC, PE, Family Office)
  - PAN Number (mandatory for financial transactions in India)
  - Aadhar Number (for individual investors)
  - Company registration number (for institutional investors)
  
- **Contact Information:**
  - Email & phone
  - Registered address
  - Username & password
  
- **Investment Profile:**
  - Investment capacity (available capital)
  - Minimum investment amount (₹50,000? ₹5 lakh?)
  - Maximum investment amount (per deal)
  - Preferred sectors (Retail, F&B, E-commerce, Healthcare, etc.)
  - Investment experience (years)
  - Risk appetite (Low/Medium/High)
  - Expected ROI percentage (10%? 20%? 30%?)
  
- **Banking Information:**
  - Bank account number (for disbursements)
  - Bank name
  - IFSC code
  
- **Documents (Proof):**
  - **PAN Card** (mandatory, verifiable with IT department)
  - **Aadhar Card** (for individuals)
  - **Bank Statement** (last 6 months - proves financial capacity)
  - **Investment Portfolio** (optional - shows track record)
  - **Net Worth Certificate** (for large investors)
  - **Company Registration** (for institutional investors)

**Why Each Piece Matters:**
- **PAN Number:** Tax compliance, verifiable, unique identity
- **Bank Statement:** Proves they have money to invest (no fake investors)
- **Investment Profile:** Match investors with suitable opportunities
- **Risk Appetite:** Don't show high-risk retailers to conservative investors
- **Expected ROI:** Align expectations, prevent disputes

---

## 🎯 Target Market Analysis

### **Small-Mid Scale Businesses (Retailers)**

**Size:** 
- 1-50 employees
- Annual revenue: ₹10 lakh - ₹10 crore
- Single location or 2-5 branches

**Pain Points:**
1. Manual inventory tracking (Excel sheets)
2. Cash flow problems (money tied in inventory)
3. Difficulty getting bank loans (lack of collateral)
4. Supplier payment terms (30-60 days credit)
5. No visibility into demand forecasting

**Why They Need FlowTrack:**
- Professional inventory system (affordable vs custom software)
- Access to working capital (from investors)
- Verified supplier network (better prices, trust)
- Growth enabler (scale with investment, not just loans)

---

## 🔐 Identity Verification Strategy

### **Why Identity Verification is Critical:**

1. **Financial Transactions:**
   - Razorpay integration requires KYC for merchants
   - Investment = regulated activity (SEBI compliance)
   - Tax reporting (TDS, GST, income tax)

2. **Trust & Safety:**
   - Prevents fake businesses
   - Reduces fraud (payment defaults, scams)
   - Legal accountability (can pursue if issues)

3. **Business Credibility:**
   - Verified badge increases trust
   - Better match-making (retailers ↔ vendors)
   - Higher investment success rate

### **Verification Workflow:**

```
User Registers → Uploads Documents → Status: PENDING
         ↓
Platform Admin Reviews → Verifies Documents → Cross-checks with Gov Databases
         ↓
Decision: APPROVED or REJECTED (with reason)
         ↓
Email Notification → User can login (if approved)
```

### **Government Database Cross-Verification (India):**

- **GST Number:** GSTN portal (free API)
- **PAN Number:** Income Tax e-filing portal
- **Company Registration:** MCA (Ministry of Corporate Affairs)
- **Trade License:** State government portals
- **Aadhar:** UIDAI (requires consent, paid API)

---

## 📊 Information Collection Impact Analysis

### **For Retailers:**

| Information | Verification Method | Impact on Platform | Risk if Not Collected |
|------------|---------------------|-------------------|----------------------|
| Business Registration Number | MCA/State Portal | Proves legal business | Fake businesses, fraud |
| GST Number | GSTN API | Tax compliance, invoicing | Illegal operations |
| Business License | Manual review | Legal to operate | Unlicensed entities |
| Owner ID Proof | Aadhar/PAN | Links to real person | Anonymous accounts |
| Bank Details | Razorpay KYC | Payment processing | Can't disburse funds |
| Business Address | Google Maps verify | Physical presence | Virtual scams |

### **For Vendors:**

| Information | Verification Method | Impact on Platform | Risk if Not Collected |
|------------|---------------------|-------------------|----------------------|
| GST Certificate | GSTN API | B2B invoicing | Tax evasion |
| Trade License | State commerce dept | Legal supplier | Counterfeit products |
| Bank Account | Penny-drop verification | Payment routing | Payment fraud |
| Company Registration | MCA database | Legal entity | Fly-by-night operators |
| Product Categories | Manual verification | Better matching | Misrepresentation |

### **For Investors:**

| Information | Verification Method | Impact on Platform | Risk if Not Collected |
|------------|---------------------|-------------------|----------------------|
| PAN Number | IT portal | Tax compliance | Income tax issues |
| Bank Statement | Analyze for capacity | Proves solvency | Fake investors |
| Net Worth Certificate | CA verification | Investment limits | Over-commitment |
| Aadhar/Passport | UIDAI/Passport office | Identity proof | Identity theft |
| Investment Experience | Self-declared + verify | Risk assessment | Unsuitable investments |

---

## 🚀 Technical Implementation

### **Current Status (as of Feb 25, 2026):**

✅ **Completed:**
- Role entity refactored (4 new roles)
- SecurityConfig updated with role-based routing
- DataInitializer creates new roles on startup
- Landing page with role selection UI
- Profile entities created (RetailerProfile, VendorProfile, InvestorProfile)
- Registration DTOs with validation
- Registration controllers with routing
- Database schema designed

⏳ **Pending:**
- Registration HTML forms (3 pages)
- Profile repositories
- Registration service layer
- Document upload functionality (file storage)
- Email notification service (SMTP)
- Verification dashboard for Platform Admin
- Role-specific dashboards (4 pages)
- Razorpay integration
- Government API integrations for verification

---

## 🔍 Research Questions for Registration Requirements

### **For GPT/AI to Research:**

1. **Legal Compliance (India):**
   - What documents are **legally required** for B2B marketplace registration?
   - SEBI regulations for investment platforms (if applicable)?
   - RBI guidelines for payment aggregation?
   - GST requirements for platform service fee collection?

2. **Security Best Practices:**
   - What information should we **hash/encrypt** (PAN, Aadhar)?
   - Document storage: S3 with encryption? Retention period?
   - Access logs for sensitive data viewing?
   - GDPR-like compliance (even though India doesn't have full GDPR)?

3. **UX vs Security Trade-off:**
   - How much information is **too much** for registration?
   - Can we make some fields optional initially (progressive profiling)?
   - Verification at registration vs post-registration?
   - Mobile number OTP vs email verification?

4. **Financial Information:**
   - Should we ask for **income/revenue** from retailers?
   - Bank statement: last 3 months or 6 months?
   - Net worth certificate: mandatory or optional for investors?
   - Credit score: integrate with CIBIL/Experian?

5. **Competitive Analysis:**
   - What does **IndiaMART** ask during vendor registration?
   - What does **Zerodha** ask for investor KYC?
   - What does **Amazon Business** ask from retailers?
   - What does **ShopX/Udaan** ask in their B2B platform?

6. **Fraud Prevention:**
   - Should we require **video KYC** (RBI compliance)?
   - IP address logging + geolocation matching?
   - Device fingerprinting for account security?
   - Facial recognition for ID proof matching?

7. **Operational Feasibility:**
   - How long should **manual verification** take? (24hrs, 48hrs, 7 days?)
   - Should we have auto-approval for certain criteria?
   - What if documents are in regional languages (Hindi, Tamil, etc.)?
   - Court-issued documents vs self-attested copies?

---

## 💡 Recommendations for Next Steps

### **Immediate (This Week):**
1. ✅ Research government API availability (GST, PAN verification)
2. ✅ Finalize mandatory vs optional fields for each role
3. ✅ Design 3 registration forms (HTML/Thymeleaf)
4. ✅ Implement file upload (Spring MultipartFile)

### **Short-term (Next 2 Weeks):**
1. ✅ Build verification dashboard for Platform Admin
2. ✅ Integrate email notifications (Spring Mail + Gmail SMTP)
3. ✅ Create approval/rejection workflow
4. ✅ Build role-specific dashboards

### **Medium-term (1 Month):**
1. ✅ Razorpay integration (test mode first)
2. ✅ Retailer ↔ Vendor transaction flow
3. ✅ Investment request system
4. ✅ Reporting & analytics

### **Long-term (2-3 Months):**
1. ✅ Government API integrations (automated verification)
2. ✅ Mobile app (optional)
3. ✅ AI-powered matching (retailers ↔ vendors, retailers ↔ investors)
4. ✅ Deploy on AWS with domain

---

## 📈 Success Metrics

**For Platform:**
- Number of verified users (by role)
- Retailer-Vendor transactions (volume & value)
- Investment deals closed
- Platform service fee revenue

**For Retailers:**
- Inventory turnover improvement
- Capital raised via platform
- Supplier network expansion

**For Vendors:**
- Number of retailer connections
- Order volume growth
- Payment cycle reduction

**For Investors:**
- ROI achieved
- Number of deals
- Portfolio diversification

---

## 🎓 Learning Outcomes (for GPT Context)

This project demonstrates:
1. **Multi-tenant SaaS architecture** (isolation, scalability)
2. **Identity verification systems** (KYC/KYB)
3. **Role-based access control** (Spring Security)
4. **Payment gateway integration** (Razorpay)
5. **B2B marketplace mechanics** (supply chain)
6. **Investment platform basics** (fintech)
7. **Government API integration** (verification)
8. **Email notification systems** (transactional emails)
9. **Document management** (upload, storage, security)
10. **Real-world compliance** (GST, tax, regulations)

---

## ❓ Key Questions to Answer via Research

**When researching registration requirements, focus on:**

1. **What is the MINIMUM viable information** needed for each role to:
   - Establish trust between parties?
   - Enable transactions?
   - Comply with Indian law?

2. **What is the MAXIMUM acceptable friction** during registration before users abandon?

3. **Which verifications can be automated** vs manual review?

4. **What are the legal liabilities** if we don't collect certain information (e.g., money laundering, tax evasion)?

5. **How do similar Indian platforms** handle this?
   - B2B: Udaan, IndiaMART, Moglix
   - Investment: Angellist India, LetsVenture
   - Payment: Razorpay, Paytm Business

---

**End of Brief**

*This document serves as context for AI-assisted research on registration requirements and identity verification standards for a B2B marketplace + investment platform in India.*
