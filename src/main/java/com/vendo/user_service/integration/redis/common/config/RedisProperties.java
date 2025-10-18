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

    private PasswordRecovery passwordRecovery;

    @Data
    public static class PasswordRecovery {

        private PrefixProperties email;

        private PrefixProperties otp;

        private PrefixProperties attempts;

        @Data
        public static class PrefixProperties {

            private String prefix;

            private long ttl;
            
            public String buildPrefix(String value) {
                return prefix + value;
            }
        }
    }
}
