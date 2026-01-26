package org.omt.labelmanager.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.omt.labelmanager.identity.domain.user.EmailAlreadyExistsException;
import org.omt.labelmanager.identity.domain.user.User;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserCRUDHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserCRUDHandler userCRUDHandler;

    @BeforeEach
    void setUp() {
        userCRUDHandler = new UserCRUDHandler(userRepository, passwordEncoder);
    }

    @Test
    void registerUser_createsUserWithEncodedPassword() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity entity = invocation.getArgument(0);
            return entity;
        });

        User user = userCRUDHandler.registerUser("new@example.com", "rawPassword", "New User");

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

        assertThatThrownBy(() ->
                userCRUDHandler.registerUser("existing@example.com", "password", "Name"))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("existing@example.com");
    }

    @Test
    void findByEmail_returnsUser_whenExists() {
        var entity = new UserEntity("find@example.com", "pwd", "Found User");
        when(userRepository.findByEmail("find@example.com")).thenReturn(Optional.of(entity));

        Optional<User> user = userCRUDHandler.findByEmail("find@example.com");

        assertThat(user).isPresent();
        assertThat(user.get().email()).isEqualTo("find@example.com");
    }

    @Test
    void findByEmail_returnsEmpty_whenNotExists() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        Optional<User> user = userCRUDHandler.findByEmail("notfound@example.com");

        assertThat(user).isEmpty();
    }

    @Test
    void emailExists_returnsTrueOrFalse() {
        when(userRepository.existsByEmail("exists@example.com")).thenReturn(true);
        when(userRepository.existsByEmail("notexists@example.com")).thenReturn(false);

        assertThat(userCRUDHandler.emailExists("exists@example.com")).isTrue();
        assertThat(userCRUDHandler.emailExists("notexists@example.com")).isFalse();
    }
}
