# LMS 2.0 Story Map and Agent Build Flow

## Overview
This document converts Figma (`Self registration + profile`, `node-id=13544:7500`) into a dependency-first story map that coding agents can implement cleanly.

## Flowchart Files (`.mmd`)
- `docs/flows/01-entry-to-registration.mmd`
- `docs/flows/02-profile-and-verification.mmd`
- `docs/flows/03-completion-home-navigator.mmd`
- `docs/flows/04-full-user-flow-with-endpoints.mmd`

## Recommended Build Order

### Phase 1: Access and Account Creation
1. Intro entry (`Login` vs `Register`)
2. Sign-up methods (SSO + email)
3. Registration form validation and submit states

### Phase 2: Verification and Onboarding Completion
4. Email verification and retry flow
5. Profile details and account security
6. Interest selection and completion actions (`Next` / `Skip for now`)

### Phase 3: Post-Onboarding Experience
7. Success page and home redirection
8. Terms and Conditions auto popup on first sign-in for current policy version
9. Home discovery blocks (search, recommended, events)
10. Ask Navigator popup and interaction loop

## Implementation-Ready User Stories

### Epic A: Access and Registration

#### US-A1 Intro Decision
As a visitor, I want to choose `Login` or `Register` from the intro page so that I can enter the correct journey.

Acceptance criteria:
- Given I open intro, when page renders, then both `Login` and `Register` actions are visible.
- Given I click `Login`, then I am routed to login flow.
- Given I click `Register`, then I am routed to registration flow.

#### US-A2 Sign-Up Method Selection
As a new user, I want multiple sign-up options (Google, LinkedIn, GitHub, Cognizant SSO, Email) so that I can use my preferred identity path.

Acceptance criteria:
- Given registration page, when it loads, then all configured methods are visible.
- Given a provider is clicked, then user is redirected to that provider flow.
- Given provider returns successfully, then user lands on profile creation.

#### US-A3 Registration Form Validation
As a user, I want required-field and password validation so that I can submit valid registration data.

Acceptance criteria:
- Required fields: `First name`, `Last Name`, `Email Address`, `Country`, `Institute`, `Password`, `Confirm password`.
- `Register` stays disabled until all required fields pass validation.
- Inline error messages appear for missing/invalid fields.
- `Password` and `Confirm password` mismatch prevents submit.

### Epic B: Verification and Profile Completion

#### US-B1 Email Verification
As a registered user, I want email verification so that my account is confirmed securely.

Acceptance criteria:
- Successful registration routes to email verification.
- Verification failure supports retry or resend.
- Successful verification routes to profile completion.

#### US-B2 Personal Profile Completion
As a user, I want to complete personal details so that recommendations are relevant.

Acceptance criteria:
- Personal details section pre-fills known values when available.
- User can edit and save: name, email, country, institute.
- Validation errors block progress until corrected.

#### US-B3 Aspirational Role and Interests
As a user, I want to set aspirational role and interests so that my learning path matches career goals.

Acceptance criteria:
- User can add and remove interest chips/cards.
- Suggested interests are available for quick add.
- User can continue using `Next` or defer using `Skip for now`.

### Epic C: Home and Navigator

#### US-C1 Success to Home Transition
As a user, I want confirmation after onboarding so that I know I can start learning.

Acceptance criteria:
- Completion shows success state.
- `Go to home` routes user to home page.

#### US-C2 Mandatory Terms and Conditions Gate
As a signed-in user, I want to see Terms and Conditions automatically on first eligible login so that platform access is policy-compliant.

Acceptance criteria:
- On first sign-in for the active policy version, Terms popup opens automatically.
- If user accepts terms, then home experience is unlocked.
- If user does not accept, then user remains blocked from normal home interactions.
- Terms acceptance is versioned; if policy version changes, popup appears again.

#### US-C3 Home Discovery Experience
As a learner, I want search, recommendations, and events so that I can quickly discover learning opportunities.

Acceptance criteria:
- Home shows welcome/banner and discovery modules.
- Search input is visible and interactive.
- Recommended and event cards are visible.

#### US-C4 Ask Navigator Interaction
As a learner, I want guided prompts and free text chat in Ask Navigator so that I can get upskilling guidance.

Acceptance criteria:
- Popup opens from home and closes without losing page context.
- User can choose prompt library or type free text.
- Assistant responds with guidance and next-step suggestions.

## Story Dependencies (for Coding Agent)
- `US-A1` -> `US-A2` -> `US-A3` -> `US-B1` -> `US-B2` -> `US-B3` -> `US-C1` -> `US-C2` -> `US-C3` -> `US-C4`

## Suggested Ticket Split (Neat Delivery)
- `FE-ONB-01`: intro and sign-up method UI
- `FE-ONB-02`: registration form + validation states
- `FE-ONB-03`: verification and retry UI
- `FE-ONB-04`: profile details + interests UI
- `FE-COMPLIANCE-01`: terms popup gating + policy-version acceptance tracking
- `FE-HOME-01`: success + home landing modules after terms acceptance
- `FE-AI-01`: Ask Navigator popup and conversation container

## Notes for Clean Implementation
- Keep route guards explicit for pre-verification and post-verification states.
- Add compliance guard for terms acceptance before enabling home modules.
- Centralize form schemas and validation messages.
- Separate onboarding state from home-page state.
- Drive UI flow from the three `.mmd` files to avoid drift.
