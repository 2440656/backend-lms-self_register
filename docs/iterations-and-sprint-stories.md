# LMS 2.0 Iterations and Sprint Stories

## Purpose
This plan converts the approved stories (`US-A1` to `US-C4`) and API contract (`docs/api_endpoints.md`) into practical iterations so coding agents can build backend and frontend in a clean sequence.

## Assumptions
- Sprint length: **2 weeks**
- Team stream: **Frontend + Backend + QA** (parallel, story-driven)
- Dependency rule: follow chain from `docs/figma-user-stories-and-mermaid-flows.md`
- Terms compliance gate (`US-C2`) is mandatory before normal home usage

---

## Iteration Roadmap

### Iteration 0 (Sprint 0) - Foundation and Architecture
**Goal**: Prepare project skeleton, environments, auth baseline, CI checks.

**Stories / Tasks**
- `ARCH-01`: Define module boundaries (`auth`, `onboarding`, `compliance`, `home`, `navigator`)
- `ARCH-02`: Add request validation/error middleware using standard error shape
- `ARCH-03`: Add auth token pipeline (issue, refresh, revoke)
- `ARCH-04`: Create DB schemas for minimum models listed in `docs/api_endpoints.md`
- `QA-00`: Seed smoke tests for health and auth bootstrap

**Key API setup**
- `/api/v1/system/bootstrap`
- `/api/v1/auth/providers`
- `/api/v1/auth/token/refresh`
- `/api/v1/auth/logout`

**Exit criteria**
- API project boots and health checks pass
- Shared response/error contracts are enforced
- CI pipeline runs lint + tests + build

---

### Iteration 1 (Sprint 1) - Access, Registration, and Verification
**Goal**: Deliver end-to-end signup + verification.

**Primary stories**
- `US-A1` Intro decision
- `US-A2` Sign-up method selection
- `US-A3` Registration form validation
- `US-B1` Email verification

**Sprint stories (implementation tickets)**
- `BE-AUTH-01`: Implement email registration and duplicate-email guard
- `BE-AUTH-02`: SSO start/callback handlers (Google, LinkedIn, GitHub, Cognizant SSO)
- `BE-VERIFY-01`: Email verification token validation + resend flow
- `FE-ONB-01`: Intro and sign-up methods UI wiring
- `FE-ONB-02`: Registration validations and disabled/enabled register state
- `FE-ONB-03`: Verification page with retry/resend
- `QA-S1-01`: Happy path + invalid field + mismatch password + expired token tests

**API scope**
- `POST /api/v1/auth/register/email`
- `POST /api/v1/auth/login/email`
- `POST /api/v1/auth/sso/:provider/start`
- `GET /api/v1/auth/sso/:provider/callback`
- `POST /api/v1/auth/email/verify`
- `POST /api/v1/auth/email/resend`

**Exit criteria**
- New user can register and verify email
- Failed verification has resend/retry support
- All `US-A*` + `US-B1` acceptance criteria pass

---

### Iteration 2 (Sprint 2) - Multi-Step Profile Setup
**Goal**: Complete profile setup journey and onboarding progression.

**Primary stories**
- `US-B2` Personal profile completion
- `US-B3` Aspirational role and interests

**Sprint stories (implementation tickets)**
- `BE-ONB-01`: Onboarding status API with deterministic step engine
- `BE-PROFILE-01`: Personal detail update and retrieval
- `BE-INTEREST-01`: Suggest/add/remove interests APIs
- `BE-ONB-02`: Onboarding completion (`next`, `skip_for_now`)
- `FE-ONB-04`: Profile details + role + interests multi-step UI
- `QA-S2-01`: Validation blocking + progress-state + skip behavior regression

**API scope**
- `GET /api/v1/onboarding/status`
- `GET /api/v1/users/me`
- `PATCH /api/v1/users/me/personal-details`
- `PATCH /api/v1/users/me/security/password`
- `PUT /api/v1/onboarding/aspirational-role`
- `GET /api/v1/interests/suggestions`
- `GET /api/v1/users/me/interests`
- `POST /api/v1/users/me/interests`
- `DELETE /api/v1/users/me/interests/:interestId`
- `POST /api/v1/onboarding/complete`

**Exit criteria**
- User can complete all profile setup steps
- User can skip optional completion path
- Onboarding status is consistent after refresh/re-login

---

### Iteration 3 (Sprint 3) - Success State + Terms Compliance Gate
**Goal**: Enforce post-login policy acceptance and controlled access.

