package com.vendo.user_service.web.controller;

import com.vendo.user_service.system.redis.common.dto.ResetPasswordRequest;
import com.vendo.user_service.service.user.PasswordRecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/password")
public class PasswordRecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/forgot")
    void forgotPassword(@RequestParam String email) {
        passwordRecoveryService.forgotPassword(email);
    }

    @PutMapping("/reset")
    void resetPassword(
            @RequestParam String otp,
            @Valid @RequestBody ResetPasswordRequest resetPasswordRequest
    ) {
        passwordRecoveryService.resetPassword(otp ,resetPasswordRequest);
    }

    @PutMapping("/resend-otp")
    void resendOtp(@RequestParam String email) {
        passwordRecoveryService.resendOtp(email);
    }

}
