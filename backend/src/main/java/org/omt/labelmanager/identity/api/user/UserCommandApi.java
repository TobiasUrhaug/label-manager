package org.omt.labelmanager.identity.api.user;

/** Write-side contract for creating users. */
public interface UserCommandApi {

    /**
     * Registers a new user with an encoded password.
     *
     * <p>Returns nothing: the stored user carries the bcrypt hash, and no caller has a reason to
     * see it.
     *
     * @param email the email address, which doubles as the login name
     * @param password the raw password, encoded before it is stored
     * @param displayName the name shown in the UI
     * @throws EmailAlreadyExistsException if a user with that email is already stored
     */
    void registerUser(String email, String password, String displayName);
}
