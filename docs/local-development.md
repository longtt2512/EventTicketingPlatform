# Local application and Phase 1–4 validation

Java 21, Node 20, npm, Docker Compose and the checked-in Gradle wrapper are
required. Run commands from the repository root unless a directory is shown.

## Isolation and ownership

The existing `infra/local/docker-compose.yml` project and its data are user
owned. Phase 1 validation uses the separate `event-ticketing-validation` Compose
project, ports 15432/11025/18025/19000/19001/16379, backend ports 18080–18087,
and browser port 14200 (Customer `/`, Organizer `/org/`, Admin `/admin/`, Staff `/staff/`). Only stop processes recorded by this validation
run. Do not run `down` on the existing local project or delete its volumes.

Validation accounts use unique `@example.test` email addresses. Never use real
accounts, financial information or identity documents in automated tests.
Validation-only admin credentials are generated into ignored `.runtime/` files;
they are not production credentials and must not be committed.

## Build and test

```sh
cd backend
./gradlew test bootJar
```

```sh
cd frontend
npm ci
npx ng build customer-web
npx ng build organizer-web
npx ng build admin-web
npx ng build checkin-web
```

Start the isolated dependencies using:

```sh
docker compose -f infra/validation/docker-compose.yml up -d --wait
python3 scripts/run-validation.py start
```

The launcher generates the local encryption key and admin credentials, records
owned PIDs, and checks readiness. Use `python3 scripts/run-validation.py restart`
after rebuilding, or `stop` to stop only its processes. It refuses occupied
unowned ports. `status` reports process ownership without exposing credentials.

Services use `DB_URL=jdbc:postgresql://localhost:15432/event_ticketing`,
the owning role from `infra/local/postgres/init/01-roles.sql`, and their explicit
ports. Identity mail uses port 11025, and `WEB_BASE_URL=http://localhost:14200`.
Gateway routes must target these isolated service ports. The four built Angular
apps are served by one listener with `node scripts/serve-frontends.mjs`:
Customer `/`, Organizer `/org/`, Admin `/admin/`, Staff `/staff/`. Angular build
base hrefs select the matching context. It proxies `/api`
to the isolated gateway without exposing internal service routes.

Require `UP` from each `/actuator/health` before browser tests. MailHog is at
`http://localhost:18025`; only inspect email for the current test accounts.
Playwright evidence belongs in `output/playwright/`. The active execution plan
records observed validation results and any remaining gaps.

Readiness alone is not evidence that a business flow passes. Use the executable
workflows below for business-flow validation.

## Phase 2: event authoring, inventory and public discovery

The verified validation stack now also owns Kafka on `19092` and Discovery on
`18084`. The launcher already supplies their service URLs. For an existing
validation PostgreSQL container created before Phase 2, provision the new isolated
role/schema once (idempotent; does not delete data):

```sh
docker compose -f infra/validation/docker-compose.yml exec -T postgres psql -U postgres -d event_ticketing < infra/local/postgres/init/03-discovery.sql
```

Build with `./gradlew test bootJar` in `backend` and `npm run build:all` in
`frontend`. Start dependencies and restart owned processes using the commands
above. Kafka's health check uses its internal listener; host services use 19092.

- Customer discovery: `http://localhost:14200/events` (guest access).
- Organizer: `http://localhost:14200/org/organizations`, choose an organization,
  open **Sự kiện và phân công**, create a draft and select **Quản lý sự kiện**.
- Admin review: `http://localhost:14200/admin/events` with the generated validation
  administrator in `.runtime/phase1/credentials.json`.
- `npm run test:unit` runs shared frontend regression tests.
- `python3 scripts/run-phase1-browser.py` provisions synthetic AUTH/ORG fixtures
  and runs the full Phase 1 regression, including a real 60-second resend wait.
- `python3 scripts/run-phase2-browser.py` reuses that synthetic organizer and
  validates create/upload/tickets/review/publish/discovery/pause/cancel. It leaves
  clearly named cancelled test events and never publishes a site or deployment.
- Structured proof and responsive captures: `output/playwright/phase1-result.json`,
  `output/playwright/phase2-result.json`, `output/playwright/phase2-*.png`.

Media uses private MinIO staging objects, signed PUT, server format/size/SHA-256
validation, then a separate immutable available key. The local accepted policy
permits this validation; a production malware scanner and storage lifecycle
configuration remain deployment prerequisites. No production-readiness claim.

