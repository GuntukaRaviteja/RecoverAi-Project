# RecoverAI - Total Project Review

## 1. Executive summary

RecoverAI is an AI-assisted payment-recovery platform for merchants. It
detects failed payments, classifies the failure, recommends a recovery action,
applies deterministic customer-protection rules, executes a bounded recovery
workflow, and records an audit trail.

The project is a strong match for Razorpay Buildathon **Track 03 - AI Revenue
Recovery**. It covers the complete conceptual loop required by the track:

```text
Revenue at risk -> diagnosis -> intervention -> bounded execution -> outcome
```

The current repository is a functional buildathon demonstration rather than a
production payment-processing system. Payment outcomes are simulated. Razorpay
Test Mode webhook ingestion is implemented, but the recovery execution itself
does not charge a real payment method.

### One-sentence pitch

> RecoverAI helps merchants recover failed payment revenue without blindly
> retrying every customer, using explainable AI recommendations, deterministic
> safety guardrails, customer-guided recovery, and a complete audit trail.

## 2. Problem and target users

### Problem

Payment failure is not one problem. A temporary network failure, insufficient
funds, expired card, suspected fraud, and an invalid account require different
responses. Blindly retrying all failures can:

- waste payment attempts;
- frustrate customers;
- increase support volume;
- retry unsafe or non-recoverable cases;
- make it difficult to explain why money was lost or recovered.

### Target users

- Subscription and SaaS merchants
- E-commerce businesses
- Marketplaces
- Merchant operations teams
- Revenue-recovery and finance-operations teams

### Value proposition

RecoverAI turns a failed payment into a controlled case. The merchant gets
recovery automation and measurable revenue outcomes; the customer gets a
choice when human action is required; and the operations team gets visibility
into every decision and state transition.

## 3. End-to-end workflow

### Standard recoverable failure

1. A failed payment is stored or received from the Razorpay webhook.
2. Payments created through the normal payment service are categorized from
   their failure reason. The current webhook service stores the Razorpay
   failure reason but does not yet explicitly assign a category before
   triggering analysis.
3. Gemini can recommend `RETRY`, `NOTIFY_CUSTOMER`, or `STOP`.
4. If Gemini is unavailable or returns an invalid result, the rule-based
   strategy is used.
5. `RecoveryPolicy` checks the recommendation.
6. A permitted payment creates a recovery attempt.
7. The scheduler executes pending or due scheduled attempts.
8. The simulated processor returns success or failure.
9. A success marks the payment `RECOVERED`.
10. Audit entries record the important actions.

### Insufficient-funds flow

Insufficient funds is intentionally customer-guided:

1. The payment enters `WAITING_FOR_CUSTOMER`.
2. RecoverAI queues outreach.
3. The customer can select retry later, promise to pay, or another payment
   method.
4. A promise-to-pay creates a future deadline and pauses reminders.
5. A scheduled retry executes after the deadline.
6. If the retry fails, the case returns to customer action instead of creating
   an uncontrolled retry loop.

### Unsafe or non-recoverable flow

Suspected fraud, closed accounts, and invalid accounts are stopped by policy.
The AI recommendation cannot bypass this policy. The system records a stop
decision and audit information rather than repeatedly attempting recovery.

## 4. Frontend review

### Technology and structure

- React 19
- Vite
- Axios
- CSS-based dashboard styling
- One primary `App.jsx` application component
- Centralized HTTP calls in `src/services/api.js`

The frontend is intentionally demo-oriented and presents an operations
dashboard rather than a customer-facing checkout.

### Dashboard view

The dashboard presents:

- revenue at risk;
- recovered revenue;
- failed and recovered payment counts;
- recovery success rate;
- active recovery cases;
- status distribution;
- recovery funnel;
- guardrail counts;
- autonomous activity;
- daily recovery trends;
- recent failed payments;
- recent activity.

This is the strongest screen for the buildathon because it connects workflow
execution to merchant value.

### Recovery Attempts view

The recovery operations screen supports:

