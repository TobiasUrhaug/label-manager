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
import org.omt.labelmanager.identity.domain.user.User;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
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
    void registerUser_createsUserWithEncodedPassword() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User user = userCommandApi.registerUser("new@example.com", "rawPassword", "New User");

        assertThat(user.email()).isEqualTo("new@example.com");
        assertThat(user.password()).isEqualTo("encodedPassword");
        assertThat(user.displayName()).isEqualTo("New User");

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("encodedPassword");
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
}
