package org.omt.labelmanager.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omt.labelmanager.identity.api.user.EmailAlreadyExistsException;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserCommandApiImplTest {

    @Mock private UserRepository userRepository;

    @Mock private PasswordEncoder passwordEncoder;

    private UserCommandApiImpl userCommandApi;

    @BeforeEach
    void setUp() {
        userCommandApi = new UserCommandApiImpl(userRepository, passwordEncoder);
    }

    @Test
    void registerUser_storesTheUserWithAnEncodedPassword() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        userCommandApi.registerUser("new@example.com", "rawPassword", "New User");

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("encodedPassword");
        assertThat(captor.getValue().getDisplayName()).isEqualTo("New User");
    }

    @Test
    void registerUser_throwsException_whenEmailExists() {
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                userCommandApi.registerUser(
                                        "existing@example.com", "password", "Name"))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("existing@example.com");
    }

    @Test
    void registerUser_throwsEmailAlreadyExists_whenAConcurrentInsertWinsTheRace() {
        when(userRepository.existsByEmail("racing@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(UserEntity.class)))
                .thenThrow(new DataIntegrityViolationException("app_user_email_key"));

        assertThatThrownBy(
                        () -> userCommandApi.registerUser("racing@example.com", "password", "Name"))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("racing@example.com");
    }
}
