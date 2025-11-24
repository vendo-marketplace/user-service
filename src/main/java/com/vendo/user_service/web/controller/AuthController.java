package com.vendo.user_service.web.controller;

import com.vendo.user_service.service.user.UserService;
import com.vendo.user_service.service.user.auth.AuthService;
import com.vendo.user_service.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    private final UserService userService;

    @PostMapping("/sign-in")
    ResponseEntity<AuthResponse> signIn(@Valid @RequestBody AuthRequest authRequest) {
        return ResponseEntity.ok(authService.signIn(authRequest));
    }

    @PostMapping("/sign-up")
    void signUp(@Valid @RequestBody AuthRequest authRequest) {
        authService.signUp(authRequest);
    }

    @PatchMapping("/complete-auth")
    void completeAuth(
            @RequestParam String email,
            @Valid @RequestBody CompleteAuthRequest completeAuthRequest
    ) {
        authService.completeAuth(email, completeAuthRequest);
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest refreshRequest) {
        return ResponseEntity.ok(authService.refresh(refreshRequest));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getAuthenticatedUser() {
        return ResponseEntity.ok(userService.getAuthenticatedUser());
    }

    @PostMapping("/google")
    ResponseEntity<AuthResponse> googleAuth(@Valid @RequestBody GoogleAuthRequest googleAuthRequest) {
        return ResponseEntity.ok(authService.googleAuth(googleAuthRequest));
    }
}