Commerce writes an outbox in the event transaction. Kafka outages retain pending
rows and retry them; Discovery commits inbox/version checks with the projection
before acknowledging. Source update times appear in public details. DLT topics
retain poison messages for operator inspection; do not discard or blindly replay
unvalidated records. The schema contracts and topic ownership are documented in
`backend/event-contracts/README.md`.

Attendee lists query Discovery's authorized read model, populated only by Ticket
& Admission facts. Phase 4 now populates one record per issued ticket and applies
check-in/void facts. Pending issuance never counts as an issued attendee. Phase 2
introduced allocation primitives; Phase 3 adds public checkout below.

## Phase 3: booking and local payment simulation

Decision `docs/decisions/0002-local-phase3-policy.md` authorizes local mock payment.
Commerce owns orders and holds; Payment owns receipts/reconciliation in a separate
schema; Notification owns confirmation email delivery. Only the gateway provider
is simulated. No actual charge, transfer or refund is involved. Only configured sample card numbers are accepted; submitted full numbers are not persisted. Phase 4 consumes
confirmed orders to issue real local QR/PDF artifacts.

For an existing validation PostgreSQL instance, provision the additive schemas:

```sh
docker compose -f infra/validation/docker-compose.yml exec -T postgres psql -U postgres -d event_ticketing < infra/local/postgres/init/04-payment.sql
docker compose -f infra/validation/docker-compose.yml exec -T postgres psql -U postgres -d event_ticketing < infra/local/postgres/init/05-notification.sql
```

Run `./gradlew test bootJar` in `backend`, then `npm run test:unit` and
`npm run build:all` in `frontend`. After the build succeeds, restart the owned
services with `python3 scripts/run-validation.py restart`. Payment uses `18085`,
Notification `18086`. Outside the isolated launcher, Payment defaults to `8087`
(`8085` is already assigned to local Schema Registry) and Notification to `8086`.
The launcher generates and stores a local-only mock signing
secret in protected `.runtime/phase1/credentials.json`, enabling the simulator.
Outside the launcher, `LOCAL_PAYMENT_MOCK_ENABLED` defaults to false; enabling it
also requires `LOCAL_PAYMENT_MOCK_SECRET` of at least 32 characters. Never set
this simulator configuration for a real payment deployment.

- Customer: event detail → **Chọn vé** → hold/checkout → free confirmation or
  **Thanh toán** → choose QR or card → authoritative result.
- `/account/orders` shows the current customer's history; login preserves the
  original selection or checkout destination. Holds last ten minutes by server
  time; loading, payment retry and reload do not reset them.
- Customer methods/configuration: see [QR/card configuration](payment-test-configuration.md).
  QR defaults to an automatic result after four seconds; SQL controls failures and
  deadlines. Card outcomes use a configured sample allowlist.
- Admin-only regression scenarios: success, failure, unknown (choose a final outcome later), 15-second
  delayed success, duplicate callbacks, success after hold expiry, wrong amount,
  and lost callback recovered by query/reconciliation. Unknown does not mean failed.
- Admin `/payments` and `/payment-exceptions`: query/reconcile, review a case,
  and confirm evidence of a simulated return. Enter `MOCK-RETURN-...`, amount,
  currency/date/evidence. Partial return retains the case; actual total received
  is preserved. This performs no real money movement.
- Organizer: **Xem đơn hàng của sự kiện** opens a scoped read-only order list.
- Confirmed paid/free orders enqueue an email in Notification and deliver to
  MailHog. SMTP outages retain the obligation; no order rollback. SMTP can deliver
  duplicates after a crash between sending and recording success.
- Confirmed-order events carry immutable item identities as the durable Phase 4
  issuance obligation. Ticket processing cannot reconfirm an order or allocate
  inventory again.

Synthetic browser proof setup (requires the existing Phase 1 synthetic owner):

```sh
python3 scripts/prepare-phase3-fixture.py
python3 scripts/run-phase3-browser.py
```

The setup creates a clearly named local test event using real application APIs.
Only `.runtime/phase1/phase3-fixtures.json` contains synthetic credentials (0600).
Reports and screenshots are written under `output/playwright/phase3-*`. Browser
result logs may include generated fixture code: keep them protected, never paste
the raw log or commit it. Request contracts: `backend/openapi/phase3.json`; generated
frontend types: `frontend/libs/api-client/src/phase3.generated.ts`.

Additional boundary proof:

```sh
python3 scripts/prepare-phase3-edge-fixture.py
python3 scripts/run-phase3-edge-browser.py
```