- selecting and searching payments;
- viewing recovery attempts;
- filtering by category, method, status, attempt count, and customer action;
- viewing active and completed cases;
- inspecting attempt state and response.

This screen demonstrates that the product is an operations tool rather than
only an AI classification page.

### Payment Details view

The payment details screen combines:

- payment metadata;
- failure explanation;
- AI recovery insight;
- policy notices;
- customer action controls;
- recovery history;
- audit log.

This is the best screen for showing explainability and customer-protection
behavior in the demo video.

### Analytics view

The analytics screen includes:

- payments by status;
- recovered and failed amounts;
- recovery attempt results;
- failure-category breakdown;
- failure impact analysis;
- RecoverAI versus blind-retry comparison;
- benchmark funnel;
- safety outcomes.

The comparison UI is useful for the buildathon, but the benchmark should be
described honestly as a demo/cohort comparison unless the inputs are generated
from a reproducible experiment.

### API client and state management

`src/services/api.js` centralizes Axios calls and uses `VITE_API_URL`, defaulting
to `http://localhost:8080`. The application loads payments, operations,
dashboard data, trends, failure reasons, and comparison data on startup.

The UI uses local React state and `Promise.allSettled` for dashboard extras,
which allows some panels to render even if one endpoint fails. Abort signals
are used when the page unmounts.

### Frontend strengths

- Clear separation between merchant dashboard and payment details.
- Good visibility into policy and audit information.
- Customer-action flows are visible rather than hidden in backend logic.
- API URL is configurable.
- Production build and lint pass.
- The UI directly supports a compelling five-minute demo.

### Frontend gaps

- `App.jsx` is very large and should eventually be split into components.
- There is no authentication or role-based access control.
- There is no production error-reporting or observability integration.
- The default error text still references backend port 8080, while the Docker
  demo exposes the backend at host port 8081.
- The frontend has no dedicated customer portal or payment checkout.
- Accessibility, responsive behavior, and browser compatibility need a formal
  review before production use.

## 5. Backend review

### Technology

- Java 17
- Spring Boot 4.1.1
- Spring MVC
- Spring Data JPA
- MySQL for Docker/local runtime
- H2 for tests
- Maven
- Google GenAI/Gemini SDK
- Spring Mail
- Twilio REST integration

### Package structure

| Package | Responsibility |
| --- | --- |
| `controller` | REST API surface |
| `service` | Workflow and business logic |
| `repository` | Spring Data persistence |
| `entity` | JPA data model |
| `dto` | API response/request shapes |
| `recovery` | Policy and state machine |
| `strategy` | AI, fallback, and decision types |
| `scheduler` | Automatic due-work processing |
| `config` | CORS, Gemini, and demo data |
| `exception` | REST error mapping |

Controllers are relatively thin and delegate workflow decisions to services.

### Main entities

#### Payment

Stores:

- internal database ID;
- payment identifier;
- amount and currency;
- customer ID and optional contact details;
- status;
- failure reason and category;
- payment method;
- safe payment-method reference;
- creation time.

#### RecoveryAttempt

Stores:

- payment ID;
- recovery method;
- lifecycle status;
- timestamps;
- response and failure reason;
- customer action;
- scheduled retry time;
- promise-to-pay deadline;
- customer notification time;
- alternative payment method and masked reference.

#### AiDecision

Stores:

- payment ID;
- decision;
- recovery action;
- explanation;
- confidence score;
- decision source;
- creation time.

#### AuditLog

Stores:

- payment ID;
- action name;
- details;
- timestamp.

The audit model is simple and effective for a buildathon demo. It is not yet a
tamper-evident or append-only production audit system.

### REST API surface

#### Payments

- `GET /api/payments`
- `POST /api/payments`
- `GET /api/payments/{id}`
- `GET /api/payments/{id}/details`

#### AI decisions

- `POST /api/ai-decisions/{paymentId}`
- `GET /api/ai-decisions/all`
- `GET /api/ai-decisions/payment/{paymentId}`

#### Recovery

- `POST /api/recovery/{paymentId}`
- `GET /api/recovery/payment/{paymentId}`
- `POST /api/recovery/attempts/{id}/schedule`
- `POST /api/recovery/attempts/{id}/retry-later`
- `POST /api/recovery/attempts/{id}/promise-to-pay`
- `POST /api/recovery/attempts/{id}/notify`
- `POST /api/recovery/attempts/{id}/choose-payment-method`
- `POST /api/recovery/attempts/{id}/alternative-payment-method`
- `POST /api/recovery/attempts/{id}/execute`
- `PUT /api/recovery/{id}/status`

#### Operations and analytics

- `GET /api/recovery-attempts`
- `GET /api/recovery-attempts/payment/{paymentId}`
- `GET /api/audit-logs`
- `GET /api/audit-logs/payment/{paymentId}`
- `GET /api/dashboard/stats`
- `GET /api/dashboard/summary`
- `GET /api/dashboard/analytics`
- `GET /api/dashboard/recent-activity`
- `GET /api/dashboard/recovery-trends`
- `GET /api/dashboard/failure-reasons`
- `GET /api/dashboard/recovery-comparison`

#### Razorpay

- `POST /api/webhooks/razorpay`

## 6. Recovery policy and state machine

`RecoveryPolicy` is the core safety layer. It defines:

- maximum of five recovery attempts per payment;
- recovery only for payments in `FAILED` state;
- one active attempt at a time;
- customer-guided handling for insufficient funds;
- policy blocking for suspected fraud, closed accounts, and invalid accounts.

The main states are:

```text
PENDING -> PROCESSING -> SUCCESS / FAILED
SCHEDULED -> PROCESSING
WAITING_FOR_CUSTOMER -> SCHEDULED
WAITING_FOR_CUSTOMER -> WAITING_FOR_PAYMENT_METHOD
WAITING_FOR_PAYMENT_METHOD -> PROCESSING
PROCESSING -> WAITING_FOR_CUSTOMER
```

The service validates transitions before applying them and audits state
changes. This directly addresses the Buildathon requirement for bounded,
compliant recovery.

### Scheduler

`RecoveryScheduler` runs every 30 seconds and:

1. executes pending attempts;
2. executes scheduled attempts whose time has arrived;
3. creates permitted follow-up attempts for failed payments;
4. respects active-attempt and maximum-attempt rules.

For production, this should use a distributed job/lock strategy so multiple
backend instances cannot process the same attempt concurrently.

## 7. AI decisioning

### Gemini strategy

`LlmDecisionStrategy` sends the failure category and reason to Gemini and
requests structured JSON containing:

- decision;
- recovery action;
- confidence;
- explanation.

The core strategy contract allows these decisions:

- `RETRY`
- `NOTIFY_CUSTOMER`
- `STOP`

The core strategy contract allows these actions:

- `CREATE_RECOVERY_ATTEMPT`
- `NOTIFY_CUSTOMER`
- `NO_ACTION`

The response is schema-constrained and the confidence score is checked to be
between zero and one.

### Fallback behavior

If no Gemini key is configured, the rule-based strategy is used directly. If
Gemini is configured but fails or returns malformed data, the fallback wrapper
logs the failure and uses deterministic rules.

This is a strong reliability decision for a demo: the product remains usable
without external AI availability.

### Policy precedence

AI recommends; `RecoveryPolicy` decides whether the recommendation is allowed.
For example, an unsafe category is converted to `STOP` and `NO_ACTION`.
Customer-guided insufficient-funds handling is also enforced centrally.

### AI limitations

- The fallback strategy is category-based rather than learned from historical
  merchant outcomes.
- There is no offline evaluation set, precision/recall report, or calibration
  analysis.
- The Gemini model call is synchronous.
- There is no prompt/version registry or decision replay mechanism.
- AI confidence is accepted as a model value and is not calibrated against
  observed success.
- The seeded presentation dataset contains additional outcome-oriented labels
  in some demo decision records; these should not be confused with the core
  strategy contract values.

For the Buildathon, present AI as a meaningful diagnosis/recommendation layer
with deterministic safety controls, not as a fully autonomous production
financial agent.

## 8. Razorpay and external integrations

### Razorpay webhook ingestion

The webhook service can be connected to Razorpay Test Mode or another
Razorpay webhook source. It:

1. receives the raw request body;
2. verifies `X-Razorpay-Signature` using HMAC-SHA256 and the configured secret;
3. parses the payment entity;
4. ingests failed, authorized, and captured events;
5. stores amount in rupees and only a masked card reference;
6. writes an audit event;
7. triggers AI analysis for a newly received failed payment.

The webhook currently does not map Razorpay error codes/descriptions to the
normal application failure taxonomy. This is a high-priority fix because an
unclassified webhook payment can fall through to the rule-based `STOP`
decision.

Unsigned or invalid requests are rejected. The endpoint is configured through
`RAZORPAY_WEBHOOK_SECRET`.

### Webhook production gaps

- There is no dedicated webhook-event table or durable event ID for strong
  idempotency.
- The endpoint should be tested with official Razorpay test payloads and
  signature vectors.
- Production webhook processing should be acknowledged quickly and delegated
  to a durable queue.
- Event-to-payment mapping and customer identity validation need stronger
  rules.

### Email and WhatsApp

Email uses Spring Mail/Gmail SMTP. WhatsApp uses Twilio. Both are opt-in and
fall back to audit-only behavior when credentials are missing. This is safe for
the demo and avoids accidental customer messaging.

Phone outreach is currently audit-only. WhatsApp delivery can send through
Twilio when explicitly enabled and configured.

### Payment execution boundary

Recovery execution uses `Math.random()` to simulate success probabilities.
The project must not claim that it charges or recovers real money. The
Razorpay integration currently proves verified event ingestion; it does not
replace a real payment retry/checkout flow.

## 9. Demo data and database behavior

The demo seeder creates a broad batch of synthetic failed payments across
categories such as insufficient funds, expired cards, invalid cards, network
technical failures, suspected fraud, timeouts, limits, issuer unavailability,
and card declines.

The seeder now behaves as follows:

- `RECOVERAI_DEMO_SEED=true` enables seeding;
- data is seeded only when the database is empty;
- `RECOVERAI_DEMO_RESET=true` intentionally clears and recreates demo data;
- the Docker MySQL database persists in the `recoverai-mysql` volume.

This prevents normal application restarts from deleting the demo state. Running
`docker compose down -v` deliberately removes the database volume.

The seeder produces repeatable presentation data, but seeded outcomes are not
the same as an independent benchmark experiment. That distinction should be
made explicit in the application and video.

## 10. Analytics and measurement quality

The dashboard calculates:

- payment and recovery counts;
- successful and failed recovery attempts;
- recovery success rate;
- revenue at risk;
- recovered revenue;
- active/open recovery cases;
- failure-category counts;
- RecoverAI versus blind-retry comparison;
- customer-action and policy-stopped cases.

The comparison endpoint uses the same failed/recovered cohort and compares
RecoverAI outcomes with a first-attempt baseline. The UI displays recovery
rate, recovered revenue, attempt counts, unnecessary retries, customer-action
cases, and policy-stopped cases.

### What is good

- The metric is merchant-facing and money-oriented.
- The cohort and funnel are visible.
- Safety outcomes are presented alongside recovery outcomes.
- The benchmark is transparent in the code and README.

### What must be improved for a strong submission

Show a reproducible batch experiment with:

- fixed input cohort;
- baseline definition;
- RecoverAI policy version;
- total INR at risk;
- INR recovered;
- recovery rate;
- incremental revenue;
- attempts per recovered payment;
- customer-contact rate;
- policy-stopped cases;
- maximum-attempt cases;
- exceptions and failures.

Do not present seeded deterministic outcomes as a measured Razorpay benchmark.

## 11. Docker and local operation

The repository includes:

- root `docker-compose.yml`;
- backend Dockerfile;
- frontend Dockerfile;
- MySQL 8.4 service.

