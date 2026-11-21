# Execution Plan: UI/UX P1 remediation and polish

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

Resolve the four P1 findings accepted by the user: actionable form errors,
protection for unsaved input and navigation context, working email verification
recovery, and explicit confirmation for consequential administrator actions.
Then apply Impeccable polish to the changed flows and verify the rendered UI.

## Context

- User-approved findings: the UI/UX review in this conversation.
- UX authority: `docs/03-ui-ux-design.md` sections 2.5, 4.2, 6.2–6.3,
  X-02, and 10.1–10.2 (inline errors, dirty-form warning, safe return path,
  resend recovery, contextual confirmation and focus handling).
- Security: `docs/07-security-design.md` section 5.4; accepted Phase 1 token
  lifetimes in `phase-1-foundation.md` remain unchanged.
- Runtime and ownership: `docs/local-development.md`.

## Scope

Shared Angular forms, routing, organization editing and moderation, the minimal
identity resend API needed by the recovery flow, and focused regression proof.
Mobile redesign, discovery/booking/QR work, deployment and publishing are excluded.

## Approach

1. Reuse the existing Indigo system; add shared field feedback, unsaved-change
   protection and an accessible confirmation dialog.
2. Integrate the patterns into existing account and organization flows; preserve
   the selected organization section in the URL and a safe login destination.
3. Implement resend after the owner resolves the missing cooldown policy.
4. Run regression tests, four production builds, and real browser workflows.
5. Polish changed paths, run the Impeccable detector once, record evidence.

## Risks And Recovery

- Existing application code is untracked user work. Preserve unrelated files;
  capture a task-local baseline outside the repository before editing.
- Never persist passwords, tokens or full payout account drafts in web storage.
  Protect navigation with a warning and keep drafts only in mounted UI memory.
- Use isolated validation accounts and local MailHog; no real recipients or
  existing business decisions are used for mutation tests.
- The resend interval is not established by repository defaults. An asynchronous
  decision is pending; independent frontend work can continue.

## Progress

- [x] Review findings, implementation, authority and runtime instructions.
- [x] Shared feedback and navigation protection.
- [x] Administrator confirmation flows.
- [x] Email verification resend and recovery.
- [x] Regression/build/browser validation including real resend and cooldown proof.
- [x] Impeccable polish and rendered proof including the mobile resend form.

## Decisions

- 2026-11-20: Retain section components in memory when changing organization
  sections; store only the section in `?section=...`. Departure/reload/logout
  protection applies to drafts across mounted sections. No sensitive draft is
  persisted in local/session storage. This implements UX §2.5/6.2, without adding
  a draft retention policy.
- 2026-11-20: Confirmation captures the reviewed subject and reason. Cancel/Escape
  performs no mutation, focus starts on Cancel and returns to the trigger.
  Applied to application approval/rejection, account lock/unlock, organization
  suspension/restoration, and payout approval/rejection.

- 2026-11-19: User explicitly prioritizes P1 and defers mobile optimization;
  the final requested skill action is `impeccable polish`, not publishing.
- 2026-11-19: Preserve the current visual system and business rules. Keep
  unsaved sensitive input in memory only; warn before it would be discarded.

## Validation

- Angular tests for error visibility/focus, draft protection, safe navigation,
  and cancel/confirm behavior; all four Angular production builds.
- Identity integration tests for generic resend responses and cooldown behavior
  once authorized, including concurrent requests and email delivery.
- Playwright: real error/correction, tab navigation, login return, resend email
  and confirm/cancel paths; desktop visual review and narrow regression checks.

## Previously pending owner decisions (accepted 2026-11-20)

The following proposal was accepted by the owner on 2026-11-20; see the decision
record above. The original pending text is retained below as historical context. `docs/WORKFLOW.md` requires a decision before edits
that would introduce these externally observable policies.

1. **Email verification resend cooldown:** proposed 60 seconds between sends to
   the same normalized email (alternative offered: 120 seconds). Generic response
   regardless of account existence/verification state. Existing token lifetimes
   and single-use requirements remain unchanged. No resend API or sending UI has
   been implemented while this decision is pending.
2. **Local Phase 2 policy bundle**, based on BRS §5–7 and API §7 (currently
   PROPOSED/OPEN):
   - Incomplete drafts allowed; submit requires name, category, description,
     banner, venue, time, capacity, attendance terms and a valid ticket type.
   - Pending review is read-only. Approved content changes return to draft and
     require review again. Published events permit future price/quota changes
     and sale pause/resume; core event content remains locked.
   - Owner/management members and Admin may cancel an event before completion,
     with a reason; lifecycle follows server time.
   - Integer VND prices; half-open sale window with close at/before event start.
   - Admin controls featured; popular uses total confirmed ticket count, then
     upcoming start time. Cancelled public details remain readable without a
     purchase action.
   - PNG/JPEG banner/gallery images, at most 5 MB each. Local MinIO validates
     actual type/size; production malware scanning is deferred to deployment.

Phase 2 scope after approval: PRD EVT-01–14, INV-01–13, DSC-01–11. Order/payment
work remains Phase 3; do not pretend paid attendees or sale history exist before
that integration. Preserve the Event Commerce ownership and inventory boundaries
in the existing architecture/database/API documents.

## Observed validation (2026-11-20)

- `npm run test:unit -- --code-coverage`: 71 passed; statements 92.99%, lines
  95.13%, branches 79.59%. No coverage enforcement configuration was changed.
- `npm run build:all`: all four production builds pass.
- `./gradlew test --rerun-tasks`: 26 integration tests pass (Identity 15,
  durable mail 2, Organization 7, Event Commerce draft boundary 2).
- `python3 scripts/run-phase1-browser.py`: 84 checks passed on isolated runtime,
  covering real registration/verification/reset, rejection/resubmission/approval,
  payout masking, staff/membership revocation, suspension/restoration, account
  lock/unlock, token replay, cross-organization denial, dirty navigation,
  return destinations, and Admin confirm/cancel/focus behavior.
- Browser report: `output/playwright/phase1-result.json`; zero JavaScript errors,
  zero unexpected 5xx, no unexpected console errors. Expected 401/404 for anonymous
  refresh and negative authorization, plus intentionally aborted network calls,
  were inspected separately.
- Rendered review at 390, 768 and 1440 pixels for confirmations, desktop/mobile
  login, and mobile payout. Table scrolling keeps column labels readable.
  Evidence: `output/playwright/p1-login-errors.png`,
  `output/playwright/p1-confirmation-{390,768,1440}.png`,
  `output/playwright/phase1-payout-mobile.png`.
- One manual `impeccable detect --json --changed` pass returned `[]` (exit 0),
  stored at `.runtime/phase1/p1-impeccable.json`. No critique snapshot existed.
- Earlier browser failures exposed a cancelled native submit caused by an Angular
  click expression returning false, and a test URL read before navigation ended.
  Both were corrected; the completed run above passed.

## Result

Completed and validated locally on 2026-11-20: all AUTH-01–11/ORG-01–09 and
four P1 UI/UX groups, including the accepted verification resend recovery.
The final Phase 1 browser regression after Phase 2 integration still passes
88 checks without browser exceptions or server 5xx errors. Implementation,
proof and limitations are recorded in the completion checkpoint at the top.
Phase 2 continues in its own execution plan; no push or deployment performed.
