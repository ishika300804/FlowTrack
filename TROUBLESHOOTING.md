# 🆘 Troubleshooting Guide - Common Issues & Solutions

## Quick Diagnostics Checklist

Run these commands to diagnose issues:

```powershell
# 1. Check Java version (should be 11)
java -version

# 2. Check Maven
mvn -version

# 3. Check MySQL is running
mysql -u root -p

# 4. Check port 8080 is available
netstat -ano | findstr :8080
```

---

## 🔴 Common Errors & Solutions

### 1️⃣ **Build Failures**

#### **Error: "Maven not found" or "mvn: command not found"**
```
'mvn' is not recognized as an internal or external command
```

**Solution:**
```powershell
# Use the Maven Wrapper instead (no Maven installation needed)
.\mvnw clean install

# Or install Maven:
# Download from: https://maven.apache.org/download.cgi
# Add to PATH: C:\apache-maven-3.x.x\bin
```

#### **Error: "Compilation failure" or "cannot find symbol"**
```
[ERROR] Cannot find symbol: class ItemService
```

**Solution:**
```powershell
# Clean and rebuild
mvn clean install

# If still fails, check:
# 1. Import statements in your Java files
# 2. Package names match folder structure
# 3. All required dependencies in pom.xml
```

---

### 2️⃣ **Database Connection Issues**

#### **Error: "Access denied for user 'root'@'localhost'"**
```
java.sql.SQLException: Access denied for user 'root'@'localhost'
```

**Solution:**
Edit [src/main/resources/application.properties](src/main/resources/application.properties):
```properties
# Update with your MySQL credentials
spring.datasource.username=root
spring.datasource.password=YOUR_ACTUAL_PASSWORD
```

#### **Error: "Unknown database 'flowtrack'"**
```
java.sql.SQLSyntaxErrorException: Unknown database 'flowtrack'
```

**Solution:**
```powershell
# Create the database
mysql -u root -p
```
```sql
CREATE DATABASE flowtrack;
USE flowtrack;
```

Or use the setup script:
```powershell
.\setup-mysql-database.bat
```

#### **Error: "Communications link failure"**
```
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
```

**Solution:**
```powershell
# 1. Check MySQL service is running
services.msc  # Look for MySQL service

# 2. Start MySQL if stopped
net start MySQL80  # Or your MySQL service name

# 3. Test connection
mysql -u root -p -h localhost
```

---

### 3️⃣ **Port Already in Use**

#### **Error: "Port 8080 was already in use"**
```
Web server failed to start. Port 8080 was already in use.
```

**Solution Option 1: Kill the process**
```powershell
# Find what's using port 8080
netstat -ano | findstr :8080

# Kill the process (replace <PID> with actual number)
taskkill /PID <PID> /F
```

**Solution Option 2: Change port**
Edit [src/main/resources/application.properties](src/main/resources/application.properties):
```properties
server.port=8081
```
Then access: `http://localhost:8081`

---

### 4️⃣ **Application Won't Start**

#### **Error: "Failed to configure a DataSource"**
```
Failed to configure a DataSource: 'url' attribute is not specified
```

**Solution:**
Check [application.properties](src/main/resources/application.properties) has:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/flowtrack
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

#### **Error: "Error creating bean"**
```
Error creating bean with name 'itemController'
```

**Solution:**
- Check for circular dependencies
- Ensure all `@Autowired` services exist
- Verify package structure matches imports

---

### 5️⃣ **404 Errors (Page Not Found)**

#### **Error: Whitelabel Error Page - 404**
```
Whitelabel Error Page
This application has no explicit mapping for /error
```

**Common Causes:**

**1. URL doesn't match controller mapping**
```java
// Controller has:
@GetMapping("/items")

// But you're accessing:
http://localhost:8080/item  ❌ (missing 's')
http://localhost:8080/items ✅ (correct)
```

**2. Template file not found**
```java
// Controller returns:
return "Item/View";  // Looks for: templates/Item/View.html

// Check file exists at:
// src/main/resources/templates/Item/View.html
```

**Solution:**
- Check URL matches `@GetMapping` value exactly
- Verify template file exists (case-sensitive!)
- Clear browser cache

---

### 6️⃣ **500 Errors (Server Error)**

#### **Error: Whitelabel Error Page - 500**
```
Whitelabel Error Page
There was an unexpected error (type=Internal Server Error, status=500)
```

**Solution:**
```powershell
# Check console logs for stack trace
# Look for lines starting with:
# java.lang.NullPointerException
# org.springframework...
```

**Common causes:**
- Null pointer exception (accessing null object)
- Database query error
- Missing required fields in form
- Type conversion error

**Debug with:**
```java
// Add logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class ItemController {
    private static final Logger logger = LoggerFactory.getLogger(ItemController.class);
    
    @GetMapping("/items")
    public String viewItems() {
        logger.info("Accessing items page");  // Add logs
        // Your code
    }
}
```

---

### 7️⃣ **Template/Thymeleaf Errors**