### Verified Docker ports

| Service | Host address |
| --- | --- |
| Frontend | `http://localhost:5173` |
| Backend | `http://localhost:8081` |
| MySQL | `localhost:3307` |

Inside the Compose network, the backend connects to MySQL on `mysql:3306`.

### Commands

```bash
docker compose up --build -d
docker compose ps
docker compose logs --tail=100 backend
docker compose down
```

The stack was built successfully and the frontend and backend endpoints
responded. Demo data was verified through `/api/dashboard/stats`, and data was
verified to survive a backend restart.

## 12. Testing and validation

### Automated checks completed

- Backend Maven test suite: passing.
- Frontend ESLint: passing.
- Frontend Vite production build: passing.
- Git diff check: passing.
- Docker image builds: passing.
- Docker Compose services: running.
- Frontend HTTP response: verified.
- Backend dashboard API response: verified.

### Test coverage present

- application context loading;
- recovery policy rules;
- recovery status transitions;
- insufficient-funds flow;
- non-recoverable failure blocking;
- duplicate active-attempt blocking;
- promise-to-pay behavior;
- alternative payment-method flow.

### Test coverage still needed

- Razorpay signature verification tests;
- webhook payload parsing tests;
- webhook replay/idempotency tests;
- controller/API contract tests;
- scheduler concurrency tests;
- authorization tests;
- frontend component and end-to-end tests;
- deterministic recovery simulation tests.

## 13. Security and privacy review

### Positive controls

- Secrets are represented through environment variables.
- `.env.example` contains placeholders rather than real credentials.
- Invalid Razorpay webhook signatures are rejected.
- Card references are masked.
- Email/WhatsApp delivery is disabled by default.
- Audit logs avoid intentionally storing raw payment credentials.
- CORS is limited to known local development origins.

### Important gaps

- No authentication or authorization.
- No tenant isolation.
- No rate limiting on public APIs/webhooks.
- No replay/idempotency store for webhook events.
- Contact data is stored as ordinary database columns without documented
  encryption or retention policy.
- JPA schema uses `ddl-auto=update`, which is unsuitable for controlled
  production migrations.
- No structured security event monitoring.
- No production secret manager configuration.
- No verified gateway transaction reconciliation.
- User-provided alternative payment references need stricter validation and
  redaction rules.

The application is appropriate for synthetic/demo data only until these gaps
are addressed.

## 14. Reliability and production readiness

### Current maturity

**Buildathon demo:** Strong  
**Local reproducibility:** Good with Docker  
**Production payment infrastructure:** Not ready

### Main reliability risks

- Random simulated outcomes make repeated live demos non-deterministic.
- Scheduler execution is not distributed-lock protected.
- Webhook idempotency is incomplete.
- There is no queue between webhook receipt and AI/recovery processing.
- There is no retry/dead-letter strategy for external outreach.
- The frontend has no authentication or resilient session model.
- Analytics load several full repository lists and may not scale to large data.

## 15. Razorpay Buildathon assessment

### Track selection

Choose **Track 03 - AI Revenue Recovery**.

### Requirement mapping

| Buildathon requirement | RecoverAI evidence | Assessment |
| --- | --- | --- |
| Detect revenue at risk | Failed payments, amount, status, failure categories | Implemented |
| Determine the right intervention | Gemini/rule decision strategy | Implemented |
| Execute a bounded workflow | Recovery service, scheduler, five-attempt limit | Implemented |
| Handle payment failures | Retry, notify, customer-guided, alternate method | Implemented |
| Compliant escalation | Customer action, promise-to-pay, outreach cooldown | Implemented for demo |
| Stopping rules | Fraud/account/attempt policy blocks | Implemented |
| Audit trail | AuditLog for decisions, transitions, outreach, outcomes | Implemented |
| Show measured money recovered | Dashboard batch/cohort comparison | Present, but seeded/demo evidence |
| Real Razorpay integration | Signed webhook ingestion | Partially implemented |

### Overall assessment

RecoverAI clearly fits the track and is worth submitting. Its strongest
differentiator is not merely retrying payments; it explains why a case should
be retried, asks the customer when appropriate, stops unsafe cases, and shows
the audit trail.

The main judging risk is evidence quality. A reviewer may ask whether the
recovered-money numbers are real measurements or seeded demo outcomes. Be
direct: demonstrate the reproducible synthetic batch, label simulation clearly,
and show the signed Razorpay Test Mode webhook as the real integration point.

## 16. Strengths

1. Exact alignment with the revenue-recovery track.
2. Complete workflow rather than only a classifier.
3. Customer-first insufficient-funds flow.
4. Deterministic policy takes precedence over AI.
5. Clear auditability.
6. Useful merchant dashboard and analytics.
7. Gemini fallback keeps the demo operational without an API key.
8. Dockerized local setup.
9. Public GitHub repository.
10. Automated validation already passes.

## 17. Weaknesses and prioritized improvements

### Priority 1 - before submission

- Record a focused five-minute video.
- Show the benchmark methodology and exact metric definitions.
- Demonstrate one signed Razorpay webhook.
- Keep the repository README honest about simulation.
- Fix the frontend default API-port message for Docker users.

### Priority 2 - strongest technical upgrade

- Add a durable webhook event table with event ID and unique constraint.
- Add webhook tests using official payload examples.
- Add a real Test Mode retry/checkout path, or clearly scope the webhook as
  ingestion-only.
- Replace random outcomes in the recorded demo with deterministic fixtures.

### Priority 3 - production hardening

- Add authentication, roles, and merchant tenant isolation.
- Add database migrations and indexes.
- Add queue-based scheduler execution with distributed locks.
- Add observability, structured logs, metrics, and alerting.
- Encrypt sensitive contact data and define retention.
- Add controller, integration, frontend, and end-to-end tests.

## 18. Recommended five-minute demo

### 0:00-0:30 - Problem

Explain that blind retry treats fraud, insufficient funds, and temporary
technical failures the same way.

### 0:30-1:20 - Dashboard

Show revenue at risk, recovered revenue, recovery rate, funnel, and recent
activity.

### 1:20-2:10 - AI decision

Open a failed payment and show the category, AI/rule source, confidence,
explanation, policy result, and created attempt.

### 2:10-3:20 - Customer-first recovery

Use an insufficient-funds case. Show waiting for customer, retry later,
promise-to-pay, and alternate payment method.

### 3:20-4:00 - Safety

Open a suspected-fraud case and show that policy stops recovery. Show the audit
log.

### 4:00-4:35 - Razorpay and reliability

Show the signed webhook endpoint or a prepared Razorpay Test Mode event, then
explain that invalid signatures are rejected and payment execution is
simulated for the demo.

### 4:35-5:00 - Results

Show the batch comparison, recovered INR, incremental revenue, unnecessary
blind retries, customer-action cases, policy-stopped cases, and maximum
attempts.

## 19. Suggested application wording

### What it solves

RecoverAI helps merchants recover revenue from failed payments without
blindly retrying every customer. It classifies failures with Gemini and
deterministic fallback rules, applies customer-protection guardrails, chooses
retry or outreach, supports promise-to-pay and alternate payment methods, and
records every decision in an audit trail.

### Why this track

This is an AI Revenue Recovery system: it detects money at risk, diagnoses the
failure, selects a bounded intervention, executes the workflow, and measures
recovered revenue across a synthetic batch. It also demonstrates compliant
stopping rules and customer-guided escalation.


## 20. Final verdict

RecoverAI is complete enough and relevant enough to submit to the Razorpay
Buildathon under Track 03. It demonstrates more than an AI label: it includes
decisioning, intervention, customer control, bounded execution, stopping
rules, auditability, and merchant-facing measurement.

The project should be presented as a polished functional demo with a clear
simulation boundary. The remaining success factor is not adding many more
features; it is showing a short, reliable, honest demo with reproducible
money-recovery metrics and a visible Razorpay Test Mode webhook flow.
