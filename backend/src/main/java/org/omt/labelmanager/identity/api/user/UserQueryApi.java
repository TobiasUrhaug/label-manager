package org.omt.labelmanager.identity.api.user;

/** Read-side contract other modules use to ask questions about users. */
public interface UserQueryApi {

    /**
     * Reports whether a user with this id exists.
     *
     * @param id the user id
     * @return true if a user with that id is stored
     */
    boolean exists(Long id);
}
