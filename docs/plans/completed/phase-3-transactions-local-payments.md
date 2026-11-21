# Execution Plan: Phase 3 — booking and local simulated payments

Date: 2026-11-20

## Status

Completed and validated on 2026-11-20. User authorized implementation and accepted the local rules in
[decision 0002](../../decisions/0002-local-phase3-policy.md).

## Outcome

A customer selects tickets, reserves for ten minutes, confirms a free order or
uses a clearly labelled local payment simulator, and reads authoritative history.
Admin reconciles simulated payment exceptions with an audit trail. Real database
transactions prevent overselling, duplicate effects and confirmation after expiry.

## Context and scope

PRD §6.6–6.7 (ORD-01–15, PAY-01–13), BRS §8–9, UI C-08–12/O-08/A-09–10,
SDD §4–6, API §8–9, decision 0002. Existing Phase 2 allocation, media and discovery
are retained. Real providers, QR/ticket issuance, settlement and deployment are
out of scope. No fabricated issued tickets or attendees.

## Approach

1. Extend Commerce with orders, atomic reservation/expiry, snapshots and safe
   payment claims. Reuse native PostgreSQL transactions and event outbox.
2. Add isolated Payment service and schema, durable mock provider evidence,
   signature verification, reconciliation, Kafka outbox/inbox and exception review.
3. Extend Customer/Organizer/Admin Angular flows in the existing Indigo/Be Vietnam
   Pro visual system. Checkout shows order summary, server-backed timer and one
   primary action; mock controls are explicitly local and separate from the result.
4. Integration tests for concurrency/security/recovery, builds and real browser
   flows at desktop/tablet/mobile; Impeccable polish after observable inspection.

## Risks and recovery

Extensive user work is untracked: do not reset, clean, push or deploy. Use additive
migrations and only owned validation processes/Compose. Separate schema roles;
no cross-service SQL. Keep callbacks and order confirmation recoverable during
broker/process failures. Do not expose fixture secrets in output or screenshots.

## Progress

- [x] Inspect authority; record accepted local policy and architecture.
- [x] Commerce order/reservation APIs and invariant proof.
- [x] Payment simulator, durable evidence/reconciliation and exception proof.
- [x] Angular purchase, order history and management screens.
- [x] Contracts, integration, browser/visual evidence and final regression.

## Validation

Native `./gradlew test bootJar`, `npm run test:unit`, `npm run build:all`.
Positive/negative cases: simultaneous reservations, whole-order rollback, stale
selection, foreign order, unverified customer, forged/repeated/mismatched/late
callbacks, free orders, failure retry, unknown payment, event cancellation,
expiration/confirmation races, broker retry and Admin evidence constraints.
Browser proof must use real local backend; only the provider is a simulator.
Local enforcement only; no CI/hooks/branch-protection changes.

## Result

Phase 3 is implemented and validated locally. Purchase, free/paid confirmation,
expiry, customer history, scoped organizer access, Admin exception handling and
confirmation email work against the real local services. Only payment-provider
behavior is mocked. No push, deployment, real money movement or QR issuance.

## Implementation notes

Commerce owns immutable order/item snapshots, atomic multi-line holds and terminal
order transitions under the existing event lock. Quote versions compare accepted
price/terms without invalidating every concurrent booking. Durable request keys
and stable payment claims recover lost responses. Free orders consume inventory
without a provider. Payment evidence never reopens expired/cancelled orders.

Payment has a separate schema, local provider ledger, signed receipt verification,
transaction/delivery deduplication, reconciliation, outbox/inbox and Admin audit.
Eight scenarios exercise successful, failed, unknown, delayed, duplicate, late,
wrong-amount and lost-callback outcomes. Admin return evidence supports partial
amounts, reviewer ownership and idempotent replay at database time precision.
Missing signed receipt fields are rejected before persistence. Mock is opt-in;
default Payment port 8087 avoids the existing Schema Registry port 8085.

The Customer flow preserves selection through login, uses server-backed countdowns
and explains unknown/exception states. Organizer order access is scoped/read-only.
Admin review and return actions require explicit confirmation; evidence validation
and unsaved-form protection reuse Phase 1 components. Contracts are published in
`backend/openapi/phase3.json` with generated frontend types.

Notification follows SDD §16's requirement that necessary email ships with its
domain. Its isolated schema/service consumes confirmed-order facts, deduplicates
by order and sends an honest local confirmation through MailHog. Recipient contact
comes from service-authenticated Identity REST, never cross-service SQL. SMTP
failure retains the obligation and cannot roll back a confirmed order.

## Visual review

Impeccable polish retains the established Indigo/Be Vietnam Pro system. Real
browser captures were reviewed at 390/768/1440px; tablet purchase content was
changed to one column after the original two-column view proved too narrow.
Admin tabs, audit spacing, Vietnamese action labels, visible zero monetary
balances and explicit mock copy were refined. Capture paths:
`output/playwright/phase3-{selection,checkout,mock,success,exception,admin-exception,history,organizer-orders}-*.png`.

Impeccable source detector ran once over the Phase 3 library and shared SCSS:
12 TypeScript/SCSS files, regex-only, no reported match. It did not parse rendered
contrast/tiny-text rules and is not a clean accessibility bill of health. Native
confirmation focus/cancel checks and targeted keyboard/reflow proof supplement it.
Full screen-reader and browser zoom audits are not claimed.

## Evidence and limits

- Native backend suite: 94 tests, zero failure/error/skip. Baseline 46 plus
  20 Commerce order, 24 Payment, one Payment outbox and three Notification tests.
  Full suite/build passed; final Payment changes passed its focused suite/build.
- Frontend unit suite: 91 tests pass. Four application production builds pass.
  Measured coverage: statements 84.93%, branches 66.6%, functions 81.79%,
  lines 88.44%; no complete-coverage or release-hardening claim.
- Browser regression: Phase 1 88 and Phase 2 65 checks passed, no JS exceptions
  or unexpected 5xx. Phase 3 passed 77 main checks plus eight boundary checks
  (238 total browser checks across all three phases). Reports are in
  `output/playwright/phase{1,2,3}-result.json` and `phase3-edge-result.json`.
  Purchase also passed targeted 320px, keyboard/accessible-name and 200%
  text-scaling checks; the latter is not a full-browser zoom test.
- Boundary browser: eight checks pass for expiry, late success, delayed result
  and lost callback. Only newly created synthetic fixture deadlines were shortened
  in isolated validation PostgreSQL; normal ten-minute behavior is independently
  verified by the main workflow. No application clock/policy bypass is installed.
- Backend tests exercise concurrency, rollback, foreign ownership, unverified
  customer, stale selection, forged/replayed/malformed callbacks, amount/currency
  mismatch, event cancellation, expiry races, original-payment preservation after
  extra-collection return, evidence constraints, outbox retry and email retry.
- This is local enforcement by executable commands, not CI or branch protection.
  Real payment gateways, real refunds, fees/settlement, QR/PDF issuance, check-in,
  production load/security/accessibility/recovery certification remain outside this
  phase. SMTP can duplicate mail after a crash between send and bookkeeping.

## Reusable friction observed

The static frontend server can encounter removed hash-named assets if rebuilt
while serving browser checks. Validation stopped only its owned frontend process
before each build and restarted it afterward. No generic harness/tool changes were
made for this observation. Random-order unit execution also exposed a test leaving
its own request key behind; that test now cleans up its owned key.
