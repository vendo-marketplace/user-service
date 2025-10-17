package com.vendo.user_service.web.controller;

import com.vendo.user_service.integration.redis.common.dto.ForgotPasswordRequest;
import com.vendo.user_service.integration.redis.common.dto.ResetPasswordRequest;
import com.vendo.user_service.service.ForgotPasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/password")
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    @PostMapping("/forgot")
    void forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        forgotPasswordService.forgotPassword(forgotPasswordRequest);
    }

    @PutMapping("/reset")
    void resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        forgotPasswordService.resetPassword(resetPasswordRequest);
    }

}
