package com.example.restaurantpro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Vui lòng nhập họ tên.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(regexp = "^(0|\\+84)\\d{9,10}$", message = "Số điện thoại chưa đúng định dạng.")
    private String phone;

    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    @Size(min = 6, message = "Mật khẩu cần ít nhất 6 ký tự.")
    private String password;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu.")
    private String confirmPassword;

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