#### **Error: "Error resolving template"**
```
Error resolving template [item_list], template might not exist
```

**Solution:**
- Check file is in `src/main/resources/templates/`
- Verify file extension is `.html`
- Controller should return template name without `.html`:
  ```java
  return "item_list";  ✅
  return "item_list.html";  ❌
  ```

#### **Error: "Property or field cannot be found"**
```
Property or field 'name' cannot be found on object of type 'Item'
```

**Solution:**
- Ensure `Item.java` has `getName()` method (getter)
- Check spelling matches exactly (case-sensitive)
- Add getter if missing:
  ```java
  public String getName() {
      return name;
  }
  ```

---

### 8️⃣ **IDE-Specific Issues**

#### **IntelliJ: "Cannot resolve symbol"**
Red underlines everywhere, but code compiles fine.

**Solution:**
```
1. File → Invalidate Caches → Invalidate and Restart
2. Right-click on project → Maven → Reload Project
3. Ensure JDK is configured: File → Project Structure → SDKs
```

#### **VS Code: "Cannot be resolved to a type"**
```
ItemService cannot be resolved to a type
```

**Solution:**
```
1. Install "Extension Pack for Java"
2. Install "Spring Boot Extension Pack"
3. Reload VS Code
4. Clean workspace: Ctrl+Shift+P → "Java: Clean Java Language Server Workspace"
```

---

### 9️⃣ **Git Issues**

#### **Problem: `.idea/` or `target/` keeps appearing in git**
```
git status shows .idea/misc.xml modified
```

**Solution:**
```powershell
# Already fixed, but if it happens again:
git rm -r --cached .idea target
git commit -m "Remove IDE files"

# Verify .gitignore has:
# .idea/
# target/
```

---

### 🔟 **Performance Issues**

#### **Application is slow to start**
```
Takes 2+ minutes to start
```

**Solution:**
```powershell
# 1. Disable Spring Boot DevTools in production
# Remove from pom.xml:
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
</dependency>

# 2. Increase Java heap size
set JAVA_OPTS=-Xmx512m -Xms256m
mvn spring-boot:run
```

#### **Database queries are slow**
**Solution:**
```properties
# Add to application.properties to see slow queries
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Add indexes to frequently queried columns
# In Item.java:
@Entity
@Table(indexes = {@Index(columnList = "name")})
public class Item {
    // ...
}
```

---

## 🛠️ Advanced Debugging

### **Enable Full Debug Logging**
Add to [application.properties](src/main/resources/application.properties):
```properties
# Debug Spring Boot
logging.level.root=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate=DEBUG

# Show SQL queries
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Show HTTP requests
logging.level.org.springframework.web.servlet.mvc=TRACE
```

### **Check Application Health**
```powershell
# Add Spring Boot Actuator to pom.xml
# Then access:
http://localhost:8080/actuator/health
```

### **Use MySQL Workbench**
- Download: https://dev.mysql.com/downloads/workbench/
- Connect to localhost:3306
- Visualize tables and run queries
- Debug database issues visually

---

## 📋 Pre-Flight Checklist

Before running the application, verify:

- [ ] Java 11 installed (`java -version`)
- [ ] Maven installed or use `mvnw` wrapper
- [ ] MySQL service running
- [ ] Database `flowtrack` created
- [ ] [application.properties](src/main/resources/application.properties) configured correctly
- [ ] Port 8080 available
- [ ] No compilation errors (`mvn clean install`)

---

## 🆘 Still Stuck?

### **Get Help:**

1. **Read error messages carefully**
   - Full stack trace is in console
   - Google the exact error message

2. **Check logs:**
   ```powershell
   # Look for errors in console output
   # Search for keywords: ERROR, Exception, Failed
   ```

3. **Ask for help:**
   - Stack Overflow: Tag with `spring-boot`, `java`
   - Spring Community: https://spring.io/community
   - Reddit: r/SpringBoot

4. **Share relevant info:**
   - Error message (full stack trace)
   - What you were trying to do
   - Code snippet causing issue
   - Java/Spring Boot version

---

## 🔍 Quick Diagnostic Commands

```powershell
# Full diagnostic check
java -version
mvn -version
mysql --version
netstat -ano | findstr :8080
git status

# Build and run with verbose output
mvn clean install -X
mvn spring-boot:run -X

# Check database
mysql -u root -p -e "SHOW DATABASES;"

# View recent logs (if logs are saved to file)
Get-Content logs/spring-boot.log -Tail 50
```

---

## 💾 Backup & Recovery

### **Before Making Major Changes:**
```powershell
# Create a git branch
git checkout -b feature/my-new-feature

# Or commit current state
git add .
git commit -m "Backup before changes"
```

### **If You Broke Something:**
```powershell
# Discard all changes
git checkout .

# Revert to specific commit
git log  # Find commit hash
git reset --hard <commit-hash>

# Clean build
Remove-Item -Recurse -Force target
mvn clean install
```

---

**Remember: Every developer encounters these issues! It's part of learning. 💪**
