package com.vendo.user_service.service;

import com.vendo.user_service.builder.UserDataBuilder;
import com.vendo.user_service.common.exception.UserAlreadyExistsException;
import com.vendo.user_service.model.User;
import com.vendo.user_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void save_shouldPersistUserInDatabase() {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);

        userService.save(user);

        verify(userRepository).findByEmail(user.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void save_shouldThrowException_whenUserAlreadyExists() {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(UserAlreadyExistsException.class, () -> userService.save(user));

        verify(userRepository).findByEmail(user.getEmail());
        verify(userRepository, never()).save(user);
    }

    @Test
    void findByEmailOrThrow_shouldReturnUser() {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        userService.findByEmailOrThrow(user.getEmail());

        verify(userRepository).findByEmail(user.getEmail());
    }

    @Test
    void findByEmailOrThrow_shouldThrowException_whenUserNotFound() {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.findByEmailOrThrow(user.getEmail()));

        verify(userRepository).findByEmail(user.getEmail());
    }

    @Test
    void throwIfUserExistsByEmail_shouldNotThrowException_whenUserDoesNotExist() {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        userService.throwIfUserExistsByEmail(user.getEmail());

        verify(userRepository).findByEmail(user.getEmail());
    }

    @Test
    void throwIfUserExistsByEmail_shouldThrowException_whenUserExists() {
        User user = UserDataBuilder.buildUserWithRequiredFields().build();

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(UserAlreadyExistsException.class, () -> userService.throwIfUserExistsByEmail(user.getEmail()));

        verify(userRepository).findByEmail(user.getEmail());
    }
}
