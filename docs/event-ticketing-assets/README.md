# Event Ticketing Platform — MVP UI Assets Pack v1.0.0

This package accompanies the `Event_Ticketing_Platform_MVP_UI_Mockups_v1.0.0.pdf` collection and provides reusable asset files for Codex. Covers 51 mockup screens across Customer, Organizer, Check-in, Admin, and shared Auth.

## Folder structure

```text
event-ticketing-assets/
├── backgrounds/
│   ├── event-posters/    # 6 PNG + SVG posters, contains sample event text
│   ├── event-plain/      # 6 PNG + SVG artworks WITHOUT text, preferred for dynamic event data
│   └── layout/           # hero, auth, QR-scanner, cover PNG + SVG backgrounds
├── icons/
│   ├── brand/            # ETP brand mark + wordmark (SVG)
│   ├── indigo/           # primary purple icon SVGs
│   ├── slate/            # dark gray icon SVGs
│   └── white/            # white icon SVGs for dark backgrounds
├── docs/
│   ├── screen-asset-map.json  # all 51 screens, routes, relevant assets
│   └── THIRD_PARTY_NOTICES.md
├── design-tokens.json
├── assets.css
├── asset-manifest.json
└── preview/contact-sheet.png
```

## Important: these backgrounds ARE NOT photographs

The source mockup used native CSS gradients, glow orbs, circles, and text—not third-party photography. Therefore:

1. The six **PNG poster backgrounds** were rendered from the source mockup's actual CSS.
2. The **SVG versions** are editable vector equivalents of the same branded gradient art (minor rendering differences are possible).
3. Prefer `event-plain` and render event names, dates, and other information from actual backend data. The `event-posters` with fixed demo labels are intended for demos and visual references only.
4. The navigation/action icons in the original mockup were Unicode glyphs, not asset files. The new SVG set normalizes these into reusable, consistent vector icons. See attribution.
5. The QR code shown in the original mockup was invalid demo data and is deliberately NOT included here. Production QR codes MUST be generated per ticket by the backend.

## Recommended use in Angular

Copy `backgrounds`, `icons`, and `assets.css` into `frontend/public/assets/etp/` or `frontend/src/assets/etp/`, adjusting URLs for your actual Angular build configuration.

```html
<img src="/assets/etp/icons/indigo/ticket.svg" alt="Ticket" width="24" height="24" />
<img src="/assets/etp/backgrounds/event-plain/aurora-festival.png" alt="" />
```

```css
.event-card-art { background-image: url('/assets/etp/backgrounds/event-plain/aurora-festival.png'); background-size: cover; background-position: center; }
.home-hero { background-image: url('/assets/etp/backgrounds/layout/hero-home-background.png'); background-size: cover; }
```

SVG files are well suited for inline use in Angular components and can be styled with CSS or edited in any vector editor. Raster PNGs are convenient for backgrounds when a precise visual match is desired.

## Asset list for 51 screens

See `docs/screen-asset-map.json`. This is an implementation-oriented *suggested* map, not a full image-placement spec. No fixed poster is required on every Organizer/Admin screen.

## Colors / theme

Indigo `#4F46E5`, deep indigo `#4338CA`, backgrounds `#F8FAFC`, dark text `#111827`. See `design-tokens.json`.

## Usage precautions

- Icons are accessibility-decorative by default. Set meaningful labels for icon-only buttons.
- Do not hardcode organizer/user/event identity from mockup demo artwork into production interfaces.
- Do not treat these assets as payment/certification logos; use only authorized payment provider brand assets when integrated.
- No fonts are included. Use a separately installed or remotely loaded font under its own license.
- Assets are a reusable reference package, not ready-to-run Angular components.
