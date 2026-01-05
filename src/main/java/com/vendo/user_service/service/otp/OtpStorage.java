package com.vendo.user_service.service.otp;

import java.util.Optional;

public interface OtpStorage {

    Optional<String> getValue(String key);

    boolean hasActiveKey(String key);

    void saveValue(String key, String value, long ttl);

    void deleteValues(String... keys);

}
