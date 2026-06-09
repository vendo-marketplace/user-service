package com.vendo.user_service.test_utils.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ping")
public class InternalPingController {

    @PostMapping("/pong")
    public String pong(@RequestBody PongRequest request) {
        return request.content();
    }
}
