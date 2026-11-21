# UI consistency and customer expression

## Status
Active — 2026-11-20

## Outcome and authority
User explicitly requested Impeccable review of all existing UI, consistent actions
versus hyperlinks, and a livelier, happier customer experience. Current mockups,
supplied assets, docs/03-ui-ux-design.md and the Phase 1–4 behavior remain authority.
No new backend, claims, financial rules, publication or deployment.

## Recovery
Frontend is existing untracked user work. Baseline of apps/libs/assets preserved
at /tmp/etp-ui-review-baseline. Retain uploaded media, real data and all permissions.
Use the existing isolated stack and synthetic fixture accounts only.

## Assessment A — recorded before detector output
⚠️ DEGRADED: single-context (spawn_agent unavailable in this session).
Targets: frontend/libs/{ui,auth,events,booking,tickets,organization}/src.
Source inventory and current reference screenshots reviewed across all four apps;
user screenshots supply direct evidence for the confirmed-order surface.
Native browser host unavailable; use the installed Playwright skill for rendered proof.

Design specificity: customer discovery has ETP assets but lacks the energy of an
event product. Operational surfaces have useful familiar hierarchy. Screens share
colors/fonts but not a clear action hierarchy. Strengths: honest server states,
real QR flow, responsive portal navigation, protected edits and confirmations.

Heuristics (0–4): status 3, real-world language 2, user control 3, consistency 1,
error prevention 3, recognition 2, efficiency 3, minimalist design 2,
error recovery 3, help 2. Total 24/40. Main cognitive burden is duplicated actions,
unclear refresh terminology and ungrouped secondary navigation. Category choices
are browse facets; do not hide meaningful categories merely to reduce a count.

Priorities:
- P1: action links/return controls have inconsistent treatment; confirmed-order
  detail includes a self-link. Standardize primary, secondary and back patterns.
- P1: refresh labels do not identify what gets refreshed; clarify ticket issuance.
- P2: customer discovery/order/tickets feel administrative. Use the supplied brand,
  category color, ticket geometry, real event data and a more joyful composition.
- P2: secondary links and action density compete with the customer's next step.
- P2: retain comfortable touch targets, keyboard focus and reduced motion while
  extending the visual system; inspect long Vietnamese titles and empty states.
Personas: first-time buyer needs one clear path from confirmation to QR; keyboard
user needs correct anchor/button semantics and focus; event staff need restrained
screens and unmistakable primary actions. No product intent question is open.

## Plan
- [x] Inspect affected surfaces, authority and incumbent behavior.
- [x] Detector/browser evidence and final direction.
- [x] Consistent controls across all existing workspaces.
- [x] Customer expression across discovery, account and purchase surfaces.
- [ ] Build, functional/keyboard/responsive proof and rendered inspection.

## Validation and result
Pending. Record detector limitations and screenshots here, not in a parallel
critique orchestration store. Questions skipped: user already requested the fixes
and specified customer direction; existing functional scope remains unchanged.

## Assessment B and chosen direction
Source detector returned no findings for frontend/libs; Angular source scan is
limited and is not evidence of overall quality. Live 390px home scan returned nine
readability findings: hero paragraph under 12px, dates/meta under 11px and mobile
navigation under 11px. Fix those together with the manually identified issues.
Native browser host could not create a tab; no overlay or live-server was started.
Playwright/Impeccable captured current live home at 1440 and 390; incumbent screenshots
cover operational/auth/ticket surfaces, verified against current source.

Chosen direction: retain ETP indigo and Be Vietnam Pro; warm paper background,
lilac hero with one event ticket, category-specific tints, perforation geometry,
comfortable readable metadata. Customer expresses anticipation; payment/QR states
stay explicit. Operational workspaces share controls while retaining restrained
surfaces. Anchors keep native navigation semantics; no role=button on links.

## Implementation
- Shared primary/secondary/quiet/back presentation retains native anchor/button
  behavior. Back controls use the asset arrow, focus ring and 44px target.
- Removed order-detail self-link; result/checkout keep a secondary details CTA.
  Refresh labels now say which state is loaded. Issuance still polls and reads
  the same GET endpoint; QR and cancellation controls are unchanged.
- Customer home has warm/lilac surfaces, a real event in a ticket composition,
  category tints, clearer metadata and one reduced-motion-aware entrance.
- Extended customer presentation to discovery, ticket selection, confirmed orders,
  payment result, ticket list/detail, account/profile and empty states.
- Unified operational/auth action links, including external organizer portal CTA.
- Increased small functional text in mobile navigation, dates, editor steps,
  table headings and check-in. Removed superseded home CSS after migration.

## Validation progress
Four apps build under the unchanged 500 kB initial warning budget (largest 498.13
kB). 121 unit tests pass. Home 390px detector now returns no findings; live source
and screenshot review remain necessary because the detector does not judge layout.
65 navigation checks pass. Extended UI proof covers customer flows, organization
sections, authoring steps, admin queues/payments and check-in at four widths;
128 extended UI checks pass with no reviewed functional text below 11px.
Phase 2: 65 pass; Phase 3: 80 pass; transaction edge cases: 8 pass; Phase 4: 76
pass, including actual QR decoding, PDF, replay protection and authorization.
Final CSS-only adjustment raises discovery result count to 14px; rebuilding and
rechecking that surface before completion.
