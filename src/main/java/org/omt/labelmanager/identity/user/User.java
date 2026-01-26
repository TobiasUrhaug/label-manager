package org.omt.labelmanager.identity.user;

import java.time.Instant;
import org.omt.labelmanager.identity.user.persistence.UserEntity;

public record User(Long id, String email, String password, String displayName, Instant createdAt) {

    public static User fromEntity(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getDisplayName(),
                entity.getCreatedAt()
        );
    }
}
