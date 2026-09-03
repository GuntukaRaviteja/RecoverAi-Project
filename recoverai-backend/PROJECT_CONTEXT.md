# RecoverAI — Complete Project Context

## 1. What RecoverAI is

RecoverAI is a Buildathon-ready, AI-guided payment-recovery platform. It helps
businesses recover failed payments while protecting the customer experience.

Instead of blindly retrying every failed payment, RecoverAI:

1. Classifies the failure.
2. Uses Gemini AI when available, with deterministic rules as a fallback.
3. Applies a recovery policy that can override unsafe AI recommendations.
4. Starts automatic recovery only when appropriate.
5. Gives the customer control for insufficient-funds failures.
6. Records every important action in an audit trail.

> Current scope: a functional demonstration application. Payment outcomes are
> simulated. Real email and WhatsApp delivery can be enabled with credentials.

---

## 2. Repository layout

```text
IdeaProjects/
├── recoverai-backend/                    Spring Boot REST API
│   ├── src/main/java/com/recoverai/backend/
│   │   ├── config/                       CORS and Gemini configuration
│   │   ├── controller/                   REST endpoints
│   │   ├── dto/                          Request/response data models
│   │   ├── entity/                       JPA database entities
│   │   ├── exception/                    Global error handling
│   │   ├── recovery/                     Policy and state machine
│   │   ├── repository/                   Spring Data repositories
│   │   ├── scheduler/                    Automatic recovery scheduler
│   │   ├── service/                      Business logic and outreach
│   │   └── strategy/                     Gemini/rule-based decisions
│   ├── src/test/                         Policy, lifecycle, service tests
│   ├── README.md                         Backend setup and integrations
│   └── .env.example                     Credential variable template
└── recoverai-frontend/recoverai-frontend/
    ├── src/App.jsx                       Dashboard and customer actions
    ├── src/services/api.js               Central HTTP client
    └── README.md                         Frontend setup and demo guide
```

---

## 3. Technology

| Area | Technology |
| --- | --- |
| Backend | Java 17, Spring Boot, Maven |
| Persistence | Spring Data JPA, MySQL |
| Tests | JUnit 5, Spring Boot Test, H2 |
| AI | Google Gen AI SDK / Gemini, rule-based fallback |
| Frontend | React 19, Vite, Axios |
| Email | Spring Mail with Gmail SMTP support |
| WhatsApp | Twilio Messages REST API |

---

## 4. Data model

### Payment

A payment contains the business payment ID, amount, currency, customer ID,
status, failure reason/category, original payment method/reference, creation
time, and optional recovery contact data:

- `customerEmail`
- `customerWhatsappNumber` in E.164 form, for example `+917993956038`

Only safe/masked payment-method references should be stored.

### RecoveryAttempt

One recovery workflow for a payment. It stores method, status, timestamps,
customer choice, scheduled retry, promise-to-pay deadline, response, and an
optional alternate payment method/reference.

### AiDecision

Stores the AI/rule decision, recovery action, explanation, confidence score,
source, payment ID, and timestamp.

### AuditLog

Stores an action, details, payment ID, and timestamp. It provides the
demonstrable history for recovery and outreach events.

---

## 5. Failure classification and AI decisions

When a payment is analyzed, RecoverAI uses the strategy pattern:

```text
Failed payment
      ↓
Gemini decision strategy
      ↓ (AI unavailable, malformed, or unsuccessful)
Rule-based decision strategy
      ↓
Fallback decision result
      ↓
Recovery policy validation
```

Decision values:

- `RETRY`
- `NOTIFY_CUSTOMER`
- `STOP`

Recovery actions:

- `CREATE_RECOVERY_ATTEMPT`
- `NOTIFY_CUSTOMER`
- `NO_ACTION`

The policy always has the final say. It can prevent attempts for
`SUSPECTED_FRAUD`, `ACCOUNT_CLOSED`, and `INVALID_ACCOUNT`, even if a
caller directly invokes the recovery API.

---

## 6. Recovery policy and lifecycle

### Policy rules

- Maximum attempts: **5** per payment.
- A payment must have status `FAILED` to start recovery.
- Only one active attempt may exist for a payment.
- Insufficient funds is always customer-guided; it never enters blind automatic retry.
- Fraud, closed-account, and invalid-account failures cannot create recovery attempts.
- Customer outreach has a 30-minute cooldown.
- A promise-to-pay pauses outreach until its deadline.

