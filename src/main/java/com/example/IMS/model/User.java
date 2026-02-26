package com.example.IMS.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Username is required")
    @Column(nullable = false, unique = true)
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(nullable = false, unique = true)
    private String email;
    
    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password;
    
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Column(nullable = false)
    private boolean enabled = true;

    // ── Registration hints ────────────────────────────────────────────────────
    // Business data collected during sign-up. Stored so the business-profile
    // creation form can be pre-populated. NOT the authoritative/validated source
    // (that lives in the business_profiles table).
    @Column(name = "registration_phone")
    private String registrationPhone;

    @Column(name = "registration_business_name")
    private String registrationBusinessName;

    @Column(name = "registration_business_type")
    private String registrationBusinessType;

    @Column(name = "registration_gst_hint")
    private String registrationGstHint;

    @Column(name = "registration_address")
    private String registrationAddress;
    // ─────────────────────────────────────────────────────────────────────────

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
    
    public User() {}
    
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Set<Role> getRoles() {
        return roles;
    }
    
    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
    
    public void addRole(Role role) {
        this.roles.add(role);
    }

    // -------------------------------------------------------------------------
    // Registration hint getters/setters
    public String getRegistrationPhone() { return registrationPhone; }
    public void setRegistrationPhone(String registrationPhone) { this.registrationPhone = registrationPhone; }

    public String getRegistrationBusinessName() { return registrationBusinessName; }
    public void setRegistrationBusinessName(String registrationBusinessName) { this.registrationBusinessName = registrationBusinessName; }

    public String getRegistrationBusinessType() { return registrationBusinessType; }
    public void setRegistrationBusinessType(String registrationBusinessType) { this.registrationBusinessType = registrationBusinessType; }

    public String getRegistrationGstHint() { return registrationGstHint; }
    public void setRegistrationGstHint(String registrationGstHint) { this.registrationGstHint = registrationGstHint; }

    public String getRegistrationAddress() { return registrationAddress; }
    public void setRegistrationAddress(String registrationAddress) { this.registrationAddress = registrationAddress; }

    // UserDetails interface methods
    // -------------------------------------------------------------------------

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
