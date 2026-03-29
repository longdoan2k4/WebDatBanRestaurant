package com.example.restaurantpro.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.restaurantpro.model.MenuCategory;
import com.example.restaurantpro.model.MenuItem;
import com.example.restaurantpro.service.MenuService;

@Controller
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/menu")
    public String menuPage(Model model) {
        Map<MenuCategory, java.util.List<MenuItem>> groupedMenu = menuService.getGroupedAvailableMenu();
        model.addAttribute("groupedMenu", groupedMenu);
        return "menu";
    }
}
