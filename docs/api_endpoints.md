# LMS 2.0 API Endpoints (Figma-Aligned Backend Contract)

## Purpose
This file defines backend endpoints required to implement the onboarding and post-login prototype flow from:
- `docs/figma-user-stories-and-mermaid-flows.md`
- `docs/flows/01-entry-to-registration.mmd`
- `docs/flows/02-profile-and-verification.mmd`
- `docs/flows/03-completion-home-navigator.mmd`
- `docs/flows/04-full-user-flow-with-endpoints.mmd`

Use this as the implementation contract for backend coding agents.

## API Conventions
- **Base URL**: `/api/v1`
- **Auth**: `Authorization: Bearer <access_token>` for protected routes
- **Content type**: `application/json`
- **Time format**: ISO-8601 UTC
- **Idempotency**: support `Idempotency-Key` for critical write operations (`register`, `accept terms`, `complete onboarding`)

## Standard Response Shape
```json
{
  "success": true,
  "data": {},
  "meta": {},
  "error": null
}
```

## Standard Error Shape
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "One or more fields are invalid.",
    "details": [
      { "field": "email", "message": "Invalid email format" }
    ]
  }
}
```

---

## 1) System + Auth Discovery

### `GET /api/v1/auth/providers`
Returns available sign-in methods for UI rendering.

**Response `200`**
```json
{
  "success": true,
  "data": {
    "providers": ["google", "linkedin", "github", "cognizant_sso", "email"]
  }
}
```

### `GET /api/v1/system/bootstrap`
Returns initial app flags needed at login/home bootstrap.

**Response `200`**
```json
{
  "success": true,
  "data": {
    "termsPolicyVersion": "2026-02",
    "askNavigatorEnabled": true,
    "interestSuggestionsEnabled": true
  }
}
```

---

## 2) Authentication + Session

### `POST /api/v1/auth/register/email`
Registers a user via email path.

**Request**
```json
{
  "firstName": "Sanjana",
  "lastName": "Shah",
  "email": "sanjana@example.com",
  "country": "IN",
  "institute": "XYZ Institute of Technology",
  "password": "Strong@123",
  "confirmPassword": "Strong@123"
}
```

**Response `201`**
```json
{
  "success": true,
  "data": {
    "userId": "usr_123",
    "emailVerificationRequired": true
  }
}
```

### `POST /api/v1/auth/login/email`
Email/password login.

**Request**
```json
{
  "email": "sanjana@example.com",
  "password": "Strong@123"
}
```

**Response `200`**
```json
{
  "success": true,
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "expiresIn": 3600,
    "user": {
      "id": "usr_123",
      "emailVerified": true,
      "onboardingCompleted": false
    }
  }
}
```

### `POST /api/v1/auth/sso/:provider/start`
Starts SSO redirect (`provider`: `google|linkedin|github|cognizant_sso`).

### `GET /api/v1/auth/sso/:provider/callback`
Handles SSO callback and issues tokens/session.

### `POST /api/v1/auth/token/refresh`
Refresh access token.

### `POST /api/v1/auth/logout`
Invalidates refresh token/session.

---

## 3) Email Verification

### `POST /api/v1/auth/email/verify`
Verifies email by token/OTP.

**Request**
```json
{
  "verificationToken": "token-from-email"
}
```

**Response `200`**
```json
{
  "success": true,
  "data": {
    "emailVerified": true
  }
}
```

### `POST /api/v1/auth/email/resend`
Resends verification mail.

**Request**
```json
{
  "email": "sanjana@example.com"
}
```

---

## 4) Onboarding + Profile Setup (Multi-Step)

### `GET /api/v1/onboarding/status` (Protected)
Returns progress and next step.

**Response `200`**
```json
{
  "success": true,
  "data": {
    "completed": false,
    "step": "personal_details",
    "progressPercent": 50,
    "requiredPending": ["aspirational_role", "interests"]
  }
}
```

### `GET /api/v1/users/me` (Protected)
Returns current profile for prefill.

### `PATCH /api/v1/users/me/personal-details` (Protected)
Updates personal details.

**Request**
```json
{
  "firstName": "Sanjana",
  "lastName": "Shah",
  "country": "IN",
  "institute": "XYZ Institute of Technology"
}
```

### `PATCH /api/v1/users/me/security/password` (Protected)
Updates password when needed.

### `PUT /api/v1/onboarding/aspirational-role` (Protected)
Sets aspirational role.

**Request**
```json
{
  "role": "UX Designer"
}
```

### `GET /api/v1/interests/suggestions?role=UX%20Designer` (Protected)
Returns suggestions used in chips.

### `GET /api/v1/users/me/interests` (Protected)
Gets current interests.

### `POST /api/v1/users/me/interests` (Protected)
Adds one or more interests.

**Request**
```json
{
  "interests": ["UX Design", "Visual Design", "Interaction Design"]
}
```

### `DELETE /api/v1/users/me/interests/:interestId` (Protected)
Removes an interest.

### `POST /api/v1/onboarding/complete` (Protected)
Marks completion action from UI (`next` or `skip_for_now`).

**Request**
```json
{
  "action": "next"
}
```

**Response `200`**
```json
{
  "success": true,
  "data": {
    "onboardingCompleted": true,
    "redirectTo": "/home"
  }
}
```

---

## 5) Terms & Conditions Compliance Gate (Post Login)

### `GET /api/v1/compliance/terms/active` (Protected)
Returns active terms content + version.

**Response `200`**
```json
{
  "success": true,
  "data": {
    "version": "2026-02",
    "title": "Terms and Conditions",
    "contentHtml": "<p>...</p>",
    "publishedAt": "2026-02-01T00:00:00Z"
  }
}
```

### `GET /api/v1/compliance/terms/status` (Protected)
Checks whether current user accepted active version.

**Response `200`**
```json
{
  "success": true,
  "data": {
    "required": true,
    "accepted": false,
    "activeVersion": "2026-02",
    "acceptedVersion": "2025-11"
  }
}
```

### `POST /api/v1/compliance/terms/accept` (Protected)
Accepts current terms version.

**Request**
```json
{
  "version": "2026-02",
  "acceptedAt": "2026-03-04T10:23:00Z"
}
```

**Response `200`**
```json
{
  "success": true,
  "data": {
    "accepted": true,
    "acceptedVersion": "2026-02"
  }
}
```

---

## 6) Home Discovery (After Terms Accepted)

### `GET /api/v1/home` (Protected)
Returns aggregated home payload for banner, quick links, widgets.

### `GET /api/v1/search?q=<query>` (Protected)
Unified search endpoint for courses/content.

### `GET /api/v1/recommendations` (Protected)
Returns recommended cards based on profile/interests.

### `GET /api/v1/events/upcoming` (Protected)
Returns upcoming events widget data.

---

## 7) Ask Navigator

### `GET /api/v1/navigator/prompts` (Protected)
Returns prompt library options for quick actions.

### `POST /api/v1/navigator/sessions` (Protected)
Creates a chat session.

**Response `201`**
```json
{
  "success": true,
  "data": {
    "sessionId": "nav_sess_123"
  }
}
```

### `POST /api/v1/navigator/sessions/:sessionId/messages` (Protected)
Sends user query and receives assistant response.

**Request**
```json
{
  "message": "Suggest me some up skilling ideas based on my current role"
}
```

**Response `200`**
```json
{
  "success": true,
  "data": {
    "reply": "Based on your role, start with...",
    "suggestedActions": ["Set aspirational role", "Add interests"]
  }
}
```

### `GET /api/v1/navigator/sessions/:sessionId/messages` (Protected)
Returns conversation history.

---

## 8) Story-to-Endpoint Mapping
- **US-A1, US-A2**: `GET /auth/providers`, `POST /auth/sso/:provider/start`, `GET /auth/sso/:provider/callback`
- **US-A3**: `POST /auth/register/email`
- **US-B1**: `POST /auth/email/verify`, `POST /auth/email/resend`
- **US-B2, US-B3**: onboarding/profile endpoints in section 4
- **US-C1**: `POST /onboarding/complete`, `GET /onboarding/status`
- **US-C2**: terms endpoints in section 5
- **US-C3**: `GET /home`, `GET /search`, `GET /recommendations`, `GET /events/upcoming`
- **US-C4**: navigator endpoints in section 7

---

## 9) Minimum Backend Data Models
- `User`
- `UserSecurity`
- `EmailVerificationToken`
- `OnboardingState`
- `UserInterest`
- `TermsPolicy`
- `TermsAcceptance`
- `Recommendation`
- `Event`
- `NavigatorSession`
- `NavigatorMessage`

---

## 10) Critical Guards for Backend Agent
1. Block protected home modules until `emailVerified = true`.
2. Block normal home interactions until active terms are accepted.
3. Keep onboarding progression deterministic via `onboarding/status`.
4. Ensure terms acceptance is policy-version aware.
5. Log security/compliance events (`verify`, `accept_terms`, `profile_complete`).
