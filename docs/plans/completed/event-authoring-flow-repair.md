# Execution Plan: Event authoring flow repair

Date: 2026-11-21

## Status

Completed — 2026-11-21

## Outcome

An organizer can create an event, complete its schedule and ticket types, and
submit it for review without contradictory feedback or hidden requirements.
Entered event dates in the past are rejected. Money inputs format Vietnamese
dong automatically. Required fields are visibly marked and prose does not join
sentences with semicolons.

## Authority and scope

- The user's 2026-11-21 request explicitly authorizes these corrections and a
  thorough Impeccable review of the authoring journey.
- `docs/decisions/0001-local-phase2-policy.md`: incomplete drafts remain allowed,
  review requires complete content, integer VND, ticket quotas, sale windows,
  and separate review/publication.
- `docs/03-ui-ux-design.md` §§3.4, 5.3, 6: authoring steps, validation, localized
  amounts, accessibility and recoverable feedback.
- `docs/local-development.md`: isolated validation services, synthetic accounts,
  actual browser workflows and process ownership.
- Previous authorized local UI edits remain in place. The untracked
  `frontend/libs/organization/src/events.component 2.html` is unrelated and is
  not owned by this task.

## Approach

1. Record design assessment A before detector evidence, reproduce with a synthetic
   organizer, and identify frontend/backend validation gaps.
2. Reject past event dates at save and submission, retain incomplete drafts,
   provide actionable required-field feedback and accurate ticket terminology.
3. Implement reusable VND inputs and required markers. Audit money fields and
   user-facing semicolon copy across frontend and backend.
4. Clear stale messages and guide users from review to the exact missing field.
5. Run unit/integration tests and the real create/upload/tickets/review/publish
   workflow against isolated local services. Inspect desktop/mobile renders.

## Risks and recovery

- Do not change or delete the user's events. Use unique synthetic events and
  cancel only events created by this run after verification.
- Keep API amounts numeric and preserve integer/range validation, including
  percentage controls and non-VND transaction currency when present.
- Mark requirements for review without forbidding intentionally incomplete drafts.
- Rebuild before restarting only the owned commerce service if necessary.
- No push, deployment, hooks or CI configuration changes.

## Progress

- [x] Identified missing draft date validation and stale success/error messages.
- [x] Design review and baseline reproduction.
- [x] Backend validation and focused integration proof.
- [x] Authoring feedback, field guidance, money and copy fixes.
- [x] Real browser workflow and responsive visual verification.

## Review method

Impeccable's `spawn_agent` tool is unavailable. Assessment A is performed in
this context before detector assessment B. The review report will disclose
this single-context limitation. Questions are skipped because the user already
specified the fixes and authorized implementation.

## Validation

- Frontend: `npm --prefix frontend run test:unit`, 171 passing tests.
- All four production frontend builds passed (`npm --prefix frontend run build:all`).
- Backend: `./gradlew test bootJar`, 196 tests across the suite, no final failures.
- Real browser flow: 73 checks passed in `scripts/phase2-browser-workflow.js`,
  including upload through MinIO, negative date and quota cases, review/rejection/
  resubmission/approval/publication, public search, paid price update, cancellation.
- Real recovery flow: 17 checks passed in
  `scripts/event-authoring-recovery-workflow.js`. One transport abort is injected
  to verify failure recovery. Other requests reach the actual local services.
- Final manual inspection and live Impeccable injection: information, ticket edit,
  and review at 1440px and 390px. No document overflow or detector findings in the
  four final inspected states. Source detector returned no findings.
- Production copy audit found no remaining user-facing semicolon prose in scanned
  frontend HTML/TS literals and backend Java strings. Syntax/comments and the
  unrelated `events.component 2.html` are excluded.
- Evidence: `output/playwright/event-authoring-{full,recovery}-result.json`,
  `output/playwright/event-authoring-final-*.png`, `output/qa/event-authoring-*.json`.
- The pre-existing PDF extraction test failed once on a randomly generated UUID
  containing an `ff` ligature, then passed on rerun. PDF rendering was not changed.
  The finance message assertion was updated to match sentence capitalization.
- Browser runner now waits for visible images. Hidden lazy preview images are not
  expected to load until their authoring step is opened.
- Impeccable review was single-context because no spawn_agent tool is exposed.
  Its baseline/manual score improved from 17/40 to 35/40. Detector checks do not
  replace the behavioral proof above.

## Result

The event creation flow works end to end on the isolated local stack. Past event
dates are rejected both inline and by the API. VND inputs retain numeric API
values, required fields are marked, and review links focus missing fields.
Old success/error messages no longer coexist. Save actions remain in the footer.

Only synthetic events created by this run were modified. The published test event
and the two draft/recovery events were cancelled after validation. Test browser
contexts and temporary Impeccable servers were closed. Existing local application
services remain running with rebuilt code. No push or deployment was performed.

The user's existing untracked `events.component 2.html` was left untouched.
Generated credential-bearing runners/logs stay in ignored `.runtime/` with mode
0600. The critique temp body was removed after persistence. No harness or CI rules
were changed.
