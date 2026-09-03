RecoverAI

AI-Powered Revenue Recovery

RecoverAI is an AI-guided failed-payment recovery platform built for the
Razorpay Buildathon --- Track 03: AI Revenue Recovery.

The system detects failed payments, evaluates recovery decisions,
executes bounded recovery actions, keeps customers in control when
required, and records recovery outcomes and audit events.

The project includes a Spring Boot backend, MySQL persistence, a
React/Vite operations dashboard, and Gemini-powered recovery decision
support.

What RecoverAI Does

RecoverAI turns a failed payment into a structured recovery workflow:

Detect a failed payment.

Evaluate the payment and recovery context.

Generate or retrieve an AI recovery decision.

Select an appropriate recovery path.

Execute an allowed recovery action or request customer input.

Track the resulting recovery state.

Record the workflow in the audit trail.

Surface recovery performance and failure analytics in the dashboard.

The objective is not simply to retry every failed payment. RecoverAI
applies bounded recovery behavior and supports customer-directed
recovery when an intervention requires customer action.

Key Features

AI-Assisted Recovery Decisions

RecoverAI integrates AI decision support for recovery actions such as:

Retry

Notify customer

Stop recovery

AI decisions include information such as:

Decision

Recovery action

Confidence score

Decision source

Reasoning

Creation timestamp

The Payment Details view exposes these recovery insights for individual
payments.

Automated Recovery

Eligible recovery attempts can move through the recovery lifecycle
automatically.

Successful recovery marks the associated payment as RECOVERED.

Failed recovery attempts are tracked separately and can require
additional customer action or escalation according to the recovery
state.

Customer-in-the-Loop Recovery

When customer intervention is required, RecoverAI supports:

Retry later

Promise to pay

Another payment method

Alternative payment method submission

For example, a customer can select a future retry time instead of being
repeatedly charged immediately.

Recovery Guardrails

The recovery workflow includes bounded behavior such as:

Maximum recovery-attempt limits

Customer-action states

Scheduled retry timing

Promise-to-pay deadlines

Outreach cooldown behavior

State-transition validation

Prevention of execution outside allowed recovery states

These controls are intended to prevent uncontrolled retry behavior.

Recovery Analytics

The dashboard provides operational metrics including:

Revenue at risk

Revenue recovered

Active recovery cases

Recovery success rate

Payment status distribution

Recovery activity by day

Failure categories

Failure reasons

Recent recovery activity

The recovery success rate is calculated from completed recovery
attempts:

successful recovery attempts / (successful + failed recovery
attempts)

Audit Trail

Recovery actions and state changes are recorded in the audit trail.

Payment Details exposes:

Recovery attempts

Attempt status

Recovery method

Responses

Audit actions

Audit details

Timestamps

This provides an observable history of how a payment moved through the
recovery lifecycle.

Recovery Lifecycle

A typical recovery lifecycle is:

FAILED PAYMENT
|
v
AI RECOVERY DECISION
|
+--------------------+
|                    |
v                    v
AUTOMATIC RECOVERY     CUSTOMER ACTION
|                    |
|          +---------+---------+
|          |         |         |
|          v         v         v
|       RETRY    PROMISE    ANOTHER
|        LATER    TO PAY     METHOD
|                              |
|                              v
|                     ALTERNATIVE METHOD
|                              |
+--------------+---------------+
|
v
RECOVERY OUTCOME
/           \
v             v
SUCCESS         FAILED
|
v
RECOVERED

The actual path depends on the payment state, recovery policy, AI
decision, and customer action.

Technology Stack

Frontend

React

Vite

Axios

CSS

Backend

Java

Spring Boot

Spring Web

Spring Data JPA

Jakarta Validation

Database

MySQL

AI

Gemini integration for recovery decision support

Application Structure

RecoverAI
|
+-- recoverai-frontend
|   +-- src
|   |   +-- App.jsx
|   |   +-- services
|   |       +-- api.js
|   +-- public
|   +-- package.json
|   +-- vite.config.js
|   +-- .env.example
|   +-- README.md
|
+-- recoverai-backend
+-- Spring Boot application
+-- Controllers
+-- Services
+-- DTOs
+-- Entities
+-- Repositories

The frontend communicates with the Spring Boot REST API. The backend
owns recovery-state transitions, persistence, recovery execution,
analytics, and audit behavior.

Frontend Views

Recovery Dashboard

