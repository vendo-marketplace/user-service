package com.vendo.user_service.web.controller;

import com.vendo.user_service.integration.redis.common.dto.ResetPasswordRequest;
import com.vendo.user_service.service.PasswordRecoveryService;
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
    void resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        passwordRecoveryService.resetPassword(resetPasswordRequest);
    }

    @PutMapping("/resend")
    void resendOtp(@RequestParam String email) {
        passwordRecoveryService.resendOtp(email);
    }

}
