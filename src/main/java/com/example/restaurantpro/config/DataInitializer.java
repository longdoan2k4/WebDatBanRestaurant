package com.example.restaurantpro.config;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.restaurantpro.model.RoleName;
import com.example.restaurantpro.service.AppUserService;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedData(AppUserService appUserService) {
        return args -> {
            appUserService.createSeedUser("Admin", "888888", "123456", Set.of(RoleName.ROLE_ADMIN));
            appUserService.createSeedUser("QuanLiBan", "0808080", "123456", Set.of(RoleName.ROLE_TABLE_MANAGER));
            appUserService.createSeedUser("QuanLiMon", "000000", "123456", Set.of(RoleName.ROLE_MENU_MANAGER));
            appUserService.createSeedUser("User", "111111", "123456", Set.of(RoleName.ROLE_CUSTOMER));
        };
    }
}
