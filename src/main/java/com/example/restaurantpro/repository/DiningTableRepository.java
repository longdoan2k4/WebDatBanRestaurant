package com.example.restaurantpro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.restaurantpro.model.DiningTable;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    List<DiningTable> findByActiveTrueOrderByCapacityAsc();

    List<DiningTable> findByActiveTrueAndCapacityGreaterThanEqualOrderByCapacityAsc(Integer guestCount);

    List<DiningTable> findByActiveTrueAndCapacityOrderByNameAsc(Integer capacity);

    boolean existsByTableNumberIgnoreCase(String tableNumber);
}
