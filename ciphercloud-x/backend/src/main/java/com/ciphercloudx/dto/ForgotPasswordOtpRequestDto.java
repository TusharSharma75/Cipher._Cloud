package com.ciphercloudx.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordOtpRequestDto {

    @Email
    @NotBlank
    private String email;

}