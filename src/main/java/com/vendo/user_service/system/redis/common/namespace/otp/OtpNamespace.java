package com.vendo.user_service.system.redis.common.namespace.otp;

import com.vendo.integration.redis.common.config.PrefixProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class OtpNamespace {

    private PrefixProperties email;

    private PrefixProperties otp;

    private PrefixProperties attempts;

}
