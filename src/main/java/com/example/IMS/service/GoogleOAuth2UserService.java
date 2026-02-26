package com.example.IMS.service;

import com.example.IMS.model.User;
import com.example.IMS.repository.IUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles Google OAuth2 sign-in.
 *
 * Flow:
 *   1. Google returns an OAuth2User with attributes (email, name, picture, …)
 *   2. We look up—or auto-create—our domain {@link User} by email
 *   3. We return the pure OAuth2User (NOT our User entity); role-redirect and
 *      principal-swap happen later in {@link com.example.IMS.config.GoogleOAuth2SuccessHandler}
 */
@Service
public class GoogleOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuth2UserService.class);

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");

        if (email == null) {
            logger.error("Google did not return an email address — OAuth2 login rejected");
            throw new OAuth2AuthenticationException("No email returned by Google");
        }

        Optional<User> existing = userRepository.findByEmail(email);

        if (existing.isEmpty()) {
            // Auto-provision a new account — no role assigned yet.
            // User will be redirected to /get-started to choose a role.
            User newUser = new User();
            newUser.setEmail(email);

            // Derive a username from the email prefix, handle collisions
            String baseUsername = email.split("@")[0].replaceAll("[^A-Za-z0-9_]", "_");
            String username = baseUsername;
            int suffix = 1;
            while (userRepository.findByUsername(username).isPresent()) {
                username = baseUsername + "_" + suffix++;
            }
            newUser.setUsername(username);

            // Split display name into first / last if possible
            if (name != null && name.contains(" ")) {
                String[] parts = name.split(" ", 2);
                newUser.setFirstName(parts[0]);
                newUser.setLastName(parts[1]);
            } else {
                newUser.setFirstName(name != null ? name : username);
                newUser.setLastName("");
            }

            // Random secure password — user can never use this directly (OAuth-only login)
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            newUser.setEnabled(true);
            newUser.setRoles(new HashSet<>());

            userRepository.save(newUser);
            logger.info("Auto-provisioned new user via Google OAuth2: {} ({})", username, email);
        } else {
            logger.info("Existing user signed in via Google OAuth2: {}", email);
        }

        // Return the raw OAuth2User — success handler swaps the principal to our User
        return oAuth2User;
    }
}
