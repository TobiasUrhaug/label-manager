package org.omt.labelmanager.identity.application;

import org.omt.labelmanager.identity.api.user.EmailAlreadyExistsException;
import org.omt.labelmanager.identity.api.user.UserCommandApi;
import org.omt.labelmanager.identity.domain.user.User;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
class UserCommandApiImpl implements UserCommandApi {

    private static final Logger log = LoggerFactory.getLogger(UserCommandApiImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    UserCommandApiImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
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
}
