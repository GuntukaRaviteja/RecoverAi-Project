# RecoverAI Backend

RecoverAI is a Spring Boot demo backend for an AI-guided payment recovery workflow.
It classifies failed payments, creates safe recovery attempts, respects policy
restrictions, records an audit trail, and can optionally notify the customer via
email or WhatsApp.

This repository contains the backend API and the Spring-based business logic that
powers the RecoverAI dashboard.

## What the backend does

- Stores and exposes payment records and recovery attempts
- Runs a decision strategy for failed payments
- Uses a rule-based fallback when Gemini/LLM access is not available
- Enforces a recovery policy for safe and customer-friendly retries
- Tracks audit logs for all recovery and outreach actions
- Supports demo outreach via SMTP and Twilio without sending real production
  traffic by default

## Tech stack

- Java 17
- Spring Boot 3.x
- Spring Data JPA
- MySQL (local/dev database)
- H2 for test execution
- Maven
- Google Gen AI / Gemini SDK (optional)
- Spring Mail for email delivery
- Twilio SDK for WhatsApp delivery

## Project structure

```text
recoverai-backend/
├── src/main/java/com/recoverai/backend/
│   ├── config/             CORS, Gemini client config, demo seeding
│   ├── controller/         REST controllers for payments, dashboard, AI, recovery
│   ├── dto/                Request/response payloads for API and analytics
│   ├── entity/             JPA entities: Payment, RecoveryAttempt, AiDecision, AuditLog
│   ├── exception/          Global exception handling
│   ├── recovery/           Policy and status enums
│   ├── repository/         Spring Data repositories
│   ├── scheduler/          Job that executes due recovery actions
│   ├── service/            Business logic for payments, outreach, dashboard, AI
│   ├── strategy/           Decision strategy pattern for AI and rules
│   └── RecoveraiBackendApplication.java
├── src/test/               JUnit and Spring Boot tests
├── README.md               Backend setup and usage guide
├── .env.example            Template for secret environment variables
├── mvnw                    Maven wrapper
├── pom.xml                 Maven configuration
└── target/                 Build artifacts
```

## Prerequisites

- Java 17 or newer
- MySQL running locally with a database named `recoverai`
- Optional: `GEMINI_API_KEY` or `GOOGLE_API_KEY` for Gemini-backed decisions
  - If missing, the backend automatically falls back to the rule-based strategy

## Local setup

1. Create or update your datasource configuration in
   `src/main/resources/application.properties`.
2. Start the app:

```bash
./mvnw spring-boot:run
```

3. The API listens on:

```text
http://localhost:8080
```

The recovery scheduler checks for pending and due retries every 30 seconds.

## Configuration and secrets

The app is designed for a demo environment, not production payment processing.
Keep credentials out of source control and prefer environment variables or a
secret manager.

### Demo environment variables

```bash
export RECOVERAI_DEMO_EMAIL='your-recipient@example.com'
export RECOVERAI_DEMO_WHATSAPP_NUMBER='+<country-code><number>'

# Gmail SMTP
export RECOVERAI_EMAIL_ENABLED=true
export RECOVERAI_EMAIL_FROM='your-sending-address@gmail.com'
export SMTP_USERNAME='your-sending-address@gmail.com'
export SMTP_APP_PASSWORD='your-16-digit-google-app-password'

# Twilio WhatsApp
export RECOVERAI_WHATSAPP_ENABLED=true
export TWILIO_ACCOUNT_SID='AC...'
export TWILIO_AUTH_TOKEN='...'
export TWILIO_WHATSAPP_FROM='+14155238886'
```

For Gmail, use an app password generated after enabling 2-Step Verification.
See the official docs:
https://support.google.com/mail/answer/185833?hl=en

When a payment includes `customerEmail` or `customerWhatsappNumber` in E.164
format, those values are used before the demo fallback values.

## Demo boundaries

- Payment processing is simulated; success/failure outcomes are intentionally
  randomized.
- Outreach is audit-log based; the app does not call a real email, WhatsApp, or
  phone provider by default.
- Do not use this implementation to process real payments or store masked or
  unmasked payment credentials in production.

## Core domain model

### Payment

Represents a failed or recovered payment record and stores:

- payment identifier
- amount and currency
- customer information
- status
- failure category / reason
- payment method reference
- optional recovery contact fields

### RecoveryAttempt

Represents a single recovery workflow for a payment.
It tracks:

- recovery method
- state/status
- scheduled retry times
- customer action state
- promise-to-pay details
- alternate payment method data

### AiDecision

Stores the AI or rule-based decision produced for a payment, including:

- decision type (`RETRY`, `NOTIFY_CUSTOMER`, `STOP`)
- recovery action
- explanation
- confidence score
- source (`LLM`, `RULE`, fallback, etc.)

### AuditLog

Provides a timestamped audit trail for operational events such as:

- recovery attempts created
- outreach queued/sent/failed
- policy checks executed
- customer actions

## Decision flow

Failed payments follow this lifecycle:

```text
Failed payment
  ↓
AI decision strategy
  ↓ (unavailable or unsuccessful)
Rule-based decision strategy
  ↓
Fallback result
  ↓
Recovery policy validation
  ↓
Recovery attempt or safe no-action outcome
```

### Decision types

- `RETRY`
- `NOTIFY_CUSTOMER`
- `STOP`

### Recovery actions

- `CREATE_RECOVERY_ATTEMPT`
- `NOTIFY_CUSTOMER`
- `NO_ACTION`

## Recovery policy

The backend enforces a strict recovery policy to keep the demo safe:

- Maximum recovery attempts: 5 per payment
- Recovery only begins for payments in `FAILED` state
- Only one active recovery attempt is allowed at a time
- Insufficient funds is customer-guided and never auto-retried blindly
- Fraud, account closed, and invalid account failures cannot create automatic
  recovery attempts
