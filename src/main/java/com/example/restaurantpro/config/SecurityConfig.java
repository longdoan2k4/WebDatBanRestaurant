package com.example.restaurantpro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.restaurantpro.service.CustomUserDetailsService;
import com.example.restaurantpro.service.GoogleOAuth2UserService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
            GoogleOAuth2UserService googleOAuth2UserService,
            CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,
            CustomAccessDeniedHandler customAccessDeniedHandler) {
        this.customUserDetailsService = customUserDetailsService;
        this.googleOAuth2UserService = googleOAuth2UserService;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider) throws Exception {
        http.authenticationProvider(authenticationProvider);

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/menu", "/login", "/register", "/set-password", "/forgot-password/**", "/403",
                        "/otp/**", "/oauth2/**", "/login/oauth2/**", "/css/**", "/js/**", "/images/**",
                        "/payment/vnpay/return", "/payment/vnpay-return", "/payment/vnpay/ipn")
                .permitAll()
                .requestMatchers("/admin/users/**").hasRole("ADMIN")
                .requestMatchers("/admin/bookings/**").hasAnyRole("ADMIN", "TABLE_MANAGER")
                .requestMatchers("/admin/tables/**").hasAnyRole("ADMIN", "TABLE_MANAGER")
                .requestMatchers("/api/admin/tables/**").hasAnyRole("ADMIN", "TABLE_MANAGER")
                .requestMatchers("/admin/kitchen-orders/**").hasAnyRole("ADMIN", "MENU_MANAGER")
                .requestMatchers("/admin/menu/**").hasAnyRole("ADMIN", "MENU_MANAGER")
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "TABLE_MANAGER", "MENU_MANAGER")
                .requestMatchers("/api/loyalty/**").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers("/api/bookings/**").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers("/booking/**", "/payment/**", "/profile/**").hasAnyRole("CUSTOMER", "ADMIN")
                .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureUrl("/login?error=true")
                        .permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo.userService(googleOAuth2UserService))
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureUrl("/login?error=true"))
                .logout(logout -> logout
                        .logoutSuccessUrl("/?logout=true")
                        .permitAll())
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(customAccessDeniedHandler));

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }
}
