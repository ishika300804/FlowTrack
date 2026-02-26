package com.example.IMS.config;

import com.example.IMS.service.GoogleOAuth2UserService;
import com.example.IMS.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private GoogleOAuth2UserService googleOAuth2UserService;

    @Autowired
    private GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                // Public resources
                .antMatchers("/css/**", "/js/**", "/images/**", "/api/chatbot/**").permitAll()
                .antMatchers("/", "/home", "/landing", "/about", "/pricing", "/get-started").permitAll()
                
                // Registration & Authentication
                .antMatchers("/register/**", "/login").permitAll()
                .antMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                // Razorpay webhook — public, signature-verified internally
                .antMatchers("/payment/webhook").permitAll()
                
                // Platform Admin Routes
                .antMatchers("/admin/**", "/platform/**").hasAuthority("ROLE_PLATFORM_ADMIN")
                
                // Retailer Routes
                .antMatchers("/retailer/**", "/inventory/**", "/transactions/**").hasAuthority("ROLE_RETAILER")
                
                // Vendor Routes
                .antMatchers("/vendor/**", "/orders/**", "/products/**").hasAuthority("ROLE_VENDOR")
                
                // Investor Routes
                .antMatchers("/investor/**", "/investments/**", "/portfolio/**").hasAuthority("ROLE_INVESTOR")
                
                // Legacy routes - will be refactored
                .antMatchers("/ItemCreate", "/ItemEdit/**", "/ItemDelete/**").hasAnyAuthority("ROLE_PLATFORM_ADMIN", "ROLE_RETAILER")
                .antMatchers("/vendors/**").hasAnyAuthority("ROLE_PLATFORM_ADMIN", "ROLE_RETAILER")
                
                .anyRequest().authenticated()
            .and()
            .formLogin()
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler((request, response, authentication) -> {
                    // Role-based redirect — scan ALL authorities (Set order is non-deterministic)
                    java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities =
                            authentication.getAuthorities();
                    String redirect = "/";
                    for (org.springframework.security.core.GrantedAuthority auth : authorities) {
                        String role = auth.getAuthority();
                        if (role.equals("ROLE_PLATFORM_ADMIN")) { redirect = "/admin/dashboard"; break; }
                        else if (role.equals("ROLE_RETAILER"))   { redirect = "/retailer/dashboard"; break; }
                        else if (role.equals("ROLE_VENDOR"))     { redirect = "/vendor/dashboard"; break; }
                        else if (role.equals("ROLE_INVESTOR"))   { redirect = "/investor/dashboard"; break; }
                    }
                    response.sendRedirect(redirect);
                })
                .failureUrl("/login?error=true")
                .permitAll()
            .and()
            .oauth2Login()
                .loginPage("/login")
                .userInfoEndpoint()
                    .userService(googleOAuth2UserService)
                .and()
                .successHandler(googleOAuth2SuccessHandler)
            .and()
            .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/landing?logout=true")
                .permitAll();
    }
}
