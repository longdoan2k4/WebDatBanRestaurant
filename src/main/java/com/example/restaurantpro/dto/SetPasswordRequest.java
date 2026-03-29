package com.example.restaurantpro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SetPasswordRequest {

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^[0-9]{8,15}$", message = "Số điện thoại phải từ 8-15 chữ số")
    private String phone;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    private String confirmPassword;

    public String getPassword() {
        return password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}