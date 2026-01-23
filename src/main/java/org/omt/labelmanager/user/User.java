package org.omt.labelmanager.user;

import java.time.Instant;

public record User(Long id, String email, String password, String displayName, Instant createdAt) {
}
