package com.vendo.user_service.adapter.security.out.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "security.jwt.internal")
public class InternalJwtProperties {

    private String key;

}
