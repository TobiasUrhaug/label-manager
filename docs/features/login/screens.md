# Screens — Login

## Screen Index

| ID | Name | Route |
|----|------|-------|
| S-01 | Login Page | `/login` |

---

## S-01 Login Page

**Route:** `/login`

**Appears in flows:** F-01, F-02, F-03, F-04 (exit), F-05, F-06 (entry)

**Purpose:** Collect and submit user credentials. The only publicly accessible page in the application.

---

### Layout

A centered, single-column card on a neutral background. No application navigation is shown (the user is not yet authenticated). The card contains:

1. Application name / logo (top of card)
2. Form (username field, password field, submit button)
3. Inline error message area (between the logo and the form; hidden unless there is an error)

---

### Components

| Component | Type | Notes |
|-----------|------|-------|
| Username field | Text input | Label "Username", required, autofocused on page load |
| Password field | Password input | Label "Password", required, masks input |
| Log in button | Primary submit button | Full width of the form; disabled during Loading state |
| Inline error message | Alert / error text | Shown only in Error state; text: "Invalid username or password." |
| Required-field indicator | Validation hint | Shown on empty field(s) when form is submitted without values (F-03) |

---

### States

#### Default
The page as it appears on first load or after a successful logout redirect. Both fields are empty. No error message is visible. The username field has focus. The submit button is enabled.

#### Loading
The form has been submitted and a response is pending. The submit button is disabled to prevent duplicate submissions. Field values are preserved. No spinner is required, but one may be added as a visual enhancement.

#### Error
The server returned an authentication failure. An inline error message ("Invalid username or password.") is displayed above the form fields. Both fields are cleared. Focus is returned to the username field. The submit button is re-enabled.

#### Empty
N/A — the form is always shown; there is no empty-data concept for this screen. The Default state already represents a freshly loaded form with no user input.

---

### Behaviour Notes

- The form must not reveal whether the username or the password was incorrect (business rule: generic error message only).
- Client-side validation enforces that both fields are non-empty before a network request is made (F-03).
- If the user arrived via a redirect (F-05), the original URL is preserved and used as the post-login destination.
- An already-authenticated user who reaches this route is immediately redirected away (F-06); this screen is never rendered for them.

---

### Logout Entry Point

When a user completes F-04 (Logout), they arrive at this screen in its Default state. No additional logout-confirmation message is shown.