The boundary fixture creates fresh synthetic orders through application APIs,
then accelerates only those orders' stored hold deadlines in the isolated
validation database. Application clocks and the ten-minute policy stay unchanged.
It proves expired free-order rejection, real late callbacks, a real 15-second
provider delay and lost-callback recovery. The main workflow separately verifies
that normal holds are ten minutes and reload/retry do not extend them.

Phase 3 baseline validation on 2026-11-20: 94 backend tests, 91 frontend unit tests, four production
builds, 88 Phase 1 + 65 Phase 2 + 77 Phase 3 + 8 boundary browser checks. Native
backend reports are in each service's `build/reports/tests/test/`; browser reports
are `output/playwright/phase{1,2,3}-result.json` and `phase3-edge-result.json`.
Responsive captures cover 390/768/1440px, plus targeted 320px, keyboard and
200% text-scaling checks for purchase. A full browser zoom/screen-reader audit is
not included. Impeccable's source scan checked 12
TypeScript/SCSS files using regex only; it did not audit rendered contrast or
establish WCAG conformance. Visual inspection supplements that limited scan.


## Phase 4: QR/PDF tickets and online admission

Accepted policy: `docs/decisions/0003-local-phase4-policy.md`. Admission is allowed
from two hours before event start until event end (end excluded), using server
time. Ticket holder details use the buyer name. Current active account,
organization, membership and event assignment are checked at the service boundary.

For an existing isolated PostgreSQL instance, add the dedicated schema/role:

```sh
docker compose -f infra/validation/docker-compose.yml exec -T postgres psql -U postgres -d event_ticketing < infra/local/postgres/init/06-ticket-admission.sql
```

Ticket & Admission defaults to `8088`; the isolated launcher uses `18087` and sets
`ADMISSION_SERVICE_URL` for Commerce/Gateway. `DATA_ENCRYPTION_KEY` is required:
the launcher reuses its protected local key. Keep that key when restarting; QR
credentials are encrypted under it. Run launcher mutations sequentially because
they share the owned PID file. Stop owned frontends before rebuilding their assets,
then start them again after all four builds finish. Backend processes use runtime
jar copies, so a build does not alter a running process.

```sh
python3 scripts/run-validation.py stop --services frontends
npm --prefix frontend run test:unit
npm --prefix frontend run build:all
./backend/gradlew -p backend test bootJar
python3 scripts/run-validation.py restart
python3 scripts/run-phase4-browser.py
```

The browser runner creates a fresh synthetic event in the current check-in window
from the existing Phase 1 owner and staff, then runs the real free/paid order,
issuance, PDF download, camera decoder, manual lookup/confirmation, duplicate,
lost-response recovery, attendee/statistics, permission and cancellation flows.
Its video feed contains actual generated QR pixels (`canvas.captureStream`), so it
exercises the shipped decoder; this does not test a physical camera or mobile
browser hardware. Run Phase 1 first if no synthetic account fixture exists.

- Customer `/account/tickets`: paged list without QR; owner-only detail and PDF.
  Confirmed order pages show durable issuance progress and an order-scoped link.
- Staff `http://localhost:14200/staff/check-in/events`: assigned events, camera, exact-ID
  lookup with explicit confirmation, paged history and unique admission counts.
- Organizer: event **Xem lại → Người tham dự** displays actual projected tickets;
  **Thống kê check-in** opens the authorized live dashboard.
- A lost scan response is unknown. Keep the screen open and use **Kiểm tra lại
  kết quả** to replay the same request key; a replay is visually distinct from a
  fresh successful admission. There is no offline admission queue.
- Cancellation first persists a Commerce request that blocks allocation, then
  freezes Admission and voids unused tickets before completing cancellation.
  A service outage retains the request for reconciliation; never clear its flag
  manually. Checked-in history remains and no sold stock is restored.
- MinIO bucket `private-ticket-artifacts` is private. The authenticated service
  streams freshly authorized PDFs; no public/presigned artifact URL is exposed.
  Rendering/storage failure does not rotate QR, recreate tickets or alter orders.
  Used, voided or expired ticket PDFs omit the QR. Long content wraps and moves
  the QR to a second page. The embedded Be Vietnam Pro font includes its OFL.
- Notification delivers authenticated order/ticket links through MailHog, without
  QR credentials or PDF attachments. Failed issuance, outbox or email obligations
  retry independently. SMTP can duplicate a message across a send/commit crash.

