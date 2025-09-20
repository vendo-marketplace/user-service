package com.vendo.user_service.security.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String secretKey;

    private int accessExpirationTime;

    private int refreshExpirationTime;
}
