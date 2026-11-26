# Execution Plan: Vietnamese technical system document

Date: 2026-11-26

## Status

Complete — diagram notation and routing revision validated on 2026-11-26

## Outcome

Produce one Vietnamese academic technical report using the PTIT template,
describing the current Event Ticketing Platform, with architecture and sequence diagrams, database
ownership and relationships, API contracts, security, operations and evidence.
Provide an editable Word document and its maintainable Markdown source. The user
selected the PTIT academic report template and explicitly requested blank course,
student, class and supervisor details.

## Context and authority

- User request on 2026-11-26: use architecture, document and database design skills.
- Baseline commit: `d9af3449698735687e77315381d436f3b89ce7ce`.
- Product: `docs/01-product-requirements.md`, `docs/02-business-rules.md`.
- Accepted behavior: `docs/decisions/0001` through `0007`.
- Current implementation: Gradle/Angular configuration, Flyway migrations,
  service code, OpenAPI and event contracts.
- Validation and operation: `docs/local-development.md`, `docs/08-test-strategy.md`,
  completed execution plans and QA reports.

## Scope

Document the existing system. Distinguish implemented behavior, accepted local
policy, original proposals and unverified production targets. Do not introduce
new product policy, change application code, modify data or deployment settings,
or commit/push this new work without a request for that action.

## Approach

1. Inspect accepted decisions, service boundaries and actual migration inventory.
2. Trace booking, payment, issuance, admission and settlement behavior to code.
3. Write a consolidated document with source references and diagrams.
4. Generate Word using the managed document runtime; render and inspect every page.
5. Validate references, schema/API inventory and artifact completeness.

## Risks and recovery

- Earlier design documents contain proposals that differ from implementation;
  use code and accepted decisions to label those differences explicitly.
- Historical QA is not a fresh test run. Preserve dates and limitations.
- No runtime credentials, QR secrets or real account data belong in the output.
- Only new document/diagram files are owned by this task; recovery can remove
  those without changing application behavior.

## Progress

- [x] Read workflow, relevant skills, accepted decisions and validation records.
- [x] Verify service, schema and API inventory and critical flows.
- [x] Distill and inspect all nine reference pages of the retained PTIT template.
- [x] Verify 10 services, 4 apps, 34 migrations and 84 application tables.
- [x] Author the Vietnamese document and diagrams.
- [x] Generate and visually verify the Word artifact.
- [x] Validate source references and record results.

## Validation

- Check referenced repository files, migration inventory and API routes.
- Render DOCX to page images and visually inspect all pages.
- Check Vietnamese text, headings, table geometry, page breaks and references.
- Application regression is not rerun for documentation-only changes; historical
  results are clearly attributed to their recorded dates.

## Previous revision v2

The report was rewritten after the user's review:

- All table captions follow their tables. Table and figure captions use 12 pt,
  one point smaller than the 13 pt body text.
- The overview uses Frontend, Microservices and PostgreSQL, without portal route
  suffixes or the physical database name.
- The narrative describes current behavior directly; migration inventory and
  authoring-process narration were removed from the report body.
- A dedicated backend chapter explains API Gateway and all nine business
  services: responsibilities, owned data, components, APIs, dependencies,
  transaction boundaries and failure recovery.
- Fifteen diagrams are rendered from editable PlantUML sources. Ten sequence
  diagrams cover authentication, publication, reservation, payment, order
  confirmation, ticket issuance, admission, cancellation, settlement and outbox.
- Professional references include arc42, C4, Microsoft interservice communication,
  AWS transactional outbox and PlantUML documentation.

Deliverables:

- `docs/Bao_cao_ky_thuat_Event_Ticketing_Platform_PTIT.docx`
- `docs/bao-cao-ky-thuat-ptit.md`
- `docs/technical-system-assets/`: 15 PNGs and 15 PlantUML sources.

The final report has 68 pages, seven chapters, 15 figures and 25 body tables.
Its appendix describes 84 application tables in nine schemas. Five cover fields
remain blank, as requested. All 65 source/image links were checked; local targets
exist and professional reference pages were retrieved from their official sites.

## Validation of v2

- PlantUML rendered all 15 sources successfully.
- DOCX was rendered through the managed document runtime. Every page was visually
  inspected at full render size. Eight final copy-edited pages were reinspected;
  pixel comparisons confirmed all remaining final pages match reviewed pages.
- Structural checks confirm captions immediately below each table, 12 pt caption
  text, repeated table headers, blank cover fields and complete diagram sources.
- All 52 contents, figure and table entries have verified page numbers and
  internal bookmarks. The final page map is stable.
- Template preservation checks confirm identical section geometry, footer,
  styles, numbering and logo. Fourteen original ZIP parts remain byte-for-byte
  identical. The retained template is unchanged (SHA-256
  `1050fc0bcb71a798a3659c726ac20daff3da59e77487fdbd693a1d118a47c970`).
- Markdown and PlantUML sources have no trailing whitespace. Git status shows
  only the new report, sources, diagrams and this execution plan.

Render images, verification output and page comparison evidence remain under
`.runtime/technical-document/`, with the final render in `revision-approved/`.
Final Word SHA-256:
`d65fed8dbf0770c8e6c4a77be2283cd699a863657e11473aa7a3382164716e61`.

No application code, schema, data or running environment was changed. Application
suites were not rerun for these documentation changes. Historical test results
retain their dates and scope. Production readiness, performance, payment-provider
integration and recovery targets remain explicitly identified as unverified.
The new report files have not been committed or pushed.

## Visual revision requested by the user

