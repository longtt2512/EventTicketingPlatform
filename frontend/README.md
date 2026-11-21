# Event Ticketing frontend

Four Angular 19 standalone applications share the existing live API services,
authentication, form feedback, confirmation dialogs and unsaved-change guards.

| Application | Local validation URL |
| --- | --- |
| Customer | http://localhost:14200/home |
| Organizer | http://localhost:14200/org/home |
| Admin | http://localhost:14200/admin/home |
| Check-in | http://localhost:14200/staff/check-in/events |

Use the isolated stack and process ownership instructions in
[local development](../docs/local-development.md). `npm run build:all` builds all
four production applications; `npm run test:unit` runs the shared regression suite.

All four apps use the same origin. Angular base hrefs scope navigation to each
portal; `/api/v1` and shared `/assets` remain at the origin. The local server
returns 404 for missing resources and the matching app shell for deep links.
Authentication email context preserves the portal and safe return destination.
Simultaneous portal tabs serialize refresh-cookie rotation with Web Locks.

## UI source

The visual reference is
[`Event_Ticketing_Platform_MVP_UI_Mockups_v1.0.0.pdf`](../docs/Event_Ticketing_Platform_MVP_UI_Mockups_v1.0.0.pdf).
The implementation covers existing Phase 1–4 functionality. It does not introduce
unimplemented revenue analytics, settlement, fee configuration or system settings.

- `libs/ui/src/app-shell.component.*`: customer header and account sidebar,
  organizer/admin navigation, shared authentication frame and mobile navigation.
- `libs/ui/src/styles.scss`: foundation, shell, customer and operations layers;
  `_controls.scss` owns shared action hierarchy and `_experience.scss` the
  customer palette, category/ticket motifs and reduced-motion-aware home.
- `libs/events/src/home-discovery.component.*`: home hero and discovery cards.
- `libs/events/src/event-art.ts`: supplied text-free artwork/category presentation.
- Existing auth, events, booking, organization and tickets libraries retain API
  and authorization behavior. Query-based search and draft protection remain.

`angular.json` copies the supplied icons, plain event artwork and layout artwork
from `docs/event-ticketing-assets/` to `/assets/etp/` for every app. Fonts are
self-hosted under `assets/fonts/` with their OFL license. No remote font requests
are required. The icon attribution is distributed with the assets.

Event cards prefer uploaded media and use text-free artwork when it is missing
or unavailable. The home hero uses branded artwork with real event metadata.
Tickets use decorative artwork; usable QR images and PDFs always come from the
authorized backend. No sample identities, event prices, financial totals or QR
credentials from the mockups are hardcoded into application data.

## Interaction presentation

Primary calls to action use the solid `.button-link` / button style. Supporting
navigation uses `.button-link.secondary`; quiet utilities use `.quiet`. Return
navigation uses `.back-link` with a consistent arrow and 44px target. These are
visual treatments: destinations remain real anchors and operations remain buttons.
Inline references and linked record names retain contextual link behavior. Order
details do not link back to themselves; payment results retain a secondary link
to order details. Refresh labels identify the state being refreshed.

The customer workspace retains the ETP indigo brand with warm paper, lilac,
category tints and ticket geometry. Organizer/admin/check-in remain restrained.
Uploaded media is preserved, including synthetic fixture images in the local stack.
Run `python3 scripts/run-ui-review-browser.py` from the repository root for the
read-only cross-workspace layout, navigation, refresh and keyboard proof, after
preparing the documented isolated stack and Phase 1–4 fixture data.
