# Login

## Problem Statement
The Label Manager React SPA requires users to authenticate before accessing any
application screen. Currently, authentication is handled by a Thymeleaf login page
backed by Spring Security. As the app migrates to React, the login screen must be
reimplemented as a React page that authenticates against the existing Spring Security
endpoint.

## Background
The backend uses Spring Security with form-based authentication. Successful
authentication establishes a server-side session identified by a `JSESSIONID` cookie.
All API calls from the React SPA use this session cookie for authorization. The existing
Thymeleaf login page will be replaced by the React login page.

## Key Decisions
- Authentication mechanism: Spring Security form login (POST to `/login`) — no backend changes
- Session management: cookie-based (`JSESSIONID`), handled transparently by the browser
- No self-registration: user accounts are managed out of band (admin creates users)
- After successful login, redirect to the application root (`/`)
- After logout, redirect to the login page

## Scope

**In scope:**
- Login form (username + password)
- Error feedback on invalid credentials
- Logout action
- Redirect to login when session is absent or expired

**Out of scope (this iteration):**
- User registration / account creation (planned for a future feature)
- Password reset / forgot password
- Remember-me / persistent sessions
- Multi-factor authentication
