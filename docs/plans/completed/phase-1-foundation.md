# Execution Plan: Phase 1 Foundation

Date: 2026-11-19

## Status

Completed — 2026-11-20. The owner accepted the 60-second resend cooldown and
Phase 2 policies in [decision 0001](../../decisions/0001-local-phase2-policy.md).
Phase 1 and the four P1 UX groups are validated. Earlier pending notes below
are historical and superseded by this checkpoint.

Final Phase 1 proof: 30 backend tests, 73 frontend tests, four production builds,
and 88 real Playwright checks passed. Browser report:
`output/playwright/phase1-result.json`; resend capture:
`output/playwright/resend-mobile.png`. The run waited the real 60 seconds,
confirmed initial-mail suppression, received a second MailHog message and
verified the new token. No JavaScript exceptions or server 5xx responses.

Phase 2 is also complete: [implementation and proof](phase-2-events-inventory-discovery.md).
Local validation only; production malware scanning, deployment and CI enforcement
are not claimed. Prior Impeccable pass retains the Indigo design system; the
resend screen was additionally inspected at 390px.

## Outcome

Deliver the first runnable local vertical slice of the Event Ticketing Platform:
Gradle-based Spring Boot microservice scaffolding, local Docker dependencies,
Identity & Access, Organizer onboarding with Admin approval, and Angular
workspace shells that can exercise the authenticated flow.

## Context

- Product authority: `docs/01-product-requirements.md`, Appendix B Phase 1,
  AUTH-01–11 and ORG-01–09.
- Business authority: `docs/02-business-rules.md`, IDN-R01–R08 and ORG-R01–R08.
- Architecture: `docs/04-system-design.md`; each service owns its database and
  cross-service communication uses REST/Kafka contracts.
- Database: `docs/05-database-design.md`; PostgreSQL migrations, outbox/inbox and
  MinIO media metadata are service-owned.
- API/security: `docs/06-api-specification.md` and `docs/07-security-design.md`.

## Scope

In scope:

- Local Docker Compose for PostgreSQL, Kafka, Schema Registry, Redis, MinIO and
  MailHog.
- Gradle multi-project backend with independent Spring Boot service modules.
- Identity service foundation: registration, login, verification, refresh,
  logout, profile, password change/reset and account status guards.
- Organization service foundation: organizer application, organization
  membership and Admin approve/reject workflow with scoped authorization.
- Minimal Admin approval API needed to complete the Phase 1 vertical slice.
- Angular 17 standalone workspace shells and auth/organizer/admin routes.
- Focused unit, API and integration tests for security and Phase 1 state guards.

Out of scope:

- Event Commerce, ticket inventory, booking, payment, ticket issuance and
  check-in.
- Production Kubernetes, observability stack and multi-environment deployment.
- Automated payout/refund, MFA and final payment-provider integration.

## Approach

1. Create the repository scaffold and local infrastructure.
2. Generate the Gradle wrapper and verify the empty multi-project build.
3. Implement Identity & Access with PostgreSQL migrations and Spring Security.
4. Implement Organization onboarding and Admin approval with ownership checks.
5. Create Angular applications/shared libraries and connect the auth flow.
6. Add focused security and integration tests, then run local smoke tests.

## Risks And Recovery

- Dependency downloads or Docker availability may block proof; keep native build
  commands and report the exact missing prerequisite.
- Service boundaries must not collapse into a shared database; each migration
  stays under the owning service.
- JWT/refresh policy has proposed defaults in the security design; record any
  implementation-specific choice as `PROPOSED` until reviewed.
- If a partial scaffold is unusable, remove only generated Phase 1 files and
  rerun the corresponding step; do not delete existing docs or user files.

## Progress

- [x] Local infrastructure and root project scaffold (six running containers;
  Identity and Organization V1 migrations observed successful).
- [x] Existing Identity APIs, persistence, security and tests (resend extension validated).
- [x] Organization APIs, persistence, authorization and tests.
- [x] Angular workspace and Phase 1 routes/forms (resend recovery validated).
- [x] Existing AUTH/ORG end-to-end proof; resend proof is still required before completion.

### Continuation baseline and completion gates