Contracts: `backend/openapi/phase4.json`, generated frontend models in
`frontend/libs/api-client/src/phase4.generated.ts`, and
`backend/event-contracts/README.md`. Reports/captures are
`output/playwright/phase4-result.json`, `phase4-*.png` and the three ticket PDFs.
The PDF layout test also writes `backend/services/ticket-admission-service/build/qa/`.
Only ignored protected fixture files contain credentials; never commit raw browser
result logs. Local checks do not establish production HA, key rotation, retention,
physical-device compatibility, CI enforcement or WCAG conformance.

Observed Phase 4 native validation (2026-11-20): 121 backend tests and 117 frontend
unit tests pass; all four production builds pass. Frontend coverage: 87.18% lines,
81.05% statements, 63.01% branches and 79.45% functions. Browser regression passes
88 Phase 1, 65 Phase 2, 77 Phase 3 and 8 transaction boundary checks. The fresh
Phase 4 workflow passes 76 checks, for 314 browser checks across Phases 1–4.
Detailed outcome and limits are recorded in
`docs/plans/completed/phase-4-ticket-fulfillment.md`.


## Phase 5: local finance, reporting and platform operations

Accepted policy: [decision 0005](decisions/0005-local-phase5-policy.md). This is
local evidence recording; no bank transfer or production readiness is implied.
Existing orders and payment evidence are retained. Orders without a fee snapshot
remain visible with unknown fees/net and are not payable through settlement.

For an existing isolated database, create the two new roles/schemas before startup:

```sh
docker compose -f infra/validation/docker-compose.yml exec -T postgres psql -U postgres -d event_ticketing < infra/local/postgres/init/07-finance.sql
docker compose -f infra/validation/docker-compose.yml exec -T postgres psql -U postgres -d event_ticketing < infra/local/postgres/init/08-admin-audit.sql
./backend/gradlew -p backend test bootJar
python3 scripts/run-validation.py stop --services frontends
npm --prefix frontend run test:unit
npm --prefix frontend run build:all
python3 scripts/run-validation.py restart
python3 scripts/run-phase5-browser.py
python3 scripts/verify-phase5-unmapped.py
```

Finance defaults to `8089` (`18088` in validation), Admin Operations to `8090`
(`18089`). The launcher supplies their service URLs, the existing internal token,
Kafka and SMTP addresses, and the Organizer portal URL. Each service migrates only
its own schema. Run launcher mutations sequentially. Keep the protected credential
and encryption-key files; never reset volumes to make a migration or test pass.

The Phase 5 browser runner needs a successfully completed Phase 1 synthetic owner,
staff and verified payout fixture. It creates three fresh synthetic events, buys
paid/free orders, changes future fees, downloads the real report CSV, requests
paid/zero settlements, rehearses UNKNOWN → definite NOT_PAID → retry, and loses a
settle response after server persistence. It also exercises category deactivation,
settings and consolidated audit. It advances only those named synthetic event
start/end times in the isolated database so the real lifecycle can reach COMPLETED;
there is no public clock or fixture endpoint. Fee/settings values are restored
through APIs in `finally`; audit, orders, claims and evidence are retained. A failed
run can leave its synthetic batch pending for inspection; do not delete its claims.

Entry points:

- Organizer organization detail → **Báo cáo doanh thu** / **Đối soát sự kiện**,
  available to the current owner or FINANCE member.
- Admin `/home` and `/analytics`: full-scope server totals, date/timezone filters,
  source freshness, current inventory, payment/attendance rates, and snapshot CSV.
- Admin `/commission`: future percentage or per-ticket integer VND configuration,
  with optimistic version, reason and confirmation. Held orders retain old quotes.
- Admin `/settlements`: current eligibility, immutable destination version,
  explicit simulated evidence, safe retry and audit history.
- Admin `/categories`, `/orders`, `/audit-logs`, `/settings`: managed selection
  catalog, existing order detail, read-only consolidated audit, and approved keys.

Recovery and interpretation:

- Missing fee/reconciliation, unresolved exception, partial-return obligation,
  suspended organization, changed/unverified payout or incomplete event blocks a
  new payout. Read the eligibility reasons; never replace unknown amounts with zero.
- A full return of the applied sale reverses its platform fee. Returns of extra or
  late collections do not reverse that sale's fee. Original order snapshots stay
  immutable. Cancelled events cannot use ordinary settlement.
- Use one batch per event. UNKNOWN is still an investigation, not permission to
  pay again. Mark FAILED only with definite non-payment evidence and retry that
  same batch. A verified destination can refresh only before transfer or after
  definite non-payment; refresh records a new snapshot and audit.
