# LMS 2.0 DynamoDB Schema Guide

## Why this document exists
This is a complete DynamoDB blueprint for the LMS 2.0 prototype flow (auth, onboarding, terms, home, navigator).

It is written for:
- People new to DynamoDB
- Backend engineers implementing APIs
- Coding agents generating backend code

Related docs:
- `docs/api_endpoints.md`
- `docs/figma-user-stories-and-mermaid-flows.md`
- `docs/iterations-and-sprint-stories.md`

---

## 1) Quick DynamoDB basics (beginner-friendly)
- A **table** stores items (JSON-like records).
- Every item needs a **Primary Key**:
  - **Partition key (PK)**: required
  - **Sort key (SK)**: optional (we use it where needed)
- A **GSI (Global Secondary Index)** gives additional query paths.
- DynamoDB is best when schema follows **access patterns** (how your APIs read/write data).

---

## 2) Recommended table strategy
For this project, use a **clear multi-table design** (easier for new developers and agents):

1. `lms_users`
2. `lms_auth_sessions`
3. `lms_email_verification_tokens`
4. `lms_onboarding_state`
5. `lms_user_interests`
6. `lms_terms_policies`
7. `lms_terms_acceptances`
8. `lms_recommendations`
9. `lms_events`
10. `lms_navigator_sessions`
11. `lms_navigator_messages`

> You can optimize to fewer tables later. Start with clarity first.

---

## 3) Global table standards (apply to all tables)
- **Billing mode**: `PAY_PER_REQUEST` (on-demand)
- **Encryption**: AWS-managed KMS (default)
- **Point-in-time recovery (PITR)**: enabled
- **Backup**: daily backup policy
- **TTL**: enabled only on token/session/message-expiry tables
- **Naming**: use environment prefix in real deployment (e.g., `dev_lms_users`, `prod_lms_users`)

---

## 4) Table-by-table schema specs

## 4.1 `lms_users`
Stores core user profile and account flags.

**Primary key**
- PK: `userId` (String)

**Attributes**
- `email` (String, lowercase)
- `firstName` (String)
- `lastName` (String)
- `country` (String, ISO code)
- `institute` (String)
- `emailVerified` (Boolean)
- `onboardingCompleted` (Boolean)
- `createdAt` (String, ISO-8601)
- `updatedAt` (String, ISO-8601)

**GSI**
- `GSI_Email`
  - PK: `email`
  - (No SK required)

**Used by APIs**
- `GET /users/me`
- `PATCH /users/me/personal-details`
- login/register lookups by email

**Example item**
```json
{
  "userId": "usr_123",
  "email": "sanjana@example.com",
  "firstName": "Sanjana",
  "lastName": "Shah",
  "country": "IN",
  "institute": "XYZ Institute of Technology",
  "emailVerified": true,
  "onboardingCompleted": false,
  "createdAt": "2026-03-04T10:00:00Z",
  "updatedAt": "2026-03-04T10:05:00Z"
}
```

---

## 4.2 `lms_auth_sessions`
Stores refresh/session state.

**Primary key**
- PK: `sessionId` (String)

**Attributes**
- `userId` (String)
- `refreshTokenHash` (String)
- `deviceInfo` (String)
- `ipAddress` (String)
- `createdAt` (String)
- `expiresAt` (String)
- `ttl` (Number, epoch seconds)
- `revoked` (Boolean)

**GSI**
- `GSI_UserSessions`
  - PK: `userId`
  - SK: `createdAt`

**TTL**
- Enabled on `ttl`

**Used by APIs**
- `POST /auth/token/refresh`
- `POST /auth/logout`

---

## 4.3 `lms_email_verification_tokens`
Stores verification tokens/OTP state.

**Primary key**
- PK: `tokenId` (String)

**Attributes**
- `userId` (String)
- `email` (String)
- `tokenHash` (String)
- `status` (String: `pending|used|expired`)
- `createdAt` (String)
- `expiresAt` (String)
- `ttl` (Number)

