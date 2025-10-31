package com.vendo.user_service.service.otp;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class OtpGenerator {

    private static final int ONE_MILLION_BOUND = 1_000_000;

    private static final String SIX_DIGIT_PATTERN = "%06d";

    public String generateSixDigitOtp() {
        int maxSixDigitNumber = new Random().nextInt(ONE_MILLION_BOUND);
        return String.format(SIX_DIGIT_PATTERN, maxSixDigitNumber);
    }
}
