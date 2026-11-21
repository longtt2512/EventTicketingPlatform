# Execution Plan: Phase 2 — events, inventory and discovery

Date: 2026-11-20

## Status

Completed — 2026-11-20. User explicitly authorized completion of Phase 1 followed by Phase 2.

## Outcome

Organizers prepare image-backed event drafts and ticket types, submit for Admin
review, publish an approved revision, manage future inventory and cancel with
an audited reason. Guests discover public events and view current sale states.

## Context and authority

- PRD §6.3–6.5: EVT-01–14, INV-01–13, DSC-01–11.
- BRS §5–7; UI/UX C-01–04, O-03–07, A-05–06.
- System design §4–7: Commerce owns canonical writes; Discovery owns an eventual
  Kafka projection with outbox/inbox, separate schema and application role.
- API spec §7/12: version-safe moderation, presigned media, public discovery.
- Accepted policy: [0001-local-phase2-policy](../../decisions/0001-local-phase2-policy.md).

## Scope

Event authoring, image media, lifecycle, ticket configuration/counters, allocation
foundation, review/history, public discovery, organizer details and attendee view.
Order/payment checkout and ticket issuance remain Phase 3; attendee view must
show the true empty state until real issued-ticket data exists. No fabricated sales.

## Approach

1. Finish verification resend and rerun Phase 1 browser evidence.
2. Extend Commerce with versioned events, tickets, validated media, audit and outbox.
3. Add isolated Discovery projection with Kafka deduplication/version ordering.
4. Extend existing Indigo/Be Vietnam Pro Angular surfaces; accessible form feedback,
   dirty guards and confirmation dialogs carry forward. No identity redesign.
5. Prove invariants in PostgreSQL tests and workflows on real local services, then
   inspect desktop/mobile views and apply Impeccable polish.

## Risks and recovery

Existing repository has extensive untracked work; no reset, push or deployment.
Baseline snapshot: `/tmp/etp-phase2-baseline-1795132502144001000.tar.gz`.
Migrations are additive and tested on isolated Testcontainers/validation Compose.
Owned services are managed only by scripts/run-validation.py. No local user data
or shared Docker resources may be deleted. Kafka outages must retain outbox work;
projection delays are visible and must never authorize inventory writes.

## Progress

- [x] Record accepted product policies and inspect applicable authority.
- [x] Close Phase 1: 30 backend + 73 frontend tests, four builds and 88 browser checks.
- [x] Commerce schema, lifecycle, media and inventory proof.
- [x] Kafka outbox and isolated Discovery projection proof.
- [x] Organizer/Admin/Customer interfaces and generated contracts.
- [x] Full build, integration, browser and visual validation.

## Validation

Native `./gradlew test bootJar`, `npm run test:unit`, `npm run build:all`.
Positive/negative proof: incomplete submit, stale review/publish, foreign scope,
invalid media, capacity/quota reduction, simultaneous allocation, cancelled sales,
draft discovery leak, duplicate/out-of-order projection and resend concurrency.
Real browser: author → upload → ticket → submit → reject/correct → approve →
publish → search/detail → pause/resume → cancel; responsive and keyboard checks.
Local tests provide enforcement evidence; no CI/branch protection changes.

## Result

Delivered locally: four-step event authoring with real image upload, version-safe
moderation/publication/cancellation and audit, ticket configuration and allocation
invariants, public discovery and scoped attendee projection. Existing Indigo UI
was polished and inspected at desktop/tablet/mobile sizes.

Final evidence: **46 backend tests, 78 frontend tests, four Angular production
builds, 88 Phase 1 browser checks and 65 Phase 2 browser checks passed**. Final
browser reports contain no JavaScript exceptions or unexpected 5xx responses.
Evidence lives in `output/playwright/phase1-result.json`, `phase2-result.json` and
responsive `phase2-*.png` captures. Execution logs are in ignored `.runtime/phase1`.

The task did not push, publish or deploy. Local apps remain running for review.
Order/payment checkout and actual ticket issuance remain later-phase integrations;
attendee reads are functional against their read model and correctly empty until
real ticket facts arrive. Production limits are recorded below and in the runbook.

### Implementation checkpoint

Commerce now implements event revision/lifecycle, validated image intents,
versioned ticket configuration, internal atomic allocation ledger and Kafka outbox.
Discovery has its own application role/schema, deduplicating versioned projection,
server-time sale states, cursor search and a scoped attendee read model. Attendee
records are consumed from Ticket & Admission facts; the current UI correctly shows
an empty list until that later-phase producer issues actual tickets. No attendee
is manufactured in normal runtime. Synthetic facts are used only in isolated tests.

Phase 1 proof is closed in `docs/plans/completed/`. Initial Phase 2 tests detected
and fixed a Spring repository proxy field-access bug; use the latest logs, not
that failed intermediate run, for final evidence. Four frontend builds and 77
unit checks currently pass. Real Phase 2 browser validation is complete (final evidence above).

### Final verification checkpoint

- 46 backend tests passed: identity 21, organization 7, commerce 13, discovery 5.
- 78 frontend tests passed; four Angular production builds passed.
- Real Phase 2 workflow passed 65 checks after final polish and recapture;
  Phase 1 regression passed 88 checks after integration.
- Impeccable detector ran once on the changed frontend source: no findings;
  it reported regex-only coverage on 14 TS/SCSS files. Parsed-page contrast and
  element rules did not run, so this is not a full automated accessibility audit.
  Visual inspection covers the real editor/review/discovery/detail at 390, 768,
  1440px; table columns remain readable through horizontal scrolling with a hint.
- Keep: Indigo palette, Be Vietnam Pro, existing form feedback, explicit status,
  focused confirmation dialogs, drafts retained only in memory while unsaved.
- No hooks, CI, branch protection, publication or deployment were changed.

The native checks are local enforcement evidence. Production Schema Registry,
HA/ACLs, full WCAG audit, media malware scanning and checkout/issuance remain
outside this delivery. These do not get replaced by invented data or mocks in
normal runtime. The browser workflow uses explicitly synthetic fixtures.

### Traceability and behavioral evidence

| Scope | Delivered path and proof |
| --- | --- |
| EVT-01–08 | Existing organization draft entry → four-step event editor; category, venue/time/capacity/terms and direct MinIO banner/gallery. Browser validates actual vs spoofed PNG and retained form after invalid quota. |
| EVT-09–13 | Submit/reject/correct/approve/publish/cancel, revision locks, reason/history, server-time catch-up; PostgreSQL tests and real organizer/Admin browser flow. |
| EVT-14 | Current-scope attendee endpoint backed by Discovery's ticket-fact projection; dedup/order/state tests, foreign event isolation, internal authentication and true empty UI before later-phase issuance. |
| INV-01–09 | Ticket configuration, integer/free VND, half-open sale windows, future price/quota changes and pause/resume confirmations. |
| INV-10–13 | Canonical available/reserved/sold, internal allocation ledger and price snapshots. Twenty concurrent allocations against ten tickets admit exactly ten; multi-line failure rolls back; expire/cancel release once; suspended organization denied. |
| DSC-01–11 | Guest featured/upcoming/popular, query/category/city/date/price, details, organizer info and sale-state presentation. Separate role/schema, Kafka outbox/inbox, keyset pages and visible source update time. |
| P1 continuity | Existing form feedback, dirty guards, safe return URLs and consequential confirmation shared by the new screens. Full Phase 1 regression remains 88/88. |

No booking/payment/QR capability is asserted from the allocation or attendee
foundation. Those depend on later-phase order and Ticket & Admission producers.
