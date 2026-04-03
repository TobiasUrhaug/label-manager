# Business Rules — Login

## BR-01: Credentials required
A user must provide a username and password to access the application.
**Rationale:** Prevents unauthorized access to label data.

## BR-02: Invalid credentials must be rejected with a generic error
The system must reject login attempts with incorrect username or password and display
an error message. The error message must not indicate which field is wrong.
**Rationale:** Generic messages prevent username enumeration attacks.

## BR-03: No self-registration (this iteration)
Users cannot create their own accounts in this iteration. Accounts are provisioned
by an administrator out of band.
**Rationale:** Self-registration is deferred to a future feature. For the initial
migration, controlled provisioning is sufficient.

## BR-04: All app routes require a valid session
Every route other than `/login` must redirect to `/login` if no valid session exists.
**Rationale:** Unauthenticated users must not see or interact with any application data.

## BR-05: Logout invalidates the server-side session
Logging out must invalidate the server-side session and clear the session cookie.
**Rationale:** Prevents session reuse after the user has explicitly logged out.
