# Execution Plan: Adopt MVP UI mockups

Date: 2026-11-20

## Status

Completed — 2026-11-20

## Outcome

Apply the supplied MVP visual system and layouts to every existing customer,
organizer, administrator and check-in function, preserving live API behavior.

## Context

- User authority: `docs/Event_Ticketing_Platform_MVP_UI_Mockups_v1.0.0.pdf` and
  `docs/event-ticketing-assets/`; user explicitly selected existing functionality.
- UX and business authority: `docs/03-ui-ux-design.md`, existing Phase 1–4 decisions.
- Runtime and proof: `docs/local-development.md`, current frontend/browser suites.

## Scope

Shared tokens/assets/navigation/authentication; discovery/home/detail; ticket
selection, checkout, orders and tickets; organizer authoring/membership/settings;
admin moderation/payments; mobile check-in. Missing backend features such as
revenue analytics, settlements, fee configuration and system audit/settings are
excluded by the user's explicit scope choice. No publishing or deployment.

## Approach

1. Inspect all 51 reference screens, existing routes and behavior.
2. Integrate supplied plain artwork/icons and shared responsive shell.
3. Apply the reference's component and page composition to existing functions.
4. Build all four apps, run existing regression suites, inspect desktop/mobile
   screenshots and correct observed issues.

## Risks And Recovery

- Existing frontend is untracked user work. Baseline copied to
  `/tmp/etp-ui-baseline`; no unrelated repository cleanup or resets.
- Mockup sample names, metrics, QR and unsupported controls cannot become live data.
- Preserve server authorization, password policy, hold/payment/admission behavior,
  draft protection and consequential-action confirmations.
- Use only isolated validation stack/accounts under the documented runbook.

## Progress

- [x] Inspect mockups, assets, app routes and existing implementation.
- [x] Confirm scope: existing functionality across all four apps.
- [x] Shared visual system, assets and shells.
- [x] Customer and shared authentication layouts.
- [x] Organizer, admin and check-in layouts.
- [x] Builds, regression and rendered validation.

## Decisions

- 2026-11-20: Supplied reference is visual authority. Existing business rules win
  over demonstrative values (e.g. password length and verified ticket QR).
- 2026-11-20: Retain supported route contracts and real data; no dummy dashboard
  numbers or nonfunctional menu items for unavailable backend features.
- 2026-11-20: One repository plan is the durable record; no separate design
  orchestration state, as required by AGENTS.md.

## Validation

- Four production builds pass with initial bundles below the existing 500 kB
  warning threshold; no budget was relaxed.
- 121 unit tests pass. Added home empty/loading/error/live-identity and uploaded
  media/fallback checks; adapted shell expectations for public navigation.
- Phase 1: 88 real browser checks pass; Phase 2: 65 pass.
- Phase 3: 80 pass, including payment outcomes, holds, cancellation, reconciliation,
  keyboard quantity controls and confirmed-order filtering.
- Phase 3 boundary: 8 pass. Phase 4: 76 pass, including actual QR decoder, PDF,
  duplicate/replayed check-in, manual confirmation and permission/cancellation.
- Additional UI proof: 64 pass across 1440, 768, 390 and 320 px. Verified real
  category/filter navigation, profile drafts across tabs, mobile portal menus and
  all new public/auth/account/dashboard layouts without page overflow.
- Final screenshots inspected for customer home/auth/profile, organizer/admin
  dashboard, booking/payment, ticket detail, scanner and check-in results.
  Fixed mobile auth word spacing, oversized single-image galleries and a tablet
  overflow caused by an absolutely positioned accessible table heading.
- All 34 referenced icon names exist in all three supplied palettes. Local PNG
  and font responses have correct MIME types. No console exceptions in UI proof.
- Browser tests adapted for the profile security tab; simulator checks scope to
  payment controls and keyboard checks explicitly traverse the stepper buttons.
  Existing business assertions remain; order-filter assertion was strengthened.
- Local static server now serves PNG/JPEG/TTF with their correct MIME types.


## Result

Complete for the user-selected existing Phase 1–4 functionality. All four apps
use the supplied visual language, assets and responsive navigation, while live
API data, authorization, consequential confirmations and QR issuance remain.
Four production builds, 121 unit tests and 381 browser checks passed. No bundle
budget was relaxed, and no deployment, push or backend feature expansion occurred.

Evidence: `output/playwright/phase1-result.json`, `phase2-result.json`,
`phase3-result.json`, `phase3-edge-result.json`, `phase4-result.json`, and
`mockup-ui-result.json`; screenshots are alongside these reports. Reproduce the
new layout proof with `python3 scripts/run-mockup-ui-browser.py` after the existing
Phase 1 fixture and isolated stack are ready. The unchanged backend stack and the
rebuilt frontend preview are left available on their existing local ports.

Limits: browser proof uses Chromium viewport emulation and a real decoder fed
synthetic video containing valid backend QR pixels. Physical phones/cameras and
other browser engines were not tested. Current local event artwork/names include
existing synthetic test uploads; the UI correctly retains those uploaded assets.
Unsupported mockup business modules remain outside the agreed scope.

Tooling note: native beforeunload interrupts Playwright CLI result reporting.
The new read-only proof leaves a dirty profile through the existing guarded router
link, with the synthetic confirmation handled as in the Phase 1 suite. It also
waits for the lazy route before asserting that the mobile menu has closed.
