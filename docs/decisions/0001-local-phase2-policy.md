# Accepted local Phase 1 completion and Phase 2 policy

Date: 2026-11-20
Status: Accepted by the repository owner in conversation

The owner instructed implementation after the explicit proposal of a 60-second
verification resend cooldown and the Phase 2 bundle in the P1 execution plan.
This supersedes the pending decision entries in that plan. It authorizes local
implementation and validation, not publishing or deployment.

- Verification emails may be sent at most once per normalized account email in
  60 seconds, including the initial registration email. The resend response is
  generic for eligible, unknown, verified, or locked accounts. Existing token
  lifetimes (24 hours) and single-use semantics stay in force.
- Incomplete event drafts are allowed. Review submission requires name, category,
  description, banner, venue, start/end, capacity, attendance terms and at least
  one valid ticket type, with a verified active organization.
- Pending review is read-only. Editing an approved event returns it to draft and
  requires review again. Published core content is locked; future ticket prices,
  quotas, sale pause/resume remain manageable without altering prior allocations.
- Organization owners/management members and Admin may cancel before completion,
  with a reason and audit history. Lifecycle follows server time.
- Prices are integer VND. Sale windows are half-open [start,end), ending no later
  than event start. Inventory follows BRS §6: nonnegative counters, atomic quota
  and capacity checks, no direct editing of sold/reserved quantities.
- Admin controls featured events. Popular ranks by total confirmed tickets, then
  upcoming start time. Cancelled public details remain readable without purchase.
- Banner/gallery uploads accept actual PNG/JPEG images up to 5 MB each. Local
  MinIO validates real format/size; production malware scanning remains a
  deployment prerequisite, not a claim of this local implementation.

Scope: PRD AUTH-01–11/ORG-01–09 completion, then EVT-01–14, INV-01–13,
DSC-01–11. Order/payment processing is Phase 3. No fabricated attendees or sales
are used to substitute for that integration. Existing architecture, API ownership,
current membership checks and no cross-service database access remain applicable.
