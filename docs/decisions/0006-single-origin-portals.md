# Single-origin portals and authentication context

Accepted by the user on 2026-11-21 (ROUTE-01, AUTH-01/02 backlog).

Customer uses `http://localhost:14200/`, Organizer `/org/`, Platform Admin
`/admin/`, and Check-in Staff `/staff/`. Four Angular apps retain their own route
and workspace configuration; one local HTTP listener selects the app by path and
proxies `/api/` to the gateway. API routes do not acquire a frontend prefix.

Authentication accepts optional `context: {portal, returnUrl}` on registration,
verification resend and password-reset requests. `portal` is one of `customer`,
`organizer`, `admin`, `checkin`; returnUrl is an allowlisted in-app path. Missing
context preserves the Customer entrypoint for existing clients. Email embeds the
fixed portal path and safe return URL before the one-time token fragment. The
existing encrypted mail outbox preserves it without browser storage or a schema
migration. Context is navigation, never authorization or a role assignment.

Verification completes at the originating portal. The sign-in link retains the
intended destination; authenticated requests remain subject to existing role,
organization membership and event assignment checks. Organizer applicants see
onboarding/progress; Staff without membership/assignments see next steps. User
roles can coexist with Customer; registration never self-grants privileged roles.

The category state-change action requests the required audit reason in its
confirmation dialog. Existing category/event history semantics are unchanged.
Every password field provides an independent, initially hidden visibility toggle.

Local runtime, manual UAT and executable browser fixtures use the new entrypoints.
Old ports are no longer user entrypoints; historical QA reports retain their
observed URLs. No production hosting or external deployment is established here.
