package org.omt.labelmanager.identity.api.user;

import org.omt.labelmanager.identity.domain.user.User;

/** Write-side contract for creating users. */
public interface UserCommandApi {

    /**
     * Registers a new user with an encoded password.
     *
     * @param email the email address, which doubles as the login name
     * @param password the raw password, encoded before it is stored
     * @param displayName the name shown in the UI
     * @return the stored user
     * @throws EmailAlreadyExistsException if a user with that email is already stored
     */
    User registerUser(String email, String password, String displayName);
}
