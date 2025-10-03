package com.vendo.user_service.integration.redis.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    public void saveToken(String token, String id, long seconds) {
        redisTemplate.opsForValue().set(token, id, Duration.ofSeconds(seconds));
    }

    public Optional<String> getId(String token) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(token));
    }

    public boolean hasActiveToken(String token) {
        return redisTemplate.hasKey(token);
    }

}
