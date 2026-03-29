package com.example.restaurantpro.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get("src", "main", "resources", "static", "images", "uploads").toAbsolutePath().normalize();
        String uploadLocation = uploadPath.toUri().toString();

        registry.addResourceHandler("/images/uploads/**")
                .addResourceLocations(uploadLocation);
    }
}
