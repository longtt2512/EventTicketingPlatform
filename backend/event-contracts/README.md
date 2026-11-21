# Local event contracts — Phases 2–5

Owner: `event-commerce-service` writes canonical event/inventory state and its
transactional outbox. `discovery-service` owns rebuildable public/attendee
projections. No service queries another service's database.

The accepted implementation uses versioned JSON for local Phase 2. The system
architecture's Avro/Schema Registry choice remains proposed; these schemas do
not claim registered production compatibility or a production broker topology.

All messages carry an immutable `eventId`, `eventVersion: 1`, `occurredAt`,
`producer`, `aggregateType`, `aggregateId`, `correlationId`, `causationId` and
`payload`. Keys are the aggregate UUID. Producers acknowledge with `acks=all`
and idempotence. Every consumer commits its inbox and projection before manual
acknowledgement; duplicate and older entity versions cannot overwrite new state.

| Topic | Producer | Payload |
| --- | --- | --- |
| `event.event.published.v1` | event-commerce | Full event projection, non-null publishedAt |
| `event.event.approved.v1` | event-commerce | Current revision; unpublished events are excluded from public data |
| `event.event.cancelled.v1` | event-commerce | Current revision; retained public detail, removed from sales search |
| `event.event.updated.v1` | event-commerce | Other event, ticket, inventory or lifecycle changes |
| `ticket.ticket.issued.v1` | ticket-admission | Full attendee projection |
| `ticket.ticket.voided.v1` | ticket-admission | Full attendee projection with new version/state |
| `ticket.admission.checked-in.v1` | ticket-admission | Full attendee projection with new version/state |

The event payload follows `EventDetail` in `../openapi/phase2.json`, without
private `reviewReason`. The canonical revision is `payload.version`; the envelope
version describes the contract. The separate envelope schema is
`event-projection.v1.schema.json`.

The attendee contract is `attendee-projection.v1.schema.json`. It is a read model
integration point for Ticket & Admission, never a command for issuing tickets. There is no public write or fixture endpoint. Customer
names/emails never join the public event payload. Only Commerce's current
organization-management/Admin authorization may read the internal attendee list.

Each topic has a matching `.dlt`. After three bounded consumer retries, malformed
messages are sent there; failure to publish the DLT record prevents recovery
acknowledgement. Fix/validate a poison payload before replaying it. Inbox records
make replay idempotent. Failed outbox delivery remains pending with an attempt
counter and next availability time; stable message identities survive retries.

Local validation includes PostgreSQL concurrency, duplicate/out-of-order
projection tests, rollback on poison payloads, outbox failure/retry and real
Kafka delivery observed through the browser workflow. It does not establish
production HA, broker ACLs, Schema Registry policy or CI merge blocking.

## Local Phase 3 transaction facts

Decision `docs/decisions/0002-local-phase3-policy.md` selects a local simulator.
`payment-service` exclusively owns provider evidence/receipts/exceptions in the
`payment` schema. Commerce owns orders and allocation in `event_commerce`.
REST claims return a stable attempt ID across a lost response. Payment consumes
only the authoritative snapshot returned by Commerce; the browser sends no price.

| Topic | Producer | Consumer / effect |
| --- | --- | --- |
| `event.order.created.v1` | Commerce | Durable order snapshot |
| `event.order.confirmed.v1` | Commerce | Durable Phase 4 issuance obligation, immutable item IDs/quantities/prices |
| `event.order.cancelled.v1`, `event.order.expired.v1` | Commerce | Durable terminal order facts |
| `payment.payment.succeeded.v1`, `payment.payment.failed.v1`, `payment.payment.refunded.v1` | Payment | Commerce validates claim and applies at most one effect |
| `event.order.payment-result.v1` | Commerce | Payment records APPLIED or an exception reason |
| `payment.payment.exception-opened.v1` | Payment | Durable Admin/Finance integration point |

