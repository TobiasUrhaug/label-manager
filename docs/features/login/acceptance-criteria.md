# Acceptance Criteria — Login

## AC-01: Successful login (US-01)
**Given** I am on the login page
**And** I enter valid credentials
**When** I submit the form
**Then** I am authenticated and redirected to the application root (`/`)

## AC-02: Failed login — wrong credentials (US-02)
**Given** I am on the login page
**When** I submit with an incorrect username or password
**Then** I remain on the login page
**And** an error message is displayed that does not reveal which field was wrong

## AC-03: Failed login — empty fields (US-02)
**Given** I am on the login page
**When** I submit with an empty username or empty password
**Then** the form prevents submission and shows a field-level validation message

## AC-04: Logout (US-03)
**Given** I am logged in
**When** I trigger the logout action
**Then** my session is invalidated on the server
**And** I am redirected to the login page

## AC-05: Redirect when unauthenticated (US-04)
**Given** I am not logged in (or my session has expired)
**When** I navigate to any protected route
**Then** I am redirected to the login page

## AC-06: Login page bypassed when already authenticated (US-01)
**Given** I am already logged in
**When** I navigate to `/login`
**Then** I am redirected to the application root (`/`)