**Primary stories**
- `US-C1` Success to home transition
- `US-C2` Mandatory terms gate

**Sprint stories (implementation tickets)**
- `BE-COMP-01`: Active terms retrieval and acceptance status
- `BE-COMP-02`: Terms acceptance with policy-version tracking
- `BE-GUARD-01`: Compliance guard middleware for protected home modules
- `FE-COMPLIANCE-01`: Auto terms popup at first sign-in for active policy version
- `FE-HOME-01`: Success page and controlled home redirection
- `QA-S3-01`: Non-accept blocked path + policy version change re-prompt tests

**API scope**
- `GET /api/v1/compliance/terms/active`
- `GET /api/v1/compliance/terms/status`
- `POST /api/v1/compliance/terms/accept`

**Exit criteria**
- Terms popup appears automatically when required
- Non-accepting users cannot access normal home interactions
- Accepting users continue to home modules

---

### Iteration 4 (Sprint 4) - Home Discovery and Ask Navigator
**Goal**: Deliver post-onboarding discovery experience and AI navigator.

**Primary stories**
- `US-C3` Home discovery experience
- `US-C4` Ask Navigator interaction

**Sprint stories (implementation tickets)**
- `BE-HOME-01`: Home aggregate API with banner/widgets
- `BE-HOME-02`: Search, recommendations, upcoming events endpoints
- `BE-AI-01`: Navigator session creation + message exchange
- `BE-AI-02`: Prompt library endpoint + history retrieval
- `FE-HOME-02`: Home cards + search UX + events rendering
- `FE-AI-01`: Ask Navigator popup, prompt picks, free-text conversation
- `QA-S4-01`: Prompt flow + free-text flow + close/reopen context tests

**API scope**
- `GET /api/v1/home`
- `GET /api/v1/search`
- `GET /api/v1/recommendations`
- `GET /api/v1/events/upcoming`
- `GET /api/v1/navigator/prompts`
- `POST /api/v1/navigator/sessions`
- `POST /api/v1/navigator/sessions/:sessionId/messages`
- `GET /api/v1/navigator/sessions/:sessionId/messages`

**Exit criteria**
- Home modules load after compliance + onboarding completion
- Ask Navigator supports prompt and free-text interactions
- Conversation continuity works per session

---

### Iteration 5 (Sprint 5) - Hardening, Analytics, and Release Readiness
**Goal**: Stabilize, secure, and prepare production release.

**Sprint stories (implementation tickets)**
- `SEC-01`: Security review (token abuse, replay, authz boundaries)
- `PERF-01`: Optimize onboarding and home API latency
- `OBS-01`: Add audit events (`verify`, `accept_terms`, `onboarding_complete`)
- `REL-01`: E2E pack for critical user journeys
- `REL-02`: Rollout checklist + runbooks + rollback strategy

**Exit criteria**
- All critical paths pass E2E
- Monitoring, alerts, and audit logs enabled
- Release checklist signed off by stakeholders

---

## Sprint Story Board Template (Reusable)
Use this template inside each sprint planning session:

```md
### Story: <ID> <Title>
- Owner: FE/BE/QA
- Dependency: <previous story IDs>
- API endpoints: <list>
- Acceptance criteria: <from story map>
- Test cases: <happy, validation, failure>
- Definition of done:
  - [ ] Code complete
  - [ ] Unit tests
  - [ ] Integration tests
  - [ ] API contract verified
  - [ ] QA signoff
```

---

## Cross-Sprint Dependency Chain
- Story chain: `US-A1 -> US-A2 -> US-A3 -> US-B1 -> US-B2 -> US-B3 -> US-C1 -> US-C2 -> US-C3 -> US-C4`
- API dependency highlights:
  1. Auth + verification before onboarding completion
  2. Onboarding completion before home routing
  3. Terms acceptance before discovery modules
  4. Discovery modules before AI guidance adoption metrics

---

## Definition of Done (Global)
A sprint story is considered done only when:
1. Acceptance criteria are validated in QA.
2. Endpoint contract matches `docs/api_endpoints.md`.
3. Flow behavior matches `.mmd` diagrams.
4. No blocker defects in auth, terms, or onboarding paths.
5. Logging and error codes are consistent with platform standards.

---

## Suggested Next Agent Tasks
1. Convert this file into sprint tickets in your tracker.
2. Generate OpenAPI 3.1 from `docs/api_endpoints.md`.
3. Scaffold backend modules by iteration (`auth`, `onboarding`, `compliance`, `home`, `navigator`).
