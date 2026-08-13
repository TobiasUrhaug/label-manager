package org.omt.labelmanager.identity.application;

import org.omt.labelmanager.identity.api.user.UserQueryApi;
import org.omt.labelmanager.identity.infrastructure.persistence.user.UserRepository;
import org.springframework.stereotype.Service;

@Service
class UserQueryApiImpl implements UserQueryApi {

    private final UserRepository userRepository;

    UserQueryApiImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean exists(Long id) {
        return userRepository.existsById(id);
    }
}
