package com.example.IMS.config;

import com.example.IMS.model.*;
import com.example.IMS.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IItemTypeRepository itemTypeRepository;

    @Autowired
    private IVendorRepository vendorRepository;

    @Autowired
    private IItemRepository itemRepository;

    @Autowired
    private IBorrowerRepository borrowerRepository;

    @Autowired
    private ILoanRepository loanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create roles if they don't exist - New FlowTrack SaaS Model
        createRoleIfNotExists("ROLE_PLATFORM_ADMIN");
        createRoleIfNotExists("ROLE_RETAILER");
        createRoleIfNotExists("ROLE_VENDOR");
        createRoleIfNotExists("ROLE_INVESTOR");

        // Create item types if they don't exist
        createItemTypeIfNotExists("Stationary");
        createItemTypeIfNotExists("Furniture");
        createItemTypeIfNotExists("Electronics");

        // ── Always ensure the Platform Admin account exists ───────────────────
        // (do NOT guard by userRepository.count() == 0, that breaks when other
        //  users already exist in the DB)
        java.util.Optional<User> existingAdmin = userRepository.findByUsername("admin");
        if (!existingAdmin.isPresent()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@flowtrack.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFirstName("Platform");
            admin.setLastName("Administrator");
            admin.setEnabled(true);

            Role adminRole = roleRepository.findByName("ROLE_PLATFORM_ADMIN").orElseThrow();
            admin.addRole(adminRole);

            userRepository.save(admin);
            System.out.println("✅ Platform Admin CREATED  — username: admin  password: admin123");
        } else {
            // Admin already exists — ensure password is correct and role is present
            User admin = existingAdmin.get();
            boolean changed = false;

            // Force-reset password to known value (idempotent — safe every restart)
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEnabled(true);
            changed = true;

            // Ensure ROLE_PLATFORM_ADMIN is attached
            boolean hasAdminRole = admin.getRoles().stream()
                    .anyMatch(r -> "ROLE_PLATFORM_ADMIN".equals(r.getName()));
            if (!hasAdminRole) {
                Role adminRole = roleRepository.findByName("ROLE_PLATFORM_ADMIN").orElseThrow();
                admin.addRole(adminRole);
                changed = true;
            }

            if (changed) {
                userRepository.save(admin);
                System.out.println("🔄 Platform Admin REFRESHED — username: admin  password: admin123");
            }
        }

        // Create sample data for testing
        createSampleData();
    }

    private void createSampleData() {
        // Only create sample data if database is empty
        if (vendorRepository.count() > 0) {
            return; // Sample data already exists
        }

        System.out.println("📦 Creating sample data...");

        // Create Sample Vendors
        Vendor vendor1 = new Vendor();
        vendor1.setName("Office Supplies Co");
        vendor1.setEmail("contact@officesupplies.com");
        vendorRepository.save(vendor1);

        Vendor vendor2 = new Vendor();
        vendor2.setName("Tech Electronics Ltd");
        vendor2.setEmail("sales@techelectronics.com");
        vendorRepository.save(vendor2);

        Vendor vendor3 = new Vendor();
        vendor3.setName("Furniture World");
        vendor3.setEmail("info@furnitureworld.com");
        vendorRepository.save(vendor3);

        System.out.println("✅ Sample vendors created");

        // Get Item Types
        ItemType stationaryType = itemTypeRepository.findAll().stream()
                .filter(t -> t.getTypeName().equals("Stationary"))
                .findFirst().orElse(null);
        ItemType furnitureType = itemTypeRepository.findAll().stream()
                .filter(t -> t.getTypeName().equals("Furniture"))
                .findFirst().orElse(null);
        ItemType electronicsType = itemTypeRepository.findAll().stream()
                .filter(t -> t.getTypeName().equals("Electronics"))
                .findFirst().orElse(null);

        // Create Sample Items
        createSampleItem("Ballpoint Pen", 50, 0.50, 0.10, 1001, vendor1, stationaryType);
        createSampleItem("Notebook A4", 30, 2.50, 0.25, 1002, vendor1, stationaryType);
        createSampleItem("Marker Set", 20, 5.00, 0.50, 1003, vendor1, stationaryType);
        createSampleItem("Stapler", 15, 8.00, 0.75, 1004, vendor1, stationaryType);
        createSampleItem("Paper Ream", 8, 12.00, 1.00, 1005, vendor1, stationaryType);

        createSampleItem("Office Chair", 12, 150.00, 5.00, 2001, vendor3, furnitureType);
        createSampleItem("Desk", 8, 250.00, 10.00, 2002, vendor3, furnitureType);
        createSampleItem("Filing Cabinet", 6, 180.00, 7.50, 2003, vendor3, furnitureType);
        createSampleItem("Bookshelf", 10, 120.00, 5.00, 2004, vendor3, furnitureType);

        createSampleItem("Laptop", 5, 800.00, 20.00, 3001, vendor2, electronicsType);
        createSampleItem("Monitor", 10, 200.00, 10.00, 3002, vendor2, electronicsType);
        createSampleItem("Keyboard", 25, 50.00, 2.00, 3003, vendor2, electronicsType);
        createSampleItem("Mouse", 30, 25.00, 1.00, 3004, vendor2, electronicsType);
        createSampleItem("Printer", 4, 300.00, 15.00, 3005, vendor2, electronicsType);

        System.out.println("✅ Sample items created");

        // Create Sample Borrowers
        Borrower borrower1 = new Borrower();
        borrower1.setFirstName("John");
        borrower1.setLastName("Doe");
        borrower1.setEmail("john.doe@example.com");
        borrowerRepository.save(borrower1);

        Borrower borrower2 = new Borrower();
        borrower2.setFirstName("Jane");
        borrower2.setLastName("Smith");
        borrower2.setEmail("jane.smith@example.com");
        borrowerRepository.save(borrower2);

        Borrower borrower3 = new Borrower();
        borrower3.setFirstName("Mike");
        borrower3.setLastName("Johnson");
        borrower3.setEmail("mike.johnson@example.com");
        borrowerRepository.save(borrower3);

        Borrower borrower4 = new Borrower();
        borrower4.setFirstName("Sarah");
        borrower4.setLastName("Williams");
        borrower4.setEmail("sarah.williams@example.com");
        borrowerRepository.save(borrower4);

        System.out.println("✅ Sample borrowers created");

        // Create Sample Loans
        Item laptop = itemRepository.findAll().stream()
                .filter(i -> i.getName().equals("Laptop"))
                .findFirst().orElse(null);
        Item monitor = itemRepository.findAll().stream()
                .filter(i -> i.getName().equals("Monitor"))
                .findFirst().orElse(null);
        Item chair = itemRepository.findAll().stream()
                .filter(i -> i.getName().equals("Office Chair"))
                .findFirst().orElse(null);
        Item notebook = itemRepository.findAll().stream()
                .filter(i -> i.getName().equals("Notebook A4"))
                .findFirst().orElse(null);

        if (laptop != null && borrower1 != null) {
            createSampleLoan(laptop, borrower1, "2024-11-01", "", 7, 0.0);
            laptop.descreaseQuantity();
            itemRepository.save(laptop);
        }

        if (monitor != null && borrower2 != null) {
            createSampleLoan(monitor, borrower2, "2024-11-05", "", 14, 0.0);
            monitor.descreaseQuantity();
            itemRepository.save(monitor);
        }

        if (chair != null && borrower3 != null) {
            createSampleLoan(chair, borrower3, "2024-10-20", "2024-11-10", 21, 5.0);
        }

        if (notebook != null && borrower4 != null) {
            createSampleLoan(notebook, borrower4, "2024-11-08", "", 7, 0.0);
            notebook.descreaseQuantity();
            itemRepository.save(notebook);
        }

        if (laptop != null && borrower2 != null) {
            createSampleLoan(laptop, borrower2, "2024-10-15", "2024-10-30", 15, 0.0);
        }

        System.out.println("✅ Sample loans created");
        System.out.println("📦 Sample data initialization complete!");
    }

    private void createSampleItem(String name, int quantity, double price, double fineRate, 
                                  long invoiceNumber, Vendor vendor, ItemType itemType) {
        Item item = new Item();
        item.setName(name);
        item.setQuantity(quantity);
        item.setPrice(price);
        item.setFineRate(fineRate);
        item.setInvoiceNumber(invoiceNumber);
        item.setVendor(vendor);
        item.setItemType(itemType);
        itemRepository.save(item);
    }

    private void createSampleLoan(Item item, Borrower borrower, String issueDate, 
                                  String returnDate, long duration, double fine) {
        Loan loan = new Loan();
        loan.setItem(item);
        loan.setBorrower(borrower);
        loan.setIssueDate(issueDate);
        loan.setReturnDate(returnDate);
        loan.setLoanDuration(duration);
        loan.setTotalFine(fine);
        loanRepository.save(loan);
    }

    private void createRoleIfNotExists(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = new Role(roleName);
            roleRepository.save(role);
            System.out.println("✅ Role created: " + roleName);
        }
    }

    private void createItemTypeIfNotExists(String typeName) {
        if (itemTypeRepository.count() == 0 || 
            itemTypeRepository.findAll().stream()
                .noneMatch(type -> type.getTypeName().equals(typeName))) {
            ItemType itemType = new ItemType();
            itemType.setTypeName(typeName);
            itemTypeRepository.save(itemType);
            System.out.println("✅ Item Type created: " + typeName);
        }
    }
}
