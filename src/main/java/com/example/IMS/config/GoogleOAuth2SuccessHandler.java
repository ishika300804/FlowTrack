package com.example.IMS.config;

import com.example.IMS.model.User;
import com.example.IMS.repository.IUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Handles post-OAuth2-login redirect.
 *
 * KEY DESIGN: We replace the OAuth2Authentication in the SecurityContext with a
 * {@link UsernamePasswordAuthenticationToken} wrapping our domain {@link User}.
 * This ensures every controller that does {@code (User) auth.getPrincipal()} keeps
 * working identically whether the user signed in via form login or Google.
 */
@Component
public class GoogleOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuth2SuccessHandler.class);

    @Autowired
    private IUserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        org.springframework.security.core.Authentication authentication)
            throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            logger.error("OAuth2 success handler: no domain User found for email {}", email);
            response.sendRedirect("/login?error=oauth2");
            return;
        }

        // ── Swap to UsernamePasswordAuthenticationToken so every controller's
        //    (User) auth.getPrincipal() cast continues to work unchanged ───────
        UsernamePasswordAuthenticationToken newAuth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        // Persist the new context into the HTTP session
        HttpSession session = request.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());

        // Role-based redirect (mirrors form-login successHandler in SecurityConfig)
        String redirectUrl = determineRedirectUrl(user);
        logger.info("OAuth2 login success for {} → redirecting to {}", email, redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private String determineRedirectUrl(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            // New user — no role yet; send to role-selection page
            return "/get-started";
        }
        String role = user.getRoles().iterator().next().getName();
        switch (role) {
            case "ROLE_PLATFORM_ADMIN": return "/admin/dashboard";
            case "ROLE_RETAILER":       return "/retailer/dashboard";
            case "ROLE_VENDOR":         return "/vendor/dashboard";
            case "ROLE_INVESTOR":       return "/investor/dashboard";
            default:                    return "/get-started";
        }
    }
}
