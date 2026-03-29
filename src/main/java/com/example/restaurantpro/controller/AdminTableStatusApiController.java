package com.example.restaurantpro.controller;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.restaurantpro.dto.TableStatusResponseDto;
import com.example.restaurantpro.service.TableService;

@RestController
@RequestMapping("/api/admin/tables")
public class AdminTableStatusApiController {

    private final TableService tableService;

    public AdminTableStatusApiController(TableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping("/status")
    public TableStatusResponseDto getStatus(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkTime) {
        return tableService.getTableStatusAt(checkTime);
    }
}
