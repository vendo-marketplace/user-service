package com.vendo.user_service.adapter.in.security;

import com.vendo.user_service.adapter.in.security.dto.PingRequest;
import com.vendo.user_service.adapter.in.security.dto.PingResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/test")
public class TestInternalController {

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @PostMapping("/ping")
    public PingResponse ping(@RequestBody PingRequest request) {
        return new PingResponse(request.content());
    }
}