Reduce dense prose, distribute explanatory drawings across the report, and replace
service lists with consistent, concise comparison tables. Preserve current
system behavior, PTIT cover, typography, 12 pt captions below tables, and blank
cover fields. Continue using locally rendered PlantUML.

Plan: back up v2; condense repeated explanations; make ten service profiles; add
journey, state, data-flow, security, deployment and validation diagrams; render
and inspect every page; check references, table placement and inventory.

No runtime changes or publishing are in scope.

### Visual revision implementation
- Reduced source from approximately 14,700 to 9,838 whitespace-delimited words
  by removing repeated explanation while retaining data constraints and errors.
- Replaced narrative descriptions of gateway and all nine business services
  with ten consistent seven-aspect tables; retained backend sequence details.
- Added twelve PlantUML diagrams covering journey, lifecycles, communication,
  routing, authorization, inventory, reporting, security, deployment and tests.
- Body remains 13 pt; captions remain 12 pt below tables. Table line spacing
  is 1.15 for readable density, with first-column tint in service profiles.
- First render passed structural checks; layout review identified diagram
  aspect ratios and fragmented code/table navigation for adjustment.


## Visual revision final result

The revised report contains 62 pages, seven chapters, 27 PlantUML figures and
35 body tables. Ten consistent service profiles cover API Gateway and all nine
business services. Eleven figures use sequence notation: the ten detailed
business flows plus the REST/Kafka communication comparison. The twelve new
figures also cover journey, event/order/ticket states, authorization, routing,
inventory, reporting, security, deployment and test scope.

The Markdown source decreased from 14,803 to 9,820 whitespace-delimited words
(33.7%). Repeated prose was condensed; transaction conditions and recovery
branches remain in profiles, sequences and exception tables. The appendix still
contains exactly 84 application tables across nine schemas.

### Observable validation

- All 27 PlantUML sources rendered successfully to PNG; each embedded image has
  an editable matching source.
- All 62 final pages were visually covered at native render size: 27 final pages
  match already inspected pages pixel-for-pixel, and 35 changed/new pages were
  directly reinspected. The last copy edit changed only page 51, which was
  rendered and reinspected; the other 61 pages remain pixel-identical.
- All 35 table captions immediately follow tables at 12 pt, versus 13 pt body;
  table headers repeat. Service profiles stay together; short chapter-end
  tables stay whole, and long tables avoid isolated first/last data rows.
- All 74 linked navigation entries have stable verified page references.
  Footer numbering runs continuously from 1 to 61 after the unnumbered cover.
- Five cover fields remain blank. Fourteen original template ZIP parts and both
  section definitions are preserved; the retained reference file is unchanged.
- All 77 local-source/image and professional-reference links are retained and
  local targets resolve. Professional references were verified in v2.
- Final render and evidence: `.runtime/technical-document/visual-approved/` and
  `.runtime/technical-document/visual-revision/`.
- Final DOCX SHA-256:
  `582962e573b523de1005d5ffab6508af3d851e88e37b7743a68a2a824a8bb50a`.

Application code, schemas and runtime remain unchanged. Historical test results
retain their original dates and scope; application tests were not rerun for
this document-only revision. No commit or push was performed for the report.


## Diagram review requested by user

Replace crooked/free-form journey connectors with a suitable standard diagram,
review every drawing for routing/alignment/legibility, and remove arrow shorthand
from prose and tables. Use role-based activity, component/deployment, state/data
models and sequence notation according to the question each view answers.
Keep current behavior, service tables, PTIT layout and caption rules.


## Diagram revision v4 completed

Replaced the six-box snake journey with a PlantUML activity diagram using
responsibility lanes. Redrew fourteen structural, workflow, lifecycle, security,
deployment and test-scope diagrams; corrected one publication sequence label.
Reviewed all 27 figures, including the eleven sequences and two ERDs.

Connectors use orthogonal routing where appropriate. Service decomposition and
test-scope views use containment rather than ambiguous flow arrows. Issuance
obligations and individual ticket states are shown separately. The interservice
contract table now has caller, receiver, content and failure-handling columns;
all arrow shorthand was removed from prose and labels. UML connector syntax
remains in diagram sources. Event state changes were checked against EventService;
issuance and cancellation transitions were checked against IssuanceService and
EventGates.

### Validation of v4

- All 27 PlantUML sources render and pass syntax validation.
- The final DOCX has 64 pages, 27 figures, 35 body tables and ten service profiles.
- All final pages were visually covered at native render size: 57 directly
  inspected pages and seven pixel-identical matches to already inspected pages.
- All embedded diagram PNGs match the current source assets byte-for-byte.
- All 74 navigation entries have stable verified page references and bookmarks.
- All 63 numbered footers are correct; five cover fields remain blank.
- All table captions immediately follow their tables at 12 pt, with 13 pt body.
- The appendix still matches all 84 application tables in nine schemas.
- Fourteen template package parts remain unchanged, including styles and footer;
  section definitions and retained reference are preserved.
- Sources have no trailing whitespace or arrow shorthand. All local links exist.

Final render: `.runtime/technical-document/diagram-approved/`.
Evidence: `.runtime/technical-document/diagram-revision/verification.json`,
`final-qa.json`, `changed-diagrams.json` and `reviewed-matches.json`.

Final Word SHA-256: `5794e2430f5cf5083b0d3f1f2a2a048990122c41b0803398eb51e66879afc6ea`.

No application code, data or environment was changed. Application tests were not
rerun for this document-only revision; historical validation dates remain explicit.
No commit, push or deployment was performed.
