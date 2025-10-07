package com.vendo.user_service.integration.redis.common.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "redis")
public class RedisProperties {

    private ResetPassword resetPassword;

    @Data
    public static class ResetPassword {

        private Prefixes prefixes;

        private long ttl;

        @Data
        public static class Prefixes {

            private String emailPrefix;

            private String tokenPrefix;
        }
    }
}
