package com.vendo.user_service.integration.redis.common.config;

import lombok.Data;

// TODO move to common
@Data
public class PrefixProperties {

    private String prefix;

    private long ttl;

    public String buildPrefix(String value) {
        return prefix + value;
    }

}
