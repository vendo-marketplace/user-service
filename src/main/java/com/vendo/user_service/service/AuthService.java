package com.vendo.user_service.service;

import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.integration.redis.common.exception.RedisValueExpiredException;
import com.vendo.security.common.exception.AccessDeniedException;
import com.vendo.user_service.common.exception.InvalidOtpForEmailException;
import com.vendo.user_service.common.exception.OtpAlreadySentException;
import com.vendo.user_service.common.exception.OtpTooManyRequestsException;
import com.vendo.user_service.common.type.UserRole;
import com.vendo.user_service.integration.kafka.producer.EmailVerificationEventProducer;
import com.vendo.user_service.integration.redis.common.config.RedisProperties;
import com.vendo.user_service.integration.redis.common.dto.UpdateUserRequest;
import com.vendo.user_service.integration.redis.common.dto.VerifyEmailRequest;
import com.vendo.user_service.integration.redis.service.RedisService;
import com.vendo.user_service.model.User;
import com.vendo.user_service.security.common.dto.TokenPayload;
import com.vendo.user_service.security.service.JwtService;
import com.vendo.user_service.security.service.JwtUserDetailsService;
import com.vendo.user_service.web.dto.AuthRequest;
import com.vendo.user_service.web.dto.AuthResponse;
import com.vendo.user_service.web.dto.RefreshRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;

    private final JwtService jwtService;

    private final PasswordEncoder passwordEncoder;

    private final JwtUserDetailsService jwtUserDetailsService;

    private final RedisService redisService;

    private final EmailVerificationEventProducer emailVerificationEventProducer;

    private final RedisProperties redisProperties;


    public AuthResponse signIn(AuthRequest authRequest) {
        User user = userService.findByEmailOrThrow(authRequest.email());

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("User is unactive.");
        }
        matchPasswordsOrThrow(authRequest.password(), user.getPassword());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void signUp(AuthRequest authRequest) {
        userService.throwIfUserExistsByEmail(authRequest.email());
        String encodedPassword = passwordEncoder.encode(authRequest.password());

        userService.save(User.builder()
                .email(authRequest.email())
                .role(UserRole.USER)
                .status(UserStatus.INCOMPLETE)
                .password(encodedPassword)
                .build());
    }

    public AuthResponse refresh(RefreshRequest refreshRequest) {
        UserDetails userDetails = jwtUserDetailsService.retrieveUserDetails(refreshRequest.refreshToken());
        TokenPayload tokenPayload = jwtUserDetailsService.generateTokenPayload(userDetails);

        return AuthResponse.builder()
                .accessToken(tokenPayload.accessToken())
                .refreshToken(tokenPayload.refreshToken())
                .build();
    }

    public void sendVerificationCode(String email) {
        userService.findByEmailOrThrow(email);
        RedisProperties.EmailVerification verificationProperties = redisProperties.getEmailVerification();

        if (redisService.hasActiveKey(verificationProperties.getEmail().buildPrefix(email))) {
            throw new OtpAlreadySentException("Otp has already sent to email.");
        }

        String otp = generateOtp();
        redisService.saveValue(verificationProperties.getOtp().buildPrefix(otp), email, verificationProperties.getOtp().getTtl());
        redisService.saveValue(verificationProperties.getEmail().buildPrefix(email), otp, verificationProperties.getEmail().getTtl());

        emailVerificationEventProducer.sendEmailVerificationEvent(email);
    }

    public void resendVerificationCode(String email) {
        RedisProperties.EmailVerification verificationProperties = redisProperties.getEmailVerification();

        userService.findByEmailOrThrow(email);
        redisService.getValue(verificationProperties.getEmail().buildPrefix(email))
                .orElseThrow(() -> new RedisValueExpiredException("Verification session expired."));

        Optional<String> attempts = redisService.getValue(verificationProperties.getAttempts().buildPrefix(email));
        increaseResendAttemptsOrThrow(email, attempts);

        Optional<String> otp = redisService.getValue(verificationProperties.getEmail().buildPrefix(email));
        resendOrGenerateOtp(email, otp);
    }

    public void verifyVerificationCode(VerifyEmailRequest verifyEmailRequest) {
        RedisProperties.EmailVerification verificationProperties = redisProperties.getEmailVerification();

        String email = redisService.getValue(verificationProperties.getOtp().buildPrefix(String.valueOf(verifyEmailRequest.otp())))
                .orElseThrow(() -> new RedisValueExpiredException("Otp has expired."));

        User user = userService.findByEmailOrThrow(verifyEmailRequest.email());

        if (!email.equals(verifyEmailRequest.email())) {
            throw new InvalidOtpForEmailException("Invalid verification code for this email.");
        }

        userService.update(user.getId(), UpdateUserRequest.builder()
                .status(UserStatus.ACTIVE)
                .build());

        redisService.deleteValues(
                verificationProperties.getOtp().buildPrefix(String.valueOf(verifyEmailRequest.otp())),
                verificationProperties.getEmail().buildPrefix(verifyEmailRequest.email()),
                verificationProperties.getAttempts().buildPrefix(verifyEmailRequest.email())
        );
    }

    private void increaseResendAttemptsOrThrow(String email, Optional<String> attempts) {
        RedisProperties.EmailVerification verificationProperties = redisProperties.getEmailVerification();
        int attempt = attempts.map(Integer::parseInt).orElse(0);

        if (attempt >= 3) {
            throw new OtpTooManyRequestsException("Reached maximum attempts for resending otp code.");
        }

        redisService.saveValue(
                verificationProperties.getAttempts().buildPrefix(email),
                String.valueOf(attempt + 1),
                verificationProperties.getAttempts().getTtl());
    }

    private void resendOrGenerateOtp(String email, Optional<String> otp) {
        RedisProperties.EmailVerification verificationProperties = redisProperties.getEmailVerification();

        if (otp.isEmpty()) {
            redisService.saveValue(verificationProperties.getEmail().buildPrefix(email), generateOtp(), verificationProperties.getOtp().getTtl());
        }

        emailVerificationEventProducer.sendEmailVerificationEvent(email);
    }

    private String generateOtp() {
        int maxSixDigitNumber = new Random().nextInt(1_000_000);
        return String.format("%06d", maxSixDigitNumber);
    }

    private void matchPasswordsOrThrow(String rawPassword, String encodedPassword) {
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        if (!matches) {
            throw new BadCredentialsException("Wrong credentials");
        }
    }
}