**GSI**
- `GSI_UserVerification`
  - PK: `userId`
  - SK: `createdAt`

**TTL**
- Enabled on `ttl`

**Used by APIs**
- `POST /auth/email/verify`
- `POST /auth/email/resend`

---

## 4.4 `lms_onboarding_state`
Tracks deterministic onboarding progression.

**Primary key**
- PK: `userId` (String)

**Attributes**
- `step` (String: `personal_details|aspirational_role|interests|completed`)
- `progressPercent` (Number)
- `requiredPending` (List<String>)
- `lastAction` (String: `next|skip_for_now|update`)
- `updatedAt` (String)

**Used by APIs**
- `GET /onboarding/status`
- `POST /onboarding/complete`

---

## 4.5 `lms_user_interests`
Stores user selected interests.

**Primary key**
- PK: `userId` (String)
- SK: `interestId` (String)  
  (use slug/uuid, e.g., `interest_ux_design`)

**Attributes**
- `interestName` (String)
- `source` (String: `suggested|manual`)
- `createdAt` (String)

**GSI (optional)**
- `GSI_InterestName`
  - PK: `interestName`
  - SK: `createdAt`

**Used by APIs**
- `GET /users/me/interests`
- `POST /users/me/interests`
- `DELETE /users/me/interests/:interestId`

---

## 4.6 `lms_terms_policies`
Stores policy versions and content.

**Primary key**
- PK: `version` (String, e.g., `2026-02`)

**Attributes**
- `title` (String)
- `contentHtml` (String)
- `publishedAt` (String)
- `isActive` (Boolean)

**GSI**
- `GSI_ActivePolicy`
  - PK: `isActive`
  - SK: `publishedAt`

**Used by APIs**
- `GET /compliance/terms/active`

---

## 4.7 `lms_terms_acceptances`
Stores per-user acceptance by policy version.

**Primary key**
- PK: `userId` (String)
- SK: `version` (String)

**Attributes**
- `acceptedAt` (String)
- `ipAddress` (String)
- `userAgent` (String)

**GSI**
- `GSI_VersionUsers` (optional for audit analytics)
  - PK: `version`
  - SK: `acceptedAt`

**Used by APIs**
- `GET /compliance/terms/status`
- `POST /compliance/terms/accept`

---

## 4.8 `lms_recommendations`
Stores home recommendations for users.

**Primary key**
- PK: `userId` (String)
- SK: `recommendationId` (String)

**Attributes**
- `title` (String)
- `type` (String: `course|path|event`)
- `score` (Number)
- `duration` (String)
- `isNew` (Boolean)
- `createdAt` (String)

**Used by APIs**
- `GET /recommendations`
- `GET /home`

---

## 4.9 `lms_events`
Stores upcoming events data.

**Primary key**
- PK: `eventDate` (String, `YYYY-MM-DD`)
- SK: `eventId` (String)

**Attributes**
- `title` (String)
- `location` (String)
- `startTime` (String)
- `status` (String: `upcoming|cancelled|completed`)

**GSI**
- `GSI_StatusDate`
  - PK: `status`
  - SK: `eventDate`

**Used by APIs**
- `GET /events/upcoming`
- `GET /home`

---

## 4.10 `lms_navigator_sessions`
Stores Ask Navigator chat session headers.

**Primary key**
- PK: `sessionId` (String)

**Attributes**
- `userId` (String)
- `createdAt` (String)
- `updatedAt` (String)
- `status` (String: `active|closed`)

**GSI**
- `GSI_UserNavigatorSessions`
  - PK: `userId`
  - SK: `updatedAt`

**Used by APIs**
- `POST /navigator/sessions`
- session listing/history bootstrap

---

## 4.11 `lms_navigator_messages`
Stores Ask Navigator messages.

