# Execution Plan: Phase 4 — ticket fulfillment and online admission

Date: 2026-11-20
Status: Completed after local executable and visual verification.

## Outcome and authority

Confirmed orders issue one stable opaque QR ticket per unit. Customers have a
private ticket list/detail/PDF and recoverable issuance progress. Authorized staff
use camera scanning or explicit manual confirmation, with unique admission,
current permissions, history and statistics. Organizers see actual attendee
projections. Cancellation stops allocation, freezes admission and voids unused
tickets while preserving history. Ready/cancellation mail uses authenticated links.

Authority: PRD ETK-01–10 and CHK-01–10; BRS §§10–11 and T26–32/T50;
UI C-13–14, S-01–06, O-09–10; SDD §6.3; API §10;
`docs/decisions/0003-local-phase4-policy.md`. The owner accepted the local policy
and authorized implementation. Existing Indigo/Be Vietnam Pro UI authority and
separate-schema, REST, JSON Kafka outbox/inbox and private MinIO patterns remain.

## Delivered

- Ticket & Admission service/schema with durable issuance, unique order/item/unit
  identity, encrypted credential storage, private APIs, QR/PDF and an outbox.
- Admission checks server time in `[startAt - 2h, endAt)`, current account and
  organization/event permission, ticket status and exact event. A conditional
  transition and unique admission serialize concurrent scans. Request replay is
  explicit and cannot be mistaken for another new admission.
- Recoverable Commerce cancellation request and Admission fence. An outage keeps
  allocation blocked and a retryable obligation; no sold stock is restored.
- Discovery attendee state/order/time projections and deduplicated, retryable
  Notification deliveries. Late confirmed facts for frozen events also emit the
  cancellation notice without issuing usable tickets.
- Customer views/PDF; staff camera switching, permission failure fallback,
  exact-ID lookup, confirmation, offline/unknown/replay states, history and stats;
  organizer attendee/dashboard links and cancellation-pending feedback.
- OpenAPI/generated TypeScript, event contract documentation, additive schema
  provisioning, owned runtime configuration and repeatable synthetic workflows.

## Validation evidence

| Check | Observed result |
| --- | --- |
| `./backend/gradlew -p backend test bootJar` | 121 tests, zero failures/errors/skips |
| `npm --prefix frontend run test:unit` | 117 passed |
| `npm --prefix frontend run build:all` | All four production builds passed |
| Phase 1 browser regression | 88 checks passed |
| Phase 2 browser regression | 65 checks passed |
| Phase 3 browser regression | 77 checks passed |
| Phase 3 transaction boundaries | 8 checks passed |
| Fresh Phase 4 browser workflow | 76 checks passed, no JS errors/unexpected 5xx |
| PDF visual QA | Valid/used/voided and long Vietnamese two-page PDFs rendered and inspected |
| Private MinIO artifact | Unauthenticated retrieval returns HTTP 403 |
| UI source detector | No findings on Phase 4 targets; regex scan only |

Backend proof covers allowed versus unconfirmed/duplicate issuance, immutable
units, failed issuance retry, private versus foreign owner access, wrong event,
outside window, concurrent scan/issuance/cancellation, same-key replay versus
changed payload, current revoked/suspended/unverified access, outbox retry,
cancellation fence recovery, mail retry and PDF/storage failure. A long-content
layout test verifies wrapping and page separation. These are local validations,
not hook, CI, branch-protection or production enforcement claims.

Frontend coverage: 87.18% lines, 81.05% statements, 63.01% branches, 79.45%
functions. Browser proof uses real local application services, PostgreSQL, Kafka,
MinIO and MailHog. It buys free/paid units, decodes actual generated QR pixels,
loses a response after the real admission commit and recovers with the same key,
revokes/restores assignment, suspends/restores organization, cancels the event,
rejects the cached QR and verifies retained counts/history and ticket email.

Evidence: service `build/reports/tests/test/`, frontend coverage output,
`output/playwright/phase{1,2,3,4}-result.json`, `phase3-edge-result.json`,
`phase4-*.png`, `phase4-ticket-{valid,used,voided}.pdf` and Admission
`build/qa/ticket-long.pdf`. Responsive captures cover 390/768/1440px; a 320px
fit and keyboard action check also pass. Instructions are in
`docs/local-development.md`.

## Limits and retained state

Local scope only: no push, deployment, real payment, offline admission queue,
transfer or resale. Physical camera/mobile browser hardware has not been tested;
Playwright supplies a synthetic video feed containing actual QR pixels to the
shipped decoder. Visual inspection and source scanning do not establish WCAG
conformance. Production HA, key rotation/retention and abuse quotas remain outside
this accepted local decision. SMTP may duplicate delivery after a send/commit crash.

Extensive pre-existing untracked work was preserved. Only additive migrations and
synthetic fixtures were used; no existing Compose project/volumes were removed.
The isolated local runtime remains available under its owned launcher.

## Task-local friction and recovery

A stray generated class named `EventManagementIntegrationTest 2` blocked Gradle
discovery. Cleaning only Commerce generated build outputs fixed it; no source or
user data was removed. Its final runtime jar now matches the cleaned build.

Concurrent launcher mutations once overwrote the frontend PID entry. The exact
PID/command from our start was verified and its ownership restored; subsequent
launcher mutations were sequential. No harness tooling or policy was changed.

Browser runner corrections: each Phase 4 run now creates a fresh event (avoids
accumulated ticket counts), opens the existing “Xem lại” step before attendees,
and isolates Admin in its own browser context so login cannot replace the buyer
refresh cookie. The final fresh 76-check run passes all of these paths.
