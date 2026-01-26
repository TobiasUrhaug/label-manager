package org.omt.labelmanager.identity.application;

import java.util.Optional;
import org.omt.labelmanager.identity.domain.user.EmailAlreadyExistsException;
import org.omt.labelmanager.identity.domain.user.User;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserCRUDHandler {

    private static final Logger log = LoggerFactory.getLogger(UserCRUDHandler.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserCRUDHandler(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String email, String password, String displayName) {
        log.info("Registering new user with email '{}'", email);

        if (userRepository.existsByEmail(email)) {
            log.warn("Registration failed: email '{}' already exists", email);
            throw new EmailAlreadyExistsException(email);
        }

        String encodedPassword = passwordEncoder.encode(password);
        UserEntity entity = new UserEntity(email, encodedPassword, displayName);
        UserEntity savedEntity = userRepository.save(entity);

        log.debug("User registered successfully with id {}", savedEntity.getId());
        return User.fromEntity(savedEntity);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email).map(User::fromEntity);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id).map(User::fromEntity);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}
