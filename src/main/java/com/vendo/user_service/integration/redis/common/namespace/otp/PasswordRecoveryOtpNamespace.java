package com.vendo.user_service.integration.redis.common.namespace.otp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "redis.password-recovery")
public class PasswordRecoveryOtpNamespace extends OtpNamespace{
}