**Primary key**
- PK: `sessionId` (String)
- SK: `messageTs` (String ISO-8601 with millis)

**Attributes**
- `messageId` (String)
- `role` (String: `user|assistant|system`)
- `message` (String)
- `suggestedActions` (List<String>, optional)
- `ttl` (Number, optional if retention policy needed)

**TTL**
- Optional based on retention policy

**Used by APIs**
- `POST /navigator/sessions/:sessionId/messages`
- `GET /navigator/sessions/:sessionId/messages`

---

## 5) Model-to-table mapping
- `User` -> `lms_users`
- `UserSecurity` + session state -> `lms_auth_sessions`
- `EmailVerificationToken` -> `lms_email_verification_tokens`
- `OnboardingState` -> `lms_onboarding_state`
- `UserInterest` -> `lms_user_interests`
- `TermsPolicy` -> `lms_terms_policies`
- `TermsAcceptance` -> `lms_terms_acceptances`
- `Recommendation` -> `lms_recommendations`
- `Event` -> `lms_events`
- `NavigatorSession` -> `lms_navigator_sessions`
- `NavigatorMessage` -> `lms_navigator_messages`

---

## 6) API -> table query map (how backend should read/write)

- `POST /auth/register/email`
  - Write: `lms_users`, `lms_onboarding_state`
  - Write verification token: `lms_email_verification_tokens`

- `POST /auth/login/email`
  - Read user by `GSI_Email` on `lms_users`
  - Write session: `lms_auth_sessions`

- `POST /auth/email/verify`
  - Read+update token: `lms_email_verification_tokens`
  - Update user: `lms_users.emailVerified = true`

- `GET /onboarding/status`
  - Read: `lms_onboarding_state`

- `PATCH /users/me/personal-details`
  - Update: `lms_users`

- `POST /users/me/interests`
  - Batch write: `lms_user_interests`

- `GET /compliance/terms/active`
  - Query `GSI_ActivePolicy` in `lms_terms_policies`

- `GET /compliance/terms/status`
  - Read active version + check acceptance in `lms_terms_acceptances`

- `POST /compliance/terms/accept`
  - Write: `lms_terms_acceptances`

- `GET /home`
  - Read from: `lms_users`, `lms_recommendations`, `lms_events`

- `POST /navigator/sessions/:sessionId/messages`
  - Write to `lms_navigator_messages`
  - Update `lms_navigator_sessions.updatedAt`

---

## 7) Security, compliance, and operations checklist
- Store only hashed tokens (`refreshTokenHash`, `tokenHash`), never plaintext tokens.
- Restrict table access by least-privilege IAM role per service.
- Enable CloudWatch alarms for throttling and error spikes.
- Enable Streams where audit/event fan-out is needed (optional start: `terms_acceptances`, `onboarding_state`).
- Keep GDPR/retention policy for navigator messages (TTL if required).

---

## 8) Capacity and scaling guidance
- Start with on-demand (`PAY_PER_REQUEST`) during development and early launch.
- Add GSIs only for real query needs; each GSI increases write cost.
- Watch hot partitions:
  - `lms_events` by date can become hot on single day -> shard if required.
  - `lms_terms_policies` is tiny and safe.

---

## 9) Suggested table creation order
1. `lms_users`
2. `lms_onboarding_state`
3. `lms_email_verification_tokens`
4. `lms_auth_sessions`
5. `lms_terms_policies`
6. `lms_terms_acceptances`
7. `lms_user_interests`
8. `lms_recommendations`
9. `lms_events`
10. `lms_navigator_sessions`
11. `lms_navigator_messages`

---

## 10) Definition of done for DB setup
Database setup is complete when:
- All tables above are created with keys and GSIs.
- TTL enabled on token/session tables.
- PITR and backups are enabled.
- At least one seed policy row exists in `lms_terms_policies` with `isActive = true`.
- Backend can run full flow: register -> verify -> onboarding -> terms acceptance -> home -> navigator.