- Identity fixes now compile; 7 PostgreSQL/Testcontainers integration tests pass
  (profile and role injection, logout access revocation, refresh replay rollback,
  failed-login persistence, locked JWT rejection, one-time verification, password
  reset preserving administrative lock). This is intermediate proof, not final
  Phase 1 completion. Frontend shared auth/session/profile and workspace shells
  are being completed while owner policy decisions remain pending.
- All four Angular production builds pass at the auth/profile checkpoint.
  Isolated validation infrastructure is running; identity 18081, organization
  18082 and gateway 18080 returned `UP`. Process IDs and logs are under ignored
  `.runtime/phase1/`; `scripts/serve-frontends.mjs` serves ports 14200–14203.
  Playwright CLI session `phase1` is testing the real UI. An initial navigation
  before the frontend was ready failed; after startup `/login` renders correctly.

- Existing code and product docs are untracked user work; preserve them. No
  publishing, deployment, push or commit is authorized by this task.
- Four Angular apps pass `ngc --noEmit`; existing backend JARs are not fresh
  validation. No business tests exist yet. Current DB contains three verified
  users, one pending application and no organizations/memberships.
- Scope matrix: AUTH-01–11; ORG-01–09; supporting Admin user lock/unlock,
  application moderation, organization status and payout review; email delivery.
- Pending owner decisions: explicit Phase 1 security/organization defaults;
  minimal Event Commerce draft ownership boundary for ORG-08. Asked via async
  clarification. Do not implement dependent new policies until accepted.
- Accepted invariant authority: BRS IDN-R02/03/04 (current server-side identity,
  role and status; locked user denied despite valid JWT), ORG-R03/08 (current
  scoped membership/assignment, no cross-organization assignment). JUnit/API
  tests must prove both allowed and rejected requests with stable error codes.
- Required final proof: backend tests/build, four production frontend builds,
  UI critical-state tests and Playwright user workflows including negative
  authorization, token replay, resubmission, payout masking, member/staff revoke,
  suspension/restore, mobile layout and console/network inspection.
- Use isolated validation data and record startup/cleanup ownership in a local
  application runbook. Do not modify existing local users or pending application.

## Decisions

- 2026-11-19 continuation: owner explicitly accepted the Phase 1 local policy
  bundle in conversation: JWT 10 minutes, refresh 30 days, verification 24 hours,
  reset 30 minutes, password minimum 10 characters, five failed logins trigger
  a 15-minute temporary lock; verified accounts for onboarding/staff. Owner
  manages membership and payout, members have explicit permissions. Application
  requires legal name, contact, address and one PDF/PNG/JPEG document <= 5 MB;
  pending review is read-only, rejected applications can be edited/resubmitted.
  Admin suspension/restoration requires a reason; suspension blocks ordinary
  mutations and staff access. New payout versions require Admin review. A minimal
  Event Commerce draft boundary is authorized for scoped event staff assignment.
  These are local Phase 1 decisions, not production-release authorization.
- Authority for current-account and tenant guards remains BRS IDN-R02/03/04
  and ORG-R03/08. Native JUnit/MockMvc tests cover allowed and forbidden paths;
  no hook, CI or external branch-protection changes are authorized.

- 2026-11-19: Use Gradle multi-project for backend; each Spring Boot service
  remains independently buildable and deployable.
- 2026-11-19: Local-only dependencies are PostgreSQL, Kafka, Schema Registry,
  Redis, MinIO and MailHog. Kubernetes, observability and environment overlays
  are deferred.

## Validation

- Focused proof: Gradle compilation/tests, Spring MVC/security tests, Angular
  build/tests and migration checks.
- Integration or end-to-end proof: local Docker dependencies plus register →
  verify → login → organizer application → Admin approval.
- Repository-required checks: whitespace, Markdown/code fence checks for plan
  updates and any native project validation available.

## Result

Completed and validated locally on 2026-11-20: all AUTH-01–11/ORG-01–09 and
four P1 UI/UX groups, including the accepted verification resend recovery.
The final Phase 1 browser regression after Phase 2 integration still passes
88 checks without browser exceptions or server 5xx errors. Implementation,
proof and limitations are recorded in the completion checkpoint at the top.
Phase 2 continues in its own execution plan; no push or deployment performed.