### States

```text
PENDING ───────────────→ PROCESSING ─→ SUCCESS / FAILED
SCHEDULED ─────────────→ PROCESSING
WAITING_FOR_CUSTOMER ──→ SCHEDULED
WAITING_FOR_CUSTOMER ──→ WAITING_FOR_PAYMENT_METHOD ─→ PROCESSING
PROCESSING ────────────→ WAITING_FOR_CUSTOMER
```

`SUCCESS` and `FAILED` are terminal states. A successful recovery changes
the source payment status to `RECOVERED`.

---

## 7. End-to-end flows

### Standard recoverable failure

1. Create a payment with status `FAILED`.
2. Analyze it through the AI endpoint, or create a recovery attempt directly.
3. A standard attempt starts as `PENDING`.
4. The scheduler runs every 30 seconds and executes it.
5. Simulated processing succeeds 70% of the time.
6. Success marks the payment `RECOVERED`; failure may permit a later retry.

### Insufficient funds

1. Failure is categorized as `INSUFFICIENT_FUNDS`.
2. The attempt starts as `WAITING_FOR_CUSTOMER`.
3. RecoverAI queues outreach and records the result in audit logs.
4. The customer can choose:
   - retry later;
   - promise to pay;
   - another payment method.
5. A due scheduled retry is executed by the scheduler.
6. If it fails, the attempt returns to `WAITING_FOR_CUSTOMER` rather than
   repeatedly retrying.

### Alternative payment method

1. Customer selects another method.
2. Attempt moves to `WAITING_FOR_PAYMENT_METHOD`.
3. Customer submits a masked reference for CARD, UPI, or bank account.
4. Simulated processing has an 80% success probability.
5. On failure, the workflow returns to `WAITING_FOR_CUSTOMER`.

---

## 8. Outreach

`OutreachService` always writes audit events. Email and WhatsApp sending are
opt-in and safe to run without credentials:

| Channel | Implementation | Required configuration |
| --- | --- | --- |
| Email | Gmail SMTP through Spring Mail | Google App Password |
| WhatsApp | Twilio Messages API | Account SID, Auth Token, WhatsApp sender |
| Phone | Audit-only placeholder | A real calling provider is not implemented |

Recipients are selected in this order:

1. The payment's `customerEmail` or `customerWhatsappNumber`.
2. Demo fallback values in environment variables.

Possible audit actions include `OUTREACH_EMAIL_QUEUED`,
`OUTREACH_EMAIL_SENT`, `OUTREACH_EMAIL_FAILED`, and
`OUTREACH_EMAIL_NOT_CONFIGURED`; the same pattern applies to WhatsApp.

Secrets belong in `.env.local` or a secret manager, never in source code.
`.env.local` is ignored by Git. See `README.md` and `.env.example`.

---

## 9. REST API

All endpoints use the base URL `http://localhost:8080`.

### Payments

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/payments` | Create a payment |
| GET | `/api/payments` | List payments |
| GET | `/api/payments/{id}` | Get one payment |
| GET | `/api/payments/{id}/details` | Get payment, decisions, attempts, and audit details |

### AI decisions

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/ai-decisions/{paymentId}` | Analyze failed payment and initiate policy-approved action |
| GET | `/api/ai-decisions` | List decisions |
| GET | `/api/ai-decisions/payment/{paymentId}` | Decision history |

