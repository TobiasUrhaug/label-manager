package org.omt.labelmanager.identity.application;

import org.omt.labelmanager.identity.api.user.EmailAlreadyExistsException;
import org.omt.labelmanager.identity.api.user.UserCommandApi;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserEntity;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void registerUser(String email, String password, String displayName) {
        log.info("Registering new user with email '{}'", email);

        if (userRepository.existsByEmail(email)) {
            log.warn("Registration failed: email '{}' already exists", email);
            throw new EmailAlreadyExistsException(email);
        }

        String encodedPassword = passwordEncoder.encode(password);
        UserEntity entity = new UserEntity(email, encodedPassword, displayName);

        try {
            UserEntity savedEntity = userRepository.save(entity);
            log.debug("User registered successfully with id {}", savedEntity.getId());
        } catch (DataIntegrityViolationException e) {
            // The existsByEmail check above is not atomic with the insert. A concurrent
            // registration for the same address passes both checks and one of the two loses
            // the unique index on app_user.email. That is the same outcome as the check
            // catching it, so it gets the same 409 rather than a 500.
            log.warn("Registration failed: email '{}' was taken concurrently", email);
            throw new EmailAlreadyExistsException(email);
        }
    }
}