Payment evidence payload: `paymentId`, `attemptId`, `orderId`, integer `amount`,
`currency`, `status`. Result payload adds `eventId`, `result`, `expectedAmount`,
`receivedAmount`. Consumers reject unsupported envelope versions; each fact uses
transactional outbox/inbox, stable IDs, manual acknowledgement and a `.dlt` after
three retries. Local JSON compatibility does not claim a Schema Registry setup.

Mock callback contract: `POST /api/v1/webhooks/payments/local-mock`, raw UTF-8 JSON
signed by HMAC-SHA256 in `X-Mock-Signature`. Body includes deliveryId, provider
`local-mock`, configured merchant, attemptId, orderId, reference, amount, currency,
status, deliveredAt and paidAt. Signature covers the complete body; deliveredAt
must be within five minutes. Same delivery ID with changed bytes is rejected;
(provider,merchant,reference) deduplicates the underlying collection across new
delivery IDs. Receipts persist only a keyed payload digest and safe references.
The simulator is disabled unless `LOCAL_PAYMENT_MOCK_ENABLED=true` and a random
secret is supplied. Browser controls and return parameters cannot confirm orders.

Request keys are 8–128 URL-safe characters, retained with local history (no TTL
purge). Reservation keys are scoped to actor; payment keys to order. Conflicting
payload reuse fails. Order rows serialize expiry/confirmation with the event
lock shared by inventory and cancellation. An unknown attempt never times out to
FAILED merely because its order expires. Query/reconciliation reads the provider
ledger and re-enters the same verified receipt path.

The Phase 3 Notification slice also consumes `event.order.confirmed.v1`. Its
own schema retains one obligation per order, with inbox deduplication. SMTP uses
the existing local 30-second failure retry cadence; mail failure never mutates
Commerce state. A crash after SMTP accepted but before delivery bookkeeping can
repeat an email (SMTP does not offer exactly-once delivery). Local messages
explicitly identify simulated payments and link to current order state.


## Local Phase 4 fulfillment facts and cancellation fence

Ticket & Admission exclusively owns issuance units, credentials, admission
requests, history, outbox and inbox in `ticket_admission`. The immutable unit is
`(orderId, itemId, ordinal)`. Duplicate confirmed facts retain the same ticket and
credential. Source order/event/contact reads use authenticated REST, never another
service's schema. A failed dependency retains a pending issuance obligation.

| Topic | Producer | Consumer / effect |
| --- | --- | --- |
| `ticket.ticket.issued.v1` | Ticket & Admission | Discovery inserts one attendee per ticket |
| `ticket.ticket.voided.v1` | Ticket & Admission | Discovery applies the newer voided state |
| `ticket.admission.checked-in.v1` | Ticket & Admission | Discovery applies the newer successful admission |
| `ticket.order.ready.v1` | Ticket & Admission | Notification queues one ready email per order |
| `ticket.order.cancelled.v1` | Ticket & Admission | Notification queues one cancellation email per order |

Order notification payloads contain `orderId`, `customerId`, `eventId` and optional
`eventName`. The attendee v1 schema adds optional `orderId` and `checkedInAt`;
older v1 messages remain readable. Entity `version` prevents delayed issuance
facts from overwriting checked-in/voided state. Raw QR material never enters any
fact, list, history or log. Notification deduplicates by `(orderId, kind)` and keeps
failed deliveries pending. Envelope/inbox/DLT and producer guarantees above apply.

Commerce cancellation persists `cancellationPending` and a recovery obligation
before synchronously asking Admission to freeze the event. Admission serializes
freeze, issuance and scan decisions on the event gate. Only after acknowledgement
does Commerce commit public CANCELLED and its final event fact. A crash/outage
retains blocked allocation and a retryable cancellation; a duplicate freeze is
safe. This is a local recoverable protocol, not a distributed transaction claim.

