package com.vendo.user_service.adapter.in.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/test")
public class TestInternalController {

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
