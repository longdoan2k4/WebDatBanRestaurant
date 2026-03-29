package com.example.restaurantpro.config;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.restaurantpro.model.AppUser;
import com.example.restaurantpro.service.AppUserService;
import com.example.restaurantpro.service.VnPayService;

@ControllerAdvice
public class GlobalModelAttributes {

    private final AppUserService appUserService;
    private final VnPayService vnPayService;

    public GlobalModelAttributes(AppUserService appUserService, VnPayService vnPayService) {
        this.appUserService = appUserService;
        this.vnPayService = vnPayService;
    }

    @ModelAttribute("currentUser")
    public AppUser currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return appUserService.findByLoginId(authentication.getName()).orElse(null);
    }

    @ModelAttribute("vnpayConfigured")
    public boolean vnpayConfigured() {
        return vnPayService.isConfigured();
    }
}