- After evidence is saved, **Kiểm tra lại cùng bằng chứng** and the recovery worker
  replay that exact decision. A dependency timeout retains it. If current state
  changed before authorization, it remains pending for investigation; never clear
  the decision, change the reference or create a replacement batch in SQL.
- PAID requires `MOCK-SETTLEMENT-…`, the exact net amount, evidence and a valid time.
  Zero net closes with NO_TRANSFER and no transfer reference. A settled batch is
  immutable in the UI and its unique event/order claims prevent a second payout.
- Unmatched local collections/returns also feed a global ledger. They appear in
  platform cash and open obligations, without inventing event revenue or assigning
  them to an Organizer. The dedicated proof returns its synthetic 100 VND in full
  through the Admin API and retains both collection and return evidence.
- Reports filter `[from,to)` in the selected timezone. Sales use confirmed time,
  cash/returns their evidence times, settlement its recorded completion time, and
  check-in its recorded admission time. Inventory/rates are current projected
  state. Export uses the displayed frozen snapshot, with current permission checks.
- Audit/source sync and report backfill are eventual. Inspect source timestamps and
  errors if a fact has not arrived. Retry/replay the owner feed or existing outbox;
  do not invent historical actors/details or edit the consolidated audit record.
- Operational mail appears in MailHog (`http://localhost:18025`). One reminder
  obligation exists per event/recipient, including late buyers within the window.
  Cancellation, voiding, event start and current settings are rechecked before
  sending. SMTP is at-least-once across a send/commit crash, not exactly-once.

API contract: `backend/openapi/phase5.json`; generated client types:
`frontend/libs/api-client/src/phase5.generated.ts`. Browser artifacts:
`output/playwright/phase5-result.json`, `phase5-sales.csv`, and `phase5-*.png`.
Protected `.runtime/phase1` scripts/logs contain synthetic credentials and must not
be committed or copied into reports. Production HA, retention, bank integration,
SLOs, legal compliance and CI/branch-protection enforcement are not established by
these local commands.


Observed Phase 5 validation (2026-11-20): 182 backend tests and 146 frontend tests
pass; all four production builds pass. Fresh browser workflows total 487 checks
(Phases 1–5, transaction boundaries, QR/card configuration and final render), plus
four real unmatched-receipt/report/return checks. The downloaded CSV contains one
snapshot, two orders, 200000 VND gross, 12000 VND platform fees and 188000 VND net.
Fee and operational configuration values were restored. Details and limitations:
[completed Phase 5 plan](plans/completed/phase-5-platform-operations.md).

## Portal/UI remediation regression

After the documented phase fixtures, use `python3 scripts/run-portal-remediation-browser.py`
for synthetic registration, email and reset destinations, all password controls,
category state dialogs, four simultaneous portal tabs and deep-link checks.
Evidence: `output/playwright/portal-remediation/result.json`. It creates QA accounts
and categories; it never resets existing data. Then run
`python3 scripts/run-portal-review-browser.py` to submit/approve a synthetic
long-content organization application and verify responsive layout, all seven
organization navigation controls, and return navigation. Run the first command
again before a new complete review cycle to provision a fresh applicant.

The complete results, screenshots and limitations are in the
[2026-11-21 remediation QA report](qa/portal-routing-auth-and-ui-remediation-2026-11-21.md).
Four apps share one refresh cookie; use separate browser profiles when testing
four different accounts. Multiple tabs for the same account are supported.

## Full-path UI polish regression

After building all four frontends, use the existing synthetic Phase 1 fixtures
(created by `python3 scripts/run-phase1-browser.py`) and the running validation
stack:

```sh
python3 scripts/run-ui-polish-browser.py
```

This Playwright CLI workflow checks password visibility controls in normal and
invalid states across all four portals, including 200% text scaling; organization
navigation on revenue/settlement deep links; translated activity history;
automatic refresh that preserves unsaved input; contextual network recovery; and
responsive layout/copy across the workspaces. It does not reset the database.
Reports and desktop/mobile captures: `output/playwright/ui-polish/`. Protected
browser logs containing fixture credentials stay under `.runtime/ui-polish/`.

Ordinary reload controls have been removed. Returning to an unchanged page,
reconnecting, entering a route, or completing an action refreshes its data.
Dirty forms, open confirmations, pending admission decisions and expanded
pagination are preserved. Payment/issuance progress continues polling. Explicit
retry remains available for failed reads and uncertain transaction outcomes.
