# UI full-path review and corrections

## Status
Completed — 2026-11-21

## Outcome and authority
User requests Impeccable + Playwright review of all UI and fixes for misaligned
password eyes, missing organization navigation on reports/settlements, technical
local/simulator copy, raw audit enums/dates, and redundant reload buttons.
Preserve existing role/finance policies and user UAT data. No external deployment.

## Approach
- Reproduce rendered failures, including form errors, at desktop/mobile sizes.
- Fix shared CSS/component causes; share organization navigation across routes.
- Translate known audit actions/details/sources and display readable local times.
- Remove normal reload controls; update on route entry, successful actions and
  return to the page. Retain explicit error recovery and transaction reconciliation.
  Never refresh over unsaved input or a pending check-in decision.
- Review all workspace surfaces and states, then validate meaningful geometry,
  navigation, copy, refresh behavior and affected business flows in real browsers.

## Progress
- [x] Read workflow and Impeccable/Playwright guidance; inspect source and reports.
- [x] Capture before-state failures and audit the rendered UI.
- [x] Implement shared fixes, navigation, copy, history and refresh behavior.
- [x] Run unit/build and browser regression; inspect final screenshots.
- [x] Record evidence/limits and complete this plan.

## Risks and recovery
Most source is untracked, so preserve a source snapshot before editing. Use only
existing synthetic QA identities for reads and fresh QA records for mutations.
Do not reset data or alter payment/authorization rules. Stop only owned browsers;
leave the local application available to the user after validation.

## Evidence
- Password eye baseline: 19px center error; corrected to 0px. Normal/error
  states verified across four portals, four widths and 200% font size.
- Frontend unit: 162 passed; all four production builds passed.
- Phase 1/2/3/4/5/6 browser: 88/65/80/76/87/259 assertions passed.
- Existing UI review: 128 passed. New full-path UI review: 869 passed over
  48 surfaces; additional application-detail/category-dialog inspection: 13.
  Total browser assertions: 1,665. No unexpected JavaScript/5xx errors.
- Actual screenshots reviewed; shared navigation survives report/settlement
  transitions and deep reload. Enum and timestamp labels are readable.
- Dirty-form return, contextual retry and lost settlement acknowledgement
  exercised. Configuration restoration read back: 5% fee, 24h reminder.
- Ten service health endpoints UP; local application left available.
- Impeccable source detector: no findings. This does not replace render proof.
- Source fingerprints match the final tested build; no application edits after
  validation. Browser workflow syntax checks passed.
- Known limits: emulated Chromium viewports, no physical touch/Safari/Firefox;
  existing component CSS budget warnings (checkout 2.53kB, operations 2.23kB).
  Backend product source was not changed or retested in this UI task.
- Report: `docs/qa/ui-polish-full-path-review-2026-11-21.md`.
  Artifacts: `output/playwright/ui-polish/`; protected logs: `.runtime/ui-polish/`.

## Recovery and handoff
The before-source archive remains in the protected task runtime directory.
User UAT records were not reset. Synthetic QA records and immutable evidence are
retained. Only owned Playwright sessions are closed; application services stay up.
No push, external deployment, hooks or CI changes.