- Customer outreach has a 30-minute cooldown
- Promise-to-pay pauses outbound outreach until the agreed deadline

### Attempt states

```text
PENDING ───────────────→ PROCESSING ─→ SUCCESS / FAILED
SCHEDULED ─────────────→ PROCESSING
WAITING_FOR_CUSTOMER ──→ SCHEDULED
WAITING_FOR_CUSTOMER ──→ WAITING_FOR_PAYMENT_METHOD ─→ PROCESSING
PROCESSING ────────────→ WAITING_FOR_CUSTOMER
```

`SUCCESS` and `FAILED` are terminal states. A successful recovery marks the
original payment as `RECOVERED`.

## Outreach behavior

`OutreachService` writes audit entries for every outreach attempt. Email and
WhatsApp delivery are opt-in and safe to run without credentials.

| Channel | Implementation | Required configuration |
| --- | --- | --- |
| Email | Gmail SMTP through Spring Mail | Google App Password |
| WhatsApp | Twilio Messages API | Account SID, Auth Token, WhatsApp sender |
| Phone | Audit-only placeholder | Not implemented |

Possible audit actions include:

- `OUTREACH_EMAIL_QUEUED`
- `OUTREACH_EMAIL_SENT`
- `OUTREACH_EMAIL_FAILED`
- `OUTREACH_EMAIL_NOT_CONFIGURED`

The same pattern applies to WhatsApp outreach.

## API overview

All endpoints are served from `http://localhost:8080`.

### Payments

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/payments` | List all payments |
| POST | `/api/payments` | Create a payment |
| GET | `/api/payments/{id}` | Fetch a payment by ID |
| GET | `/api/payments/{id}/details` | Fetch detailed payment and recovery context |

### AI decisions

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/ai-decisions/{paymentId}` | Analyze a payment using the decision strategy |
| GET | `/api/ai-decisions/all` | List all AI decisions |
| GET | `/api/ai-decisions/payment/{paymentId}` | Get decision history for a payment |

### Recovery attempts

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/recovery/{paymentId}` | Create a recovery attempt |
| GET | `/api/recovery/payment/{paymentId}` | Get recovery attempts for a payment |
| POST | `/api/recovery/attempts/{recoveryAttemptId}/schedule` | Schedule a retry |
| POST | `/api/recovery/attempts/{recoveryAttemptId}/retry-later` | Alias for scheduling a retry |
| POST | `/api/recovery/attempts/{recoveryAttemptId}/promise-to-pay` | Record a promise-to-pay |
| POST | `/api/recovery/attempts/{recoveryAttemptId}/notify` | Queue customer outreach |
| POST | `/api/recovery/attempts/{recoveryAttemptId}/choose-payment-method` | Move a case to alternative-payment flow |
| POST | `/api/recovery/attempts/{recoveryAttemptId}/alternative-payment-method` | Submit masked alternate payment method |
| POST | `/api/recovery/attempts/{recoveryAttemptId}/execute` | Execute a recovery attempt |
| PUT | `/api/recovery/{recoveryAttemptId}/status` | Update status manually |

### Dashboard and analytics

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/dashboard/stats` | Summary KPIs |
| GET | `/api/dashboard/analytics` | Recovery analytics |
| GET | `/api/dashboard/recovery-trends` | Daily trend data |
| GET | `/api/dashboard/recent-activity` | Recent event stream |
| GET | `/api/dashboard/summary` | Compact dashboard summary |
| GET | `/api/dashboard/failure-reasons` | Failure distribution analytics |
| GET | `/api/dashboard/recovery-comparison` | Simulated RecoverAI vs blind retry comparison |

### Recovery attempt listing for the UI

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/recovery-attempts` | List recovery operations for dashboard table views |
| GET | `/api/recovery-attempts/payment/{paymentId}` | Paginated recovery history by payment |

Call the AI decision endpoint before starting a recovery when a decision record
is required. Recovery endpoints called directly can create an attempt without an
AI decision record.

## Tests

```bash
./mvnw test
```

The test suite uses H2 in memory and validates policy enforcement, lifecycle
transitions, and customer-guided recovery flows.

## Frontend integration

The React/Vite dashboard lives in:

```text
../recoverai-frontend/recoverai-frontend
```

Run it with:

```bash
cd ../recoverai-frontend/recoverai-frontend
npm install
npm run dev
```

Typical frontend URL:

```text
http://localhost:5173
```

If that port is busy, Vite may choose another port such as 5174 or 5175. The
backend CORS configuration allows common local ports. Set `VITE_API_URL` if the
backend is not running on `http://localhost:8080`.

## Dashboard views

- Dashboard: revenue at risk, recovered revenue, recovery rate, guardrails, and
  recent activity
- Recovery Attempts: payment search, status tracking, failed-recovery filtering,
  and customer action visibility
- Payment Details: AI analysis, policy notices, recovery history, and audit log
- Analytics: outcome summaries, open recoveries, funnel metrics, and comparison
  against a blind retry baseline

## Analytics definitions

Recovery rate is calculated from completed recovery attempts:

```text
successful attempts / (successful attempts + failed attempts)
```

Open recovery attempts include all in-flight states:

- `PENDING`
- `WAITING_FOR_CUSTOMER`
- `WAITING_FOR_PAYMENT_METHOD`
- `SCHEDULED`
- `PROCESSING`

Promise-to-pay cases remain open while they are waiting, scheduled, or
processing, and they stop being open after `SUCCESS` or `FAILED`.

The RecoverAI vs Blind Retry comparison is a transparent simulation used for
product demo purposes; it is not a real Razorpay benchmark.
