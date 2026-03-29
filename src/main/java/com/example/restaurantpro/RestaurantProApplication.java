package com.example.restaurantpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RestaurantProApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantProApplication.class, args);
    }
}
