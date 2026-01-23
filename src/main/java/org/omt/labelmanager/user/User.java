package org.omt.labelmanager.user;

import java.time.Instant;
import org.omt.labelmanager.user.persistence.UserEntity;

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
