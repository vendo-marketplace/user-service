package com.vendo.user_service.service;

import com.vendo.domain.user.common.type.ProviderType;
import com.vendo.domain.user.common.type.UserStatus;
import com.vendo.user_service.common.builder.UserDataBuilder;
import com.vendo.user_service.common.exception.UserAlreadyExistsException;
import com.vendo.user_service.common.type.UserRole;
import com.vendo.user_service.mapper.UserMapper;
import com.vendo.user_service.model.User;
import com.vendo.user_service.repository.UserRepository;
import com.vendo.user_service.service.user.UserService;
import com.vendo.user_service.web.dto.UserProfileResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Test
    void save_shouldSaveUser_whenEmailDoesNotExist() {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);

        User savedUser = userService.save(user);

        assertThat(savedUser).isNotNull();
        assertThat(savedUser).isEqualTo(user);

        verify(userRepository, times(1)).save(user);
    }

    @Test
    void save_shouldThrowUserAlreadyExistsException_whenEmailAlreadyExists() {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.save(user))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage("User already exists.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void update_shouldUpdateExistingUserFields() {
        User existingUser = UserDataBuilder.buildUserWithRequiredFields().build();
        User requestUser = UserDataBuilder.buildUserWithRequiredFields()
                .status(UserStatus.ACTIVE)
                .providerType(ProviderType.GOOGLE)
                .password("AnotherPassword12345@")
                .build();

        when(userRepository.findById("1")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        userService.update("1", requestUser);

        verify(userRepository, times(1)).findById("1");
        verify(userRepository, times(1)).save(existingUser);

        assertThat(existingUser.getPassword()).isEqualTo(requestUser.getPassword());
        assertThat(existingUser.getStatus()).isEqualTo(requestUser.getStatus());
        assertThat(existingUser.getProviderType()).isEqualTo(requestUser.getProviderType());
        assertThat(existingUser.getUpdatedAt()).isNotNull();
    }

    @Test
    void update_shouldUpdatePartialFields_whenRequestHasPartialData() {
        User existingUser = UserDataBuilder.buildUserWithRequiredFields().build();
        User requestUser = User.builder()
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById("1")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        userService.update("1", requestUser);

        assertThat(existingUser.getStatus()).isEqualTo(requestUser.getStatus());

        assertThat(existingUser.getPassword()).isNotNull();
        assertThat(existingUser.getProviderType()).isNotNull();
        assertThat(existingUser.getEmail()).isNotNull();
        assertThat(existingUser.getRole()).isNotNull();
        assertThat(existingUser.getFullName()).isNotNull();
        assertThat(existingUser.getBirthDate()).isNotNull();
        assertThat(existingUser.getUpdatedAt()).isNotNull();
        assertThat(existingUser.getCreatedAt()).isNotNull();
    }

    @Test
    void update_shouldThrowUsernameNotFoundException_whenUserDoesNotExist() {
        User requestUser = UserDataBuilder.buildUserWithRequiredFields().build();

        when(userRepository.findById("1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update("1", requestUser))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void findUserByEmailOrSave_shouldReturnExistingUser() {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        User resultUser = userService.findUserByEmailOrSave(user.getEmail());

        assertThat(resultUser).isNotNull();
        assertThat(resultUser).isEqualTo(user);
        assertThat(resultUser.getCreatedAt()).isNotNull();
        assertThat(resultUser.getUpdatedAt()).isNotNull();

        verify(userRepository, never()).save(any());
    }

    @Test
    void findByEmailOrSave_shouldSaveNewUser_whenNotFound() {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setCreatedAt(Instant.now());
            u.setUpdatedAt(Instant.now());
            return u;
        });

        User result = userService.findUserByEmailOrSave(user.getEmail());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User capturedUser = captor.getValue();

        assertThat(capturedUser.getEmail()).isEqualTo(user.getEmail());
        assertThat(capturedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(capturedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(capturedUser.getProviderType()).isEqualTo(ProviderType.LOCAL);

        assertThat(capturedUser.getCreatedAt()).isNotNull();
        assertThat(capturedUser.getUpdatedAt()).isNotNull();

        assertThat(capturedUser.getId()).isNull();
        assertThat(capturedUser.getFullName()).isNull();
        assertThat(capturedUser.getBirthDate()).isNull();

        assertThat(result).isEqualTo(capturedUser);
    }

    @Test
    void findByUserIdOrThrow_shouldReturnUser_whenFound() {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        User result = userService.findByUserIdOrThrow("1");

        assertThat(result).isEqualTo(user);
        assertThat(result.getId()).isEqualTo(user.getId());
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
        assertThat(result.getRole()).isEqualTo(user.getRole());
        assertThat(result.getStatus()).isEqualTo(user.getStatus());
        assertThat(result.getProviderType()).isEqualTo(user.getProviderType());
        assertThat(result.getFullName()).isEqualTo(user.getFullName());
        assertThat(result.getBirthDate()).isEqualTo(user.getBirthDate());
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByUserIdOrThrow_shouldThrowUsernameNotFoundException_whenNotFound() {
        when(userRepository.findById("1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUserIdOrThrow("1"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found.");
    }

    @Test
    void findByEmailOrThrow_shouldReturnUser_whenFound() {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        User result = userService.findByEmailOrThrow(user.getEmail());

        assertThat(result).isEqualTo(user);
    }

    @Test
    void findByEmailOrThrow_shouldThrowUsernameNotFoundException_whenNotFound() {
        String email = "notfound@gmail.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmailOrThrow(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found.");
    }

    @Test
    void getCurrentUser_shouldReturnUserProfile_whenAuthenticated() {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();
        UserProfileResponse expectedProfile = UserDataBuilder.buildUserProfileResponse(user);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);
        when(userMapper.toUserProfileResponse(user)).thenReturn(expectedProfile);

        UserProfileResponse result = userService.getCurrentUser();

        assertThat(result).isEqualTo(expectedProfile);
        assertThat(result.createdAt()).isEqualTo(user.getCreatedAt());
        assertThat(result.updatedAt()).isEqualTo(user.getUpdatedAt());

    }

    @Test
    void getCurrentUser_shouldThrowException_whenAuthenticationIsNull() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
                .hasMessage("User is not authenticated.");
    }
}
