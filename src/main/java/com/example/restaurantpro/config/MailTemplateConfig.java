package com.example.restaurantpro.config;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
@EnableAsync
public class MailTemplateConfig {

    @Bean
    public ClassLoaderTemplateResolver mailTemplateResolver() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCheckExistence(true);
        resolver.setResolvablePatterns(Set.of("mail/*"));
        resolver.setOrder(1);
        return resolver;
    }
}
