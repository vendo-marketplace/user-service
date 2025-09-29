package com.vendo.user_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;

    public void saveToken(String token, String userId, long seconds) {
        redisTemplate.opsForValue().set(token, userId, Duration.ofSeconds(seconds));
    }

    public String getUserId(String token) {
        return redisTemplate.opsForValue().get(token);
    }

}