Provides an operational overview of:

Revenue at risk

Revenue recovered

Active recovery cases

Recovery rate

Recovery funnel

Recent autonomous activity

Recovery trends

Recent failed payments

Recovery Attempts

Allows an operator to select a payment and inspect its recovery-attempt
history.

Payment Details

Provides the complete payment view, including:

Payment information

AI recovery insights

Customer recovery actions

Recovery history

Audit log

Analytics

Provides:

Recovery performance

Payment outcomes

Backend recovery analytics

Failure categories

Ranked failure reasons

Backend API Overview

The frontend uses the following REST endpoints.

Payments

GET /api/payments
GET /api/payments/{paymentId}/details

Dashboard

GET /api/dashboard/stats
GET /api/dashboard/recent-activity
GET /api/dashboard/summary
GET /api/dashboard/analytics
GET /api/dashboard/recovery-trends
GET /api/dashboard/failure-reasons

Recovery

GET  /api/recovery/payment/{paymentId}

POST /api/recovery/{paymentId}

POST /api/recovery/attempts/{id}/schedule
POST /api/recovery/attempts/{id}/retry-later
POST /api/recovery/attempts/{id}/promise-to-pay
POST /api/recovery/attempts/{id}/notify
POST /api/recovery/attempts/{id}/choose-payment-method
POST /api/recovery/attempts/{id}/alternative-payment-method
POST /api/recovery/attempts/{id}/execute

PUT /api/recovery/{id}/status

The exact request and response contracts are implemented by the Spring
Boot backend.

Local Setup

Prerequisites

Install:

Java

Maven

Node.js and npm

MySQL

1. Start MySQL

Create or configure the recoverai database according to the backend
application's database configuration.

2. Start the Backend

Open the backend project in IntelliJ IDEA or run it through Maven.

The backend is configured to run on:

http://localhost:8080

3. Start the Frontend

From the frontend project directory:

npm install
npm run dev

The Vite development server is available at:

http://localhost:5173

The backend should be running on port 8080 before opening the
dashboard.

Frontend Configuration

Copy .env.example to .env.local when a local environment-specific
configuration is required.

Example:

VITE_API_URL=http://localhost:8080
VITE_SIMULATION_MODE=true

Configuration Variables

Variable                 Default                   Purpose

VITE_API_URL           http://localhost:8080   Spring Boot backend
base URL

Do not commit private credentials or API keys to the repository.

Recovery States

RecoverAI uses recovery states to control what actions are valid.

Important states include:

PENDING
PROCESSING
WAITING_FOR_CUSTOMER
WAITING_FOR_PAYMENT_METHOD
SCHEDULED
SUCCESS
FAILED

Customer-directed actions can move an attempt into states such as:

WAITING_FOR_CUSTOMER
|
+--> SCHEDULED
|
+--> WAITING_FOR_PAYMENT_METHOD

A successful recovery ends in:

SUCCESS

and the associated payment is marked:

RECOVERED

Analytics Model

RecoverAI distinguishes between:

Failed payments --- payments currently marked as failed.

Recovered payments --- payments that were subsequently
recovered.

Recovery attempts --- individual recovery actions associated
with failed payments.

Successful recovery attempts --- recovery attempts that
completed successfully.

Failed recovery attempts --- recovery attempts that completed
unsuccessfully.

Active recovery cases --- payments with recovery activity still
requiring action.

The dashboard's recovery rate is based on completed recovery attempts
rather than total payments. This prevents unrelated successful payments
from distorting the recovery-performance metric.

Simulation Mode and Limitations

This project is designed as a buildathon/demo implementation.

Payment outcomes and recovery execution use the project's simulation
behavior rather than connecting to real payment processing
infrastructure.

Outreach is represented and recorded through the recovery workflow and
audit trail. The demo does not send real customer messages or charge
real payment methods.

Do not use the application to process real payment credentials or real
customer payment data.

Quality Checks

From the frontend project directory:

npm run lint
npm run build

A successful production build generates the Vite dist/ directory.

Project Goal

RecoverAI is designed to demonstrate the core revenue-recovery loop:

Detect Risk
↓
Understand Failure
↓
Choose Recovery Action
↓
Respect Customer Control
↓
Execute Within Guardrails
↓
Recover Revenue
↓
Measure Outcome
↓
Record Audit Trail

The system focuses on measurable recovery outcomes while keeping
automated interventions bounded and observable.