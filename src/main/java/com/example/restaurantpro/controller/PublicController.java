package com.example.restaurantpro.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.restaurantpro.dto.RegisterRequest;
import com.example.restaurantpro.service.AppUserService;
import com.example.restaurantpro.service.MenuService;
import com.example.restaurantpro.service.TableService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class PublicController {

    private static final String GOOGLE_OTP_ERROR_DETAIL = "GOOGLE_OTP_ERROR_DETAIL";

    private final TableService tableService;
    private final MenuService menuService;
    private final AppUserService appUserService;

    public PublicController(TableService tableService, MenuService menuService, AppUserService appUserService) {
        this.tableService = tableService;
        this.menuService = menuService;
        this.appUserService = appUserService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featuredTables", pickRandomItems(tableService.getActiveTables(), 4));
        model.addAttribute("featuredMenus", pickRandomItems(menuService.findAllAvailable(), 4));
        return "index";
    }

    private <T> List<T> pickRandomItems(List<T> source, int limit) {
        if (source == null || source.isEmpty() || limit <= 0) {
            return List.of();
        }
        List<T> copied = new ArrayList<>(source);
        Collections.shuffle(copied);
        return copied.stream().limit(limit).toList();
    }

    @GetMapping("/login")
    public String login(@org.springframework.web.bind.annotation.RequestParam(value = "error", required = false) String error, 
                        Authentication authentication, Model model, HttpSession session) {
        Object errorDetail = session.getAttribute(GOOGLE_OTP_ERROR_DETAIL);
        if (errorDetail != null) {
            model.addAttribute("googleOtpErrorDetail", errorDetail.toString());
            session.removeAttribute(GOOGLE_OTP_ERROR_DETAIL);
        }
        if (error != null) {
            Object exceptionObj = session.getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
            if (exceptionObj instanceof Exception exception) {
                if (exception instanceof org.springframework.security.authentication.LockedException ||
                        (exception.getCause() != null && exception.getCause() instanceof org.springframework.security.authentication.LockedException)) {
                    model.addAttribute("errorMessage", "Tài khoản của bạn đã bị khóa do hủy quá 2 đơn đặt bàn!");
                } else {
                    model.addAttribute("errorMessage", "Email hoặc mật khẩu không chính xác!");
                }
            }
            session.removeAttribute("SPRING_SECURITY_LAST_EXCEPTION");
        }

        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/";
        }
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registerRequest",
                    bindingResult);
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            return "redirect:/register";
        }

        try {
            appUserService.registerCustomer(registerRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công. Bạn có thể đăng nhập ngay.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            return "redirect:/register";
        }
    }
}
