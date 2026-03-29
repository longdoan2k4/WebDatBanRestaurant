package com.example.restaurantpro.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.restaurantpro.dto.TableStatusResponseDto;
import com.example.restaurantpro.exception.TableInUseException;
import com.example.restaurantpro.model.Booking;
import com.example.restaurantpro.model.DiningTable;
import com.example.restaurantpro.model.MenuCategory;
import com.example.restaurantpro.model.MenuItem;
import com.example.restaurantpro.model.RoleName;
import com.example.restaurantpro.service.AppUserService;
import com.example.restaurantpro.service.BookingService;
import com.example.restaurantpro.service.MenuService;
import com.example.restaurantpro.service.TableService;
import com.example.restaurantpro.service.VnPayService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "svg");

    private final TableService tableService;
    private final MenuService menuService;
    private final AppUserService appUserService;
    private final BookingService bookingService;
    private final VnPayService vnPayService;

    public AdminController(TableService tableService,
                           MenuService menuService,
                           AppUserService appUserService,
                           BookingService bookingService,
                           VnPayService vnPayService) {
        this.tableService = tableService;
        this.menuService = menuService;
        this.appUserService = appUserService;
        this.bookingService = bookingService;
        this.vnPayService = vnPayService;
    }

    @GetMapping
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("customerCount", appUserService.countCustomers());
        model.addAttribute("tableCount", tableService.countTables());
        model.addAttribute("bookingCount", bookingService.countBookings());
        model.addAttribute("preOrderCount", bookingService.countPreOrderedItems());
        model.addAttribute("upcomingBookings", bookingService.getUpcomingBookings(6));
        return "admin/dashboard";
    }

    @GetMapping("/revenue")
    public String revenue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Model model) {

        LocalDate now = LocalDate.now();
        LocalDate selectedDate = (date != null) ? date : now;
        int selectedMonth = (month != null) ? month : now.getMonthValue();
        int selectedYear = (year != null) ? year : now.getYear();

        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("selectedYear", selectedYear);

        model.addAttribute("dailyRevenue", bookingService.getRevenueByDate(selectedDate));
        model.addAttribute("monthlyRevenue", bookingService.getRevenueByMonth(selectedMonth, selectedYear));
        model.addAttribute("dailyPaidBookings", bookingService.countPaidBookingsByDate(selectedDate));
        model.addAttribute("monthlyPaidBookings", bookingService.countPaidBookingsByMonth(selectedMonth, selectedYear));
        model.addAttribute("revenueByDaysInMonth", bookingService.getRevenueStatsByDaysInMonth(selectedMonth, selectedYear));

        return "admin/revenue";
    }

    @GetMapping("/tables")
    public String tables(@RequestParam(required = false) Long editId,
                         @RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime monitorDateTime,
                         Model model) {
        model.addAttribute("tables", tableService.getAdminTableResponses());
        model.addAttribute("tableGroups", tableService.getTableGroups(monitorDateTime));

        if (editId != null) {
            model.addAttribute("tableForm", tableService.getTableById(editId));
        } else {
            model.addAttribute("tableForm", new DiningTable());
        }

        TableService.TableMonitoringData monitoringData = tableService.getTableMonitoringData(monitorDateTime);
        model.addAttribute("monitorDateTime", monitoringData.selectedDateTime());
        model.addAttribute("tableMonitoringRows", monitoringData.rows());
        model.addAttribute("tableZoneSummaries", monitoringData.zoneSummaries());
        model.addAttribute("totalActiveTables", monitoringData.totalActiveTables());
        model.addAttribute("totalAvailableTables", monitoringData.totalAvailableTables());

        return "admin/tables";
    }

    @GetMapping("/tables/status")
    @ResponseBody
    public TableStatusResponseDto getTableStatusApi(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkTime) {
        return tableService.getTableStatusAt(checkTime);
    }

    @PostMapping("/tables/save")
    public String saveTable(@RequestParam(required = false) Long id,
                            @RequestParam String name,
                            @RequestParam String tableNumber,
                            @RequestParam String floor,
                            @RequestParam String roomType,
                            @RequestParam String areaPosition,
                            @RequestParam Integer capacity,
                            @RequestParam String tableType,
                            @RequestParam String chairType,
                            @RequestParam String description,
                            @RequestParam(defaultValue = "false") boolean active,
                            RedirectAttributes redirectAttributes) {
        try {
            tableService.saveOrUpdate(id, name, tableNumber, floor, roomType, areaPosition, capacity, tableType, chairType, description, active);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu thông tin bàn ăn.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/tables";
    }

    @PostMapping("/tables/delete")
    public String deleteTable(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        try {
            tableService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa bàn ăn khỏi CSDL.");
        } catch (TableInUseException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/tables";
    }

    @GetMapping("/menu")
public String menu(@RequestParam(required = false) Long editId, Model model) {
    model.addAttribute("menuItems", menuService.findAllForAdmin());
    model.addAttribute("categories", List.of(MenuCategory.values()));

    if (editId != null) {
        model.addAttribute("menuItemForm", menuService.findById(editId));
    } else {
        model.addAttribute("menuItemForm", new MenuItem());
    }

    return "admin/menu";
}

    @PostMapping("/menu/save")
    public String saveMenu(@RequestParam(required = false) Long id,
                           @RequestParam String name,
                           @RequestParam MenuCategory category,
                           @RequestParam String description,
                           @RequestParam BigDecimal price,
                           @RequestParam(required = false) String imageUrl,
                           @RequestParam(required = false) MultipartFile imageFile,
                           @RequestParam(defaultValue = "false") boolean available,
                           RedirectAttributes redirectAttributes) {
        try {
            String resolvedImageUrl = storeMenuImage(imageFile);
            if (resolvedImageUrl == null || resolvedImageUrl.isBlank()) {
                resolvedImageUrl = imageUrl;
            }
            menuService.saveOrUpdate(id, name, category, description, price, resolvedImageUrl, available);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu món ăn.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/menu";
    }

    @PostMapping("/menu/delete")
    public String deleteMenu(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        try {
            menuService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xử lý xóa món ăn. Nếu món đã có trong booking cũ thì hệ thống chuyển sang ngưng phục vụ.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/menu";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", appUserService.findAllUsers());
        model.addAttribute("roles", Arrays.asList(RoleName.values()));
        return "admin/users";
    }

    @PostMapping("/users/grant")
    public String grantRole(@RequestParam Long userId,
                            @RequestParam RoleName roleName,
                            RedirectAttributes redirectAttributes) {
        try {
            boolean enabled = appUserService.toggleRole(userId, roleName);
            redirectAttributes.addFlashAttribute("successMessage",
                    enabled ? "Đã thêm quyền cho tài khoản." : "Đã gỡ quyền khỏi tài khoản.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/delete")
    public String deleteUser(@RequestParam Long userId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            String operatorPhone = authentication != null ? authentication.getName() : null;
            appUserService.deleteUser(userId, operatorPhone);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa tài khoản người dùng.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/bookings")
    public String bookings(@RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate,
                           Model model) {
        model.addAttribute("selectedDate", bookingDate);
        model.addAttribute("bookings", bookingDate == null
                ? bookingService.getAllBookings()
                : bookingService.getBookingsByDate(bookingDate));
        return "admin/bookings";
    }

    @GetMapping("/kitchen-orders")
    public String kitchenOrders(Model model) {
        model.addAttribute("bookings", bookingService.getKitchenOrdersForActiveBookings());
        return "admin/kitchen-orders";
    }

    @PostMapping("/bookings/cancel")
    public String cancelBooking(@RequestParam Long bookingId, RedirectAttributes redirectAttributes) {
        try {
            bookingService.cancelByAdmin(bookingId);
            redirectAttributes.addFlashAttribute("successMessage", "Đơn đặt bàn đã được hủy.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @PostMapping("/bookings/no-show")
    public String noShowBooking(@RequestParam Long bookingId, RedirectAttributes redirectAttributes) {
        try {
            bookingService.markNoShow(bookingId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã đánh dấu khách không đến.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/bookings";
    }

    @PostMapping("/bookings/confirm-paid")
    public String confirmPaid(@RequestParam Long bookingId,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        String operatorName = authentication != null ? authentication.getName() : "admin";
        bookingService.manuallyConfirmPaid(bookingId, operatorName);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận đơn đã thanh toán.");
        return "redirect:/admin/bookings";
    }

    @PostMapping("/bookings/refund")
    public String refundBooking(@RequestParam Long bookingId,
                                Authentication authentication,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        Booking booking = bookingService.findById(bookingId);
        String operatorName = authentication != null ? authentication.getName() : "admin";
        String message = vnPayService.refundBooking(booking, operatorName, request);
        redirectAttributes.addFlashAttribute("successMessage", message);
        return "redirect:/admin/bookings";
    }

    private String storeMenuImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }

        String originalName = StringUtils.cleanPath(imageFile.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(originalName);
        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("Tệp hình ảnh không hợp lệ.");
        }

        extension = extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Chỉ cho phép tệp ảnh: jpg, jpeg, png, gif, webp, svg.");
        }

        try {
            Path uploadDir = Paths.get("src", "main", "resources", "static", "images", "uploads");
            Files.createDirectories(uploadDir);

            String fileName = UUID.randomUUID() + "." + extension;
            Path targetPath = uploadDir.resolve(fileName);
            Files.copy(imageFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return "/images/uploads/" + fileName;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Không thể lưu hình ảnh. Vui lòng thử lại.");
        }
    }
}