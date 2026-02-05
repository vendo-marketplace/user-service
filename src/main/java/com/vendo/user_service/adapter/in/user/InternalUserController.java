package com.vendo.user_service.adapter.in.user;

import com.vendo.user_service.application.InternalUserService;
import com.vendo.user_service.domain.user.dto.SaveUserRequest;
import com.vendo.user_service.domain.user.dto.UpdateUserRequest;
import com.vendo.user_service.domain.user.dto.ExistsUserResponse;
import com.vendo.user_service.domain.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class InternalUserController {

    private final InternalUserService internalUserService;

    @GetMapping
    ResponseEntity<User> getByEmail(@RequestParam String email) {
        return ResponseEntity.ok(internalUserService.getByEmail(email));
    }

    @GetMapping("/exists")
    ResponseEntity<ExistsUserResponse> existsByEmail(@RequestParam String email) {
        return ResponseEntity.ok(internalUserService.existsByEmail(email));
    }

    @PutMapping
    void update(@RequestParam String id, @RequestBody UpdateUserRequest updateUserRequest) {
        internalUserService.update(id, updateUserRequest);
    }

    @PostMapping
    ResponseEntity<User> save(@Valid @RequestBody SaveUserRequest saveUserRequest) {
        return ResponseEntity.ok(internalUserService.save(saveUserRequest));
    }
}