package com.vendo.user_service.service.user;

import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.security.common.exception.AccessDeniedException;
import com.vendo.user_service.common.exception.UserAlreadyExistsException;
import com.vendo.user_service.common.type.UserRole;
import com.vendo.user_service.integration.kafka.event.EmailOtpEvent;
import com.vendo.user_service.integration.redis.common.dto.ValidateRequest;
import com.vendo.user_service.integration.redis.common.namespace.otp.EmailVerificationOtpNamespace;
import com.vendo.user_service.model.User;
import com.vendo.user_service.security.common.dto.TokenPayload;
import com.vendo.user_service.security.service.JwtService;
import com.vendo.user_service.security.service.JwtUserDetailsService;
import com.vendo.user_service.service.otp.EmailOtpService;
import com.vendo.user_service.web.dto.AuthRequest;
import com.vendo.user_service.web.dto.AuthResponse;
import com.vendo.user_service.web.dto.RefreshRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.vendo.user_service.integration.kafka.event.EmailOtpEvent.OtpEventType.EMAIL_VERIFICATION;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;

    private final JwtService jwtService;

    private final PasswordEncoder passwordEncoder;

    private final JwtUserDetailsService jwtUserDetailsService;

    private final EmailOtpService emailOtpService;

    private final EmailVerificationOtpNamespace emailVerificationOtpNamespace;

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
        userService.findByEmail(authRequest.email()).ifPresent(user -> {
            throw new UserAlreadyExistsException("User with this email already exists.");
        });

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

    public void sendOtp(String email) {
        userService.findByEmailOrThrow(email);

        EmailOtpEvent event = EmailOtpEvent.builder()
                .email(email)
                .otpEventType(EMAIL_VERIFICATION)
                .build();
        emailOtpService.sendOtp(event, emailVerificationOtpNamespace);
    }

    public void resendOtp(String email) {
        userService.findByEmailOrThrow(email);

        EmailOtpEvent event = EmailOtpEvent.builder()
                .email(email)
                .otpEventType(EMAIL_VERIFICATION)
                .build();
        emailOtpService.resendOtp(event, emailVerificationOtpNamespace);
    }

    public void validate(ValidateRequest validateRequest) {
        User user = userService.findByEmailOrThrow(validateRequest.email());

        emailOtpService.verifyOtp(validateRequest.otp(), validateRequest.email(), emailVerificationOtpNamespace);

        userService.update(user.getId(), User.builder()
                .status(UserStatus.ACTIVE)
                .build());
    }

    private void matchPasswordsOrThrow(String rawPassword, String encodedPassword) {
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        if (!matches) {
            throw new BadCredentialsException("Wrong credentials");
        }
    }
}
