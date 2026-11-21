# QR and card checkout with database-controlled outcomes

Date: 2026-11-20

## Status
Completed — 2026-11-20

## Outcome and authority
Owner explicitly requests a customer checkout resembling normal payment, without
VNPAY integration: QR with a lifetime/countdown and automatic result after 3–5
seconds; cards checked against fixed configured numbers; database keys such as
QR_PAYMENT select success/failure and JSON failure reasons. This supersedes the
old customer-facing simulation labels in decision 0002. Existing ten-minute
holds, backend-authoritative receipt processing and admission remain unchanged.

## Approach
Extend existing Payment provider sessions with method, expiry, masked card suffix
and a snapshot of the configured outcome. Seed JSONB configuration and expose
authenticated owner-only method selection/session reads. Existing worker delivers
signed outcomes even after navigation/reload. Customer selects QR or card; only
backend state drives success. Retain scenario tooling for admin-only regression.
Extend current ETP controls, palette and purchase layout (Operate mode).
Settled brief and existing authorization are sufficient; no new design round.

## Recovery
Existing application files are untracked user work. Snapshot of affected source
is at /tmp/etp-payment-methods-baseline. Additive migration only; no data reset.
Use synthetic accounts in the existing owned validation stack. Restore temporary
configuration overrides after each proof. Do not push/deploy or touch real orders.

## Progress
- [x] Inspect behavior, authority, current payment boundary and validation.
- [x] Database configuration, session API and durable scheduled outcomes.
- [x] Customer method/QR/card UI, copy and recovery states.
- [x] Contract/docs and focused backend/frontend tests.
- [x] Real browser/database-change proof, responsive review and regression.

## Decisions
- QR default delay 4 seconds; validity capped by original order hold. Configuration
  is captured at method submission, so SQL changes affect new sessions only.
- Only configured sample numbers are accepted; no expiry/CVV collection or PAN
  persistence. The session stores only the last four digits.
- Real bank settlement/transfer is outside this local provider. QR payload is an
  opaque local session reference; it contains no bank destination or personal data.

## Validation and result
Validated with repository Gradle/Testcontainers, Angular tests/builds and real
Playwright workflows. Desktop/mobile, expiry/failure/retry and SQL changes passed.

## Implementation evidence
Additive V2 migration seeds `payment.mock_config`, method deadlines and outcome
snapshots. GET/POST owner scope, ADMIN-only scenario tools, durable worker receipts,
QR decoding/reload idempotency, configured failure reasons and masked card input
are covered by 14 new Testcontainers tests. Full Gradle test/bootJar passed.
Angular has 133 passing tests including 12 additional method/API checks. Four
production builds pass; live UI/database proof has passed.

## UI direction and review inputs
Operate-mode extension, code-led within the established ETP system. THESIS: choose
a payment method and follow its current state without technical scenario controls.
OWN-WORLD: existing indigo, cream/lilac context, Be Vietnam Pro and supplied QR/card
icons. STORY: method choice → QR/countdown or card input → authoritative result →
tickets/retry. FIRST VIEWPORT: method/state panel leads, amount/order summary stays
adjacent on desktop and stacks on mobile. FORM: existing purchase grid and control
hierarchy, one restrained pending pulse with reduced-motion fallback.
No visual-world replacement or concept tournament: user specified this narrow flow.
Impeccable context was already loaded earlier in this session. Subagent tools are
unavailable; final review was performed in-thread and is disclosed below.

## Final proof
- `./gradlew test bootJar`: success; native reports total 132 tests with no
  failures/errors (unchanged tasks reused Gradle results). New checkout cases: 14.
- `npm run test:unit`: 133 pass. New checkout component: 97.01% statements, 92.3%
  branches, 100% functions/lines. Overall frontend line coverage 89.04%.
- All four production builds pass under existing budgets; none relaxed.
- 241 browser checks: database-driven methods 66, visual/keyboard/reduced-motion
  follow-up 11, Phase 3 transactions 80, edge cases 8 and ticket fulfillment 76.
  Reports: `output/playwright/{payment-methods,payment-visual-review,phase3,phase3-edge,phase4}-result.json`.
- SQL string failed and JSON errors work without restart; in-flight outcome and
  deadline snapshots remain stable. QR lifetime defeats a later success, reload
  preserves QR/deadline, card failures can retry via QR within the original hold.
- Both configuration keys and original timestamps were compared to the saved
  backup after proof and match exactly. Gateway/Payment/Notification health UP;
  customer preview returns HTTP 200.
- Source detector: no findings (`/tmp/etp-payment-detector.json`); source-only scan
  is limited, supplemented by real renders. Three new text/surface pairs have
  contrast 6.25, 9.19 and 7.70:1.
- Reviewed 17 captures at 320/390/768/1440px in `output/playwright/payment-methods-*.png`;
  contact sheets `payment-review-sheet-{1,2,3}.png`. No clipping or horizontal overflow.

An initial legacy late-payment proof raced Payment's asynchronous exception
projection after Commerce already reported EXCEPTION. The test now waits for the
actual LATE_SUCCESS exception before asking the UI to refresh; the assertion and
application behavior remain unchanged. Fresh boundary proof passed.

## Impeccable finish review (degraded, same context)
Disposition: ship (scope: this checkout extension and the identified fixes).
No independent reviewer tools are available. Repository product/design documents
and existing UI are authority; this extension did not create a new visual world.

### persistence
Pass: decision 0004, this plan, API contract/generated types and the operator guide
record the authorized behavior. Existing design tokens and supplied assets retained.
### fidelity
Method selection, live QR/countdown, card input, error reasons and retry match the
explicit owner brief. Desktop summary sits alongside the method; mobile stacks.
Backend-only outcomes and the no-VNPAY requirement match implementation/proof.
### ceiling
Appropriate for an Operate surface; QR is a real decodable local reference. No
external bank application or physical-device verification is claimed.
### material_fixes
Clear. Card-label/focus spacing and failed-payment icon/color were corrected and
recaptured. Initial loading-transition capture was replaced by settled primary
action evidence; source behavior unchanged.
### keep
Preserve original hold deadlines, native form semantics, backend-authoritative
confirmation, readable failure recovery and configuration snapshots.

## Result and limits
Available on the existing local preview through event booking → payment. Normal
customer language also applies to confirmation/ticket emails. The provider remains
a configurable local simulator with no external settlement. No CVV/expiry collection
or full submitted-card persistence. Settings guide: `docs/payment-test-configuration.md`.
Validation uses Chromium viewport emulation and synthetic accounts/cards; it is not
a full accessibility certification or physical bank/camera integration test. Existing
preview remains running; no push/deploy and no real user orders were modified.
