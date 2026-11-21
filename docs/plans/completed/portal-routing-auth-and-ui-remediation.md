# Execution Plan: Portal routing, authentication and UI remediation

Date: 2026-11-21

## Status

Completed — 2026-11-21

## Outcome

Implement all nine user-approved backlog items: UI-01–05, ROUTE-01,
AUTH-01–02 and QA-01. Serve Customer `/`, Organizer `/org/`, Admin `/admin/`
and Staff `/staff/` from localhost:14200; preserve portal in account emails;
clarify onboarding; repair admin/organizer layouts and category actions; provide
password visibility on every password input. Prove behavior with tests and browsers.

## Context and authority

- User approved the nine-item backlog and requested implementation and full retest.
- `docs/WORKFLOW.md`, `docs/local-development.md` own workflow/runtime.
- `docs/02-business-rules.md` IDN-R03, ORG-R03/08: roles granted by server,
  organization membership and event assignment scope. Portal context grants no role.
- Existing Angular standalone apps, shared auth/UI libraries, identity mail outbox.
- Preserve user's ongoing UAT data; synthetic QA accounts/events only.

## Scope and approach

1. One local frontend listener with app base paths; update generated links/runtime/docs.
2. Allowlisted portal context for registration, resend and password reset email;
   preserve safe in-portal return URL; onboarding and cross-workspace navigation.
3. Shared password visibility control, category reason dialog, consistent spacing/tabs.
4. Focused unit/integration tests, all frontend builds, full backend suite and existing
   browser regression flows on dedicated fixtures, plus visual checks across viewports.
5. Update UAT URLs and document exact evidence/limitations.

## Risks and recovery

- Keep existing ports/database/container ownership unchanged except owned frontend
  listener and affected service restarts. Never reset data.
- No new role policy, external redirects, or plaintext token persistence.
- Snapshot affected sources/runtime artifacts before replacement; failed startup can
  restore previous built JARs. No publish/deploy/push.
- Single-origin refresh cookies need concurrent-tab validation.

## Progress

- [x] Inspect reported UI and underlying role/email behavior.
- [x] ROUTE-01: one origin, context paths, resources and links.
- [x] AUTH-01/02: context-aware email, role onboarding and navigation.
- [x] UI-01/02/03/04/05: layouts, category dialog, tabs, password controls and audit.
- [x] QA-01: automated, browser, visual regression and UAT documents.

## Decisions

- Preserve separate Angular apps with configured base hrefs; they share one HTTP
  origin/API proxy. Do not rewrite unrelated domain routes.
- Omitted portal remains Customer for existing API clients; supported portal context
  selects only a fixed local path, never an authorization role.

## Validation

- 195 backend tests passed with the full Gradle suite forced to rerun.
- 155 frontend unit tests passed; all four production builds passed.
- 1,130 final browser/API assertions passed across 13 regression reports, including
  Phases 1–6, transaction boundaries, QR/card configuration, unmatched receipts,
  two existing visual suites, and two new portal/review suites.
- Final visual proof covers 320/390/768/1440px and 200% text size for long review
  content; actual screenshots inspected for Admin review/categories and Organizer tabs.
- All ten services UP after the affected local restarts; existing Admin login and
  privileged APIs succeed. Only 14200 serves the four portals. Fee, reminder and
  mock payment configuration restored after QA.
- Full evidence and limits: [QA report](../../qa/portal-routing-auth-and-ui-remediation-2026-11-21.md).

## Result

All nine backlog items implemented and validated. Existing UAT data retained;
clearly synthetic QA accounts, categories, organizations, events, orders and audit
evidence remain. No database reset, external deployment, push, CI or hook changes.
Manual UAT/runbook URLs and role onboarding guidance are current.

Remaining validation limits: local simulated payments/email; synthetic QR video,
not physical-camera/browser-device certification. Frontend coverage is 89.41%
lines, 80.66% statements, 78.19% functions, 60.44% branches. Build warnings
and the limited static UI detector scope are documented in the QA report.

## Progress evidence

- All four production builds pass. Identity portal-context integration checks pass.
- Real browser Phase 1: 88; Phase 2: 65; Phase 3: 80; Phase 4: 76 checks passed.
- Dedicated portal/password/category suite: 185 checks passed, no JS/5xx errors.
- Simultaneous four-portal load initially reproduced refresh cookie replay (3×401).
  Origin-wide Web Locks serialization fixes it: all four refreshes 200, four correct destinations.
- Frontend coverage initially stalled before compilation: Angular expands the broad
  `**/*.spec.ts` exclusion synchronously through local node_modules duplicate directories.
  Scoped the same exclusion to actual app/library test roots, without excluding product code.
- Static detector reports no findings across 15 edited UI files.
- Long-content review exposed inherited two-column definition-list alignment;
  explicit stacked layout now passes measured spacing and visual inspection.
- Organization report/settlement anchors had stronger secondary-button styles;
  aligned selector specificity and verified all seven controls' computed styles.
- Review workflow passes 19 checks; UI review passes 128; layout regression passes
  65. The complete final regression matrix is in the linked QA report.