### Recovery

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/recovery/{paymentId}` | Create an attempt |
| GET | `/api/recovery/payment/{paymentId}` | Recovery history |
| POST | `/api/recovery/attempts/{id}/retry-later?scheduledRetryAt=...` | Schedule retry |
| POST | `/api/recovery/attempts/{id}/promise-to-pay` | Set promise deadline |
| POST | `/api/recovery/attempts/{id}/notify` | Request customer outreach |
| POST | `/api/recovery/attempts/{id}/choose-payment-method` | Start alternate-method flow |
| POST | `/api/recovery/attempts/{id}/alternative-payment-method` | Submit alternate method |
| POST | `/api/recovery/attempts/{id}/execute` | Execute eligible attempt |
| PUT | `/api/recovery/{id}/status` | Update status only through valid transitions |

`promise-to-pay` request body:

```json
{ "promiseToPayDeadline": "2026-09-01T10:30:00" }
```

`alternative-payment-method` request body:

```json
{
  "paymentMethod": "UPI",
  "paymentMethodReference": "customer@bank"
}
```

### Attempts, audit, and analytics

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/recovery-attempts/payment/{paymentId}?page=0&size=20` | Paginated attempts |
| GET | `/api/audit-logs?page=0&size=20` | Paginated audit log |
| GET | `/api/audit-logs/payment/{paymentId}` | Payment audit log |
| GET | `/api/dashboard/stats` | Dashboard metrics |
| GET | `/api/dashboard/recent-activity` | Recent activity |
| GET | `/api/dashboard/summary` | Summary metrics |
| GET | `/api/dashboard/analytics` | Recovery analytics |
| GET | `/api/dashboard/recovery-trends` | Daily recovery trend |
| GET | `/api/dashboard/failure-reasons` | Failure-category counts |

---

## 10. Frontend

The React application has four views:

- **Recovery Dashboard** — revenue at risk/recovered, rates, funnel, status
  distribution, recent failures, and activity.
- **Recovery Attempts** — paginated attempt history for a selected payment.
- **Payment Details** — payment information, AI insight, customer actions,
  recovery history, and audit log.
- **Analytics** — status, failure, and recovery performance breakdowns.

`src/services/api.js` centralizes frontend HTTP calls and uses
`VITE_API_URL`, defaulting to `http://localhost:8080`.

---

## 11. Local setup

### Backend

Prerequisites: Java 17+, MySQL database named `recoverai`.

```bash
cd recoverai-backend
./mvnw test
./mvnw spring-boot:run
```

Default datasource configuration is in
`src/main/resources/application.properties`. It uses
`spring.jpa.hibernate.ddl-auto=update` for development.

To enable delivery, create a private local environment file:

```bash
cp .env.example .env.local
set -a
source .env.local
set +a
./mvnw spring-boot:run
```

### Frontend

```bash
cd recoverai-frontend/recoverai-frontend
npm install
npm run dev
```

The frontend runs at `http://localhost:5173`. CORS currently allows that
origin.

---

## 12. Testing and verification

Current automated coverage includes:

- Spring application context loading.
- Valid and invalid lifecycle transitions.
- Insufficient-funds policy handling.
- Non-recoverable failure policy handling.
- Duplicate active-attempt blocking.
- Promise-to-pay scheduling.
- Alternate payment-method selection.

Run:

```bash
cd recoverai-backend && ./mvnw test
cd ../recoverai-frontend/recoverai-frontend && npm run lint && npm run build
```

---

## 13. Demo script

1. Start MySQL, backend, and frontend.
2. Create a failed payment, ideally with `Insufficient funds`.
3. Run AI analysis or create recovery.
4. Show the `WAITING_FOR_CUSTOMER` state and outreach audit records.
5. Choose retry later, promise to pay, or alternate payment method.
6. Show the resulting state transition and dashboard activity.
7. Use a standard recoverable failure to show scheduled automatic recovery.
8. Explain Gemini fallback, deterministic policies, and auditability.

---

## 14. Known limitations and next steps

- Payment processing is random simulation, not a real gateway.
- WhatsApp delivery needs Twilio Sandbox/approved sender and may need approved
  templates for production outbound messaging.
- Phone calling is audit-only.
- Authentication, authorization, tenant isolation, encryption of contact data,
  provider webhooks, monitoring, production database migrations, and deployment
  configuration are not yet implemented.
- The frontend is currently concentrated in one large `App.jsx` component and
  should be split into smaller components before long-term development.

Do not describe the current application as a production payment system. It is a
well-structured demonstration platform with explicit simulation boundaries.

---

## 15. Engineering rules

1. Preserve existing API contracts unless frontend usage is checked.
2. Keep controllers thin; put workflow logic in services and policy classes.
3. AI may recommend actions but must not bypass deterministic policy.
4. Keep customer-protection rules and state validation centralized.
5. Audit important actions and state transitions.
6. Do not log passwords, API tokens, unmasked payment details, or contact data.
7. Run backend tests and frontend lint/build after meaningful changes.