Native integration tests prove allowed and forbidden issuance/admission cases,
same-key replay versus conflicting payload, concurrent scan/issuance/cancellation,
current permission checks, outbox retry, email retry and PDF/storage failure.
They run locally with `./gradlew test`; no CI or branch-protection change is made.


## Local Phase 5 financial facts and operational obligations

Authority: `docs/decisions/0005-local-phase5-policy.md`. Finance owns commission
configuration, immutable quotes, event settlement claims and payout evidence in
`finance`; Admin Operations owns editable operational settings and a read-only
consolidated audit projection in `admin_audit`. Both use dedicated database roles.
Commerce remains the owner of each order's immutable `commissionSnapshot` and
`appliedPaymentId`. The confirmed-order fact adds these fields; old facts without
a snapshot remain readable and financially unknown. Never infer historical fees.

| Topic | Producer | Consumer / effect |
| --- | --- | --- |
| `payment.payment.financial-updated.v1` | Payment | Finance/Reporting receive a versioned event ledger: attempts, verified collections, returns, reconciled fees and open exceptions |
| `finance.settlement.updated.v1` | Finance | Reporting receives the versioned batch and its current snapshot |
| `finance.settlement.completed.v1` | Finance | Reporting records authorized PAID or NO_TRANSFER completion |

Partial returns emit financial facts immediately, before final exception closure.
The versioned ledger is a complete replacement, not an increment to add again.
A platform ledger uses `ledgerScope: UNMAPPED`, a durable `ledgerId` and null
`eventId` for collections/returns that have no event assignment. Reporting includes
it only in the unfiltered global scope, never event sales or an Organizer report.
Its global version is independent of event versions; startup backfills it even
when no event ledger exists. Finance ignores this projection because payout
eligibility reads Payment's current global exception fence synchronously.
Missing gateway reconciliation remains unknown. An explicit zero is supported by
`LOCAL_PROVIDER_LEDGER` evidence with the matching provider reference. Unmapped
open collections invalidate every known event snapshot; settlement also checks
them synchronously. Duplicate/older facts cannot roll back the current projection.
The existing outbox/inbox, manual acknowledgment, bounded retries and DLT rules
apply. JSON v1 compatibility is local; no Schema Registry deployment is claimed.

Settlement authorization uses authenticated owner REST, not projection freshness:
Finance saves immutable evidence first; Payment locks the event ledger and global
unmapped exceptions; Organization serializes the current organization/payout check
with destination replacement. Each owner persists a proof for the same settlement
and evidence fingerprint. A lost response replays that proof, never a new transfer.
A committed proof is historical authorization even if state later changes. A new
conflicting decision, amount or destination cannot reuse it. Actual bank transfer
is outside the local implementation.

Reporting backfills event and confirmed-order facts from Commerce's paged internal
feed, including legacy orders, and cycles the feed to catch commits behind a
cursor. Admin audit similarly copies canonical owner records using `(source,id)`
deduplication. Neither process reads a foreign schema. Reports expose source sync
status and frozen snapshot identity; no claim of atomic cross-service freshness.

Organization, Commerce and Finance persist notification obligations in the same
transaction as their business decision. Notification polls authenticated pending
feeds, commits recipient obligations, then acknowledges the owner. Approval,
rejection, event cancellation and settlement mail deduplicate by owner notice and
recipient. Reminder deduplication is `(event,recipient)` across orders and setting
changes; both Commerce's current event/order eligibility and Admission's current
non-voided ticket/open gate are checked before send. SMTP failure retains the
30-second retry obligation. An ineligible reminder is retained without sending;
a send/commit crash can still repeat SMTP delivery, as in earlier phases.

Emails support Vietnamese/English with Vietnamese fallback and authenticated
links. They never contain QR credentials, full bank details or private evidence.
Contracts for the public operations APIs: `../openapi/phase5.json`, with generated
TypeScript at `frontend/libs/api-client/src/phase5.generated.ts`.
