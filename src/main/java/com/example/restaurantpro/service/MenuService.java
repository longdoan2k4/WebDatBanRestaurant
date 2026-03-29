package com.example.restaurantpro.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.restaurantpro.model.MenuCategory;
import com.example.restaurantpro.model.MenuItem;
import com.example.restaurantpro.repository.BookingRepository;
import com.example.restaurantpro.repository.MenuItemRepository;

@Service
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final BookingRepository bookingRepository;

    public MenuService(MenuItemRepository menuItemRepository,
                       BookingRepository bookingRepository) {
        this.menuItemRepository = menuItemRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<MenuItem> findAllAvailable() {
        return menuItemRepository.findByAvailableTrueOrderByCategoryAscNameAsc();
    }

    public List<MenuItem> findAllForAdmin() {
        return menuItemRepository.findAllByOrderByCategoryAscNameAsc();
    }

    public MenuItem findById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món ăn."));
    }

    public Map<MenuCategory, List<MenuItem>> getGroupedAvailableMenu() {
        List<MenuItem> availableItems = findAllAvailable();
        Map<MenuCategory, List<MenuItem>> grouped = new LinkedHashMap<>();
        for (MenuCategory category : MenuCategory.values()) {
            List<MenuItem> items = availableItems.stream()
                    .filter(item -> item.getCategory() == category)
                    .toList();
            if (!items.isEmpty()) {
                grouped.put(category, items);
            }
        }
        return grouped;
    }

    public MenuItem save(MenuItem menuItem) {
        return menuItemRepository.save(menuItem);
    }

    public void delete(Long id) {
        MenuItem item = findById(id);
        if (bookingRepository.existsBookingItemByMenuItemId(id)) {
            item.setAvailable(false);
            menuItemRepository.save(item);
            return;
        }
        menuItemRepository.deleteById(id);
    }

    public long countMenuItems() {
        return menuItemRepository.count();
    }

    public List<MenuItem> findAll() {
        return findAllForAdmin();
    }

    public MenuItem saveOrUpdate(Long id,
                                 String name,
                                 MenuCategory category,
                                 String description,
                                 BigDecimal price,
                                 String imageUrl,
                                 boolean available) {
        MenuItem item;
        if (id == null) {
            item = new MenuItem();
        } else {
            item = findById(id);
        }
        item.setName(name);
        item.setCategory(category);
        item.setDescription(description);
        item.setPrice(price);
        if (imageUrl == null || imageUrl.isBlank()) {
            if (item.getImageUrl() == null || item.getImageUrl().isBlank()) {
                item.setImageUrl("/images/steak.svg");
            }
        } else {
            item.setImageUrl(imageUrl);
        }
        item.setAvailable(available);
        return save(item);
    }
}