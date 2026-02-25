package com.example.IMS.config;

import com.example.IMS.interceptor.OnboardingBannerInterceptor;
import com.example.IMS.interceptor.OnboardingEnforcementInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration for Interceptor Registration
 * 
 * Registers:
 * 1. OnboardingBannerInterceptor (Phase 2: Soft enforcement)
 * 2. OnboardingEnforcementInterceptor (Phase 3: Hard enforcement)
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private OnboardingBannerInterceptor onboardingBannerInterceptor;

    @Autowired
    private OnboardingEnforcementInterceptor onboardingEnforcementInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Phase 2: Soft enforcement (banners)
        // Applies to all paths except static resources
        registry.addInterceptor(onboardingBannerInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/static/**");

        // Phase 3: Hard enforcement (blocking)
        // Applies to all paths except whitelisted ones (handled internally)
        registry.addInterceptor(onboardingEnforcementInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/static/**");
    }
}
