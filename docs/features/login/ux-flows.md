# UX Flows — Login

## Flow Index

| ID | Name |
|----|------|
| F-01 | Successful Login |
| F-02 | Failed Login — Wrong Credentials |
| F-03 | Failed Login — Empty Fields |
| F-04 | Logout |
| F-05 | Redirect When Unauthenticated |
| F-06 | Already-Authenticated User Visits /login |

---

## F-01 Successful Login

**Entry point:** User navigates to `/login` directly, or is redirected there from a protected route.

1. The Login Page (S-01) is displayed in its Default state with empty fields.
2. User enters a valid username and password.
3. User submits the form (clicks "Log in" or presses Enter).
4. The form enters its Loading state (button disabled, no spinner required).
5. The server responds with success.
6. User is redirected to the originally requested URL if one was stored; otherwise to the application home page (`/`).

**Exit point:** User lands on the home page (or the originally requested protected page).

---

## F-02 Failed Login — Wrong Credentials

**Entry point:** User is on the Login Page (S-01) with both fields filled in.

1. User enters an incorrect username and/or password.
2. User submits the form.
3. The form enters its Loading state.
4. The server responds with an authentication failure.
5. The page re-renders in its Error state: an inline error message is shown above the form fields ("Invalid username or password.").
6. Both fields are cleared so the user can retry.
7. Focus is moved to the username field.

**Exit point:** User corrects credentials and proceeds via F-01, or abandons the page.

---

## F-03 Failed Login — Empty Fields

**Entry point:** User is on the Login Page (S-01) with one or both fields empty.

1. User submits the form without filling in all required fields.
2. Client-side validation fires before the request is sent.
3. Required-field indicators are shown on the empty field(s).
4. The request is not submitted.

**Exit point:** User fills in the missing fields and proceeds via F-01 or F-02.

---

## F-04 Logout

**Entry point:** User is authenticated and viewing any page that exposes a logout action.

1. User activates the logout action (e.g. a "Log out" link or button in the navigation).
2. A logout request is sent to the server.
3. The server invalidates the session.
4. User is redirected to `/login`.

**Exit point:** User is on the Login Page (S-01) in its Default state.

---

## F-05 Redirect When Unauthenticated

**Entry point:** User (unauthenticated) attempts to navigate directly to a protected URL.

1. The server (or frontend route guard) detects that the user is not authenticated.
2. The originally requested URL is stored so it can be used after a successful login.
3. User is redirected to `/login`.
4. The Login Page (S-01) is displayed in its Default state.
5. User completes login via F-01; on success they are forwarded to the originally requested URL.

**Exit point:** User lands on the protected page they originally requested.

---

## F-06 Already-Authenticated User Visits /login

**Entry point:** A currently authenticated user navigates to `/login` (e.g. by typing the URL directly).

1. The server (or frontend route guard) detects an active session.
2. User is immediately redirected to the application home page (`/`).

**Exit point:** User lands on the home page without seeing the login form.
