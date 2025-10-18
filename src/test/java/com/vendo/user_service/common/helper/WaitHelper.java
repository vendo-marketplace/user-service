package com.vendo.user_service.common.helper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WaitHelper {

    public static void waitSafely(long mills) {
        try {
            Thread.sleep(mills);
        } catch (InterruptedException e) {
            log.error("Error waiting ", e);
            Thread.currentThread().interrupt();
        }
    }
}
