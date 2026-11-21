# Local acceptance test strategy

## Scope and authority

Phase 6 implements the owner's 2026-11-20 revised request: functional acceptance,
concurrent business operations and four-app UX on the local stack. Disaster
recovery, backup/restore, security/dependency audits and performance measurements
are excluded. Existing regression assertions in those areas remain intact; they
do not turn this run into an audit or recovery rehearsal.

Authority: PRD §§6/15, BRS §17, UX §§10.1/11 and accepted local decisions 0001–0005.
No new business policy, deployment work, CI configuration or branch rule is added.

## Approach

- PostgreSQL Testcontainers verify persisted invariants, including rejected
  operations and race results. Mocked remote boundaries are identified by the
  individual integration test; they do not establish a cross-service end-to-end result.
- Angular/Karma verifies state, errors, forms and navigation with controlled HTTP.
- Playwright CLI drives real built apps and local services, PostgreSQL, Kafka,
  MinIO and MailHog. Only payment provider/bank outcomes are simulated.
- New Phase 6 browser proof covers a mixed paid/free order through issuance and
  two buyers synchronized at the reservation HTTP boundary. Its fixture is fresh
  on every run. Existing backend proof synchronizes 100 final-seat buyers.
- UI checks cover responsive reflow, names/labels/landmarks, keyboard, form errors,
  empty states, failed reads/retry, 200% root text scaling and reduced motion.
  Computed-color sampling excludes complex image/gradient/translucent backgrounds;
  rendered inspection supplements it. This is not a screen-reader certification.
- Initial failures are classified as product defects, fixture errors or runner
  synchronization errors. Assertions are preserved at the documented behavior level.

## Module coverage

| PRD module | Native owner | Real UI workflow |
| --- | --- | --- |
| Authentication/authorization | IdentityIntegrationTest, auth/session specs | Phase 1 |
| Organizer management | OrganizationIntegrationTest | Phase 1–2 |
| Event management | EventManagementIntegrationTest | Phase 2 |
| Ticket types/inventory | EventManagementIntegrationTest, OrderIntegrationTest | Phase 2, Phase 6 last-seat race |
| Discovery | DiscoveryIntegrationTest, discovery specs | Phase 2, Phase 6 empty/error/retry |
| Booking/orders | OrderIntegrationTest, order/booking specs | Phase 3, Phase 6 mixed order |
| Local payment | PaymentIntegrationTest, CheckoutIntegrationTest | Phase 3, payment methods |
| QR e-ticket | TicketIntegrationTest, TicketPdfLayoutTest | Phase 4, Phase 6 mixed issuance |
| Check-in | TicketIntegrationTest, tickets specs | Phase 4, Phase 6 four views |
| Commission/settlement | CommissionIntegrationTest, SettlementIntegrationTest | Phase 5 |
| Reporting | ReportingIntegrationTest | Phase 5 dashboard/CSV |
| Admin | OperationsIntegrationTest, operations specs | Phase 1/2/3/5/6 |
| Email | MailDeliveryIntegrationTest, NotificationIntegrationTest | Phase 1/3/4/5 MailHog |

## BRS traceability

`Order`, `Event`, `Payment`, `Ticket`, `Settlement`, `Commission`, `Reporting`,
`Organization`, `Notification` abbreviate their corresponding `*IntegrationTest`
classes under `backend/services/*/src/test/java/`. `Event` means
`EventManagementIntegrationTest`. Browser evidence is under `output/playwright/`.
An exclusion applies to new failure rehearsals, not removal of existing tests.

| ID | Executable evidence / explicit limit |
| --- | --- |
| T01 | Phase 3 guest selection → login → booking; no reserve before login |
| T02 | DraftEventIntegrationTest.allowedDraftPersistsOnlyInOwningServiceAndRejectsForeignOrganization; Phase 1 foreign scope |
| T03 | Organization.staffAssignmentChecksEventOwnershipAndRevocationAndSuspensionImmediately; Phase 4 |
| T04 | Organization review plus Event.suspendedOrganizationCannotAllocateNewTickets; Phase 1 pending application; active scope checked before event submission |
| T05 | Discovery.privateAndCancelledCannotLeakOrBeResurrected; Phase 2 approval does not publish |
| T06 | Event.reviewRequiresCompleteDataLocksRevisionAndDoesNotAutoPublish |
| T07 | Event.capacityIsAtomicAcrossTicketTypesAndCannotDropBelowQuota |
| T08 | Event.publishedCoreIsLockedButPriceAndPausePreserveAllocations; Phase 2 invalid quota |
| T09 | Order.hundredSimultaneousBuyersCompeteForExactlyOneFinalTicket; Phase 6 HTTP barrier race |
| T10 | Order.invalidMixedSelectionDoesNotLeaveHoldOrOrder |
| T11 | Order.reserveSnapshotsAndReplayWithoutExtendingExpiry; new response-loss rehearsal excluded |
| T12 | Same native test; Phase 3 reload retains expiresAt |
| T13 | Order.expiryReleasesOnceAndRejectsFreeConfirmation; booking-models exact-deadline test; Phase 3 edges |
| T14 | Order.expiryAndConfirmationRaceConservesInventory |
| T15 | Order.heldOrderKeepsItsPriceAfterPublishedTicketPriceChanges |
| T16 | Order.freeConfirmAndReplayConsumeExactlyOnce; Phase 3 free order |
| T17 | Order.mixedFreeAndPaidOrderConfirmsEveryUnitForOnlyThePaidTotal; Phase 6 real payment → three tickets |
| T18 | order.component.spec: return query flags; Phase 3 forged success query |
| T19 | Payment.forgedSignatureCannotChangeState; existing regression only, no new security audit |
| T20 | Payment.concurrentCallbackDeliveriesHaveOneBusinessEffect; Order.simultaneousPaymentFactsConfirmOneOrderAndPublishOneFulfillment; Ticket.confirmedUnitsIssueExactlyOnceWithOpaqueStableQr |
| T21 | Payment.failureAfterSuccessNeverDowngrades; Order.paidSuccessDuplicateAndFailureAfterSuccessAreSafe |
| T22 | Order.lateCollectionNeverReacquiresReleasedStock; Phase 3 late callback |
| T23 | Order.secondDistinctCollectionDoesNotAllocateAgain |
| T24 | Order.wrongAmountAndCurrencyNeverConfirm; Payment.wrongAmountRetainsActualCollectionAndOneException; Phase 3 |
| T25 | Order.pendingPaymentBlocksCancelAndNewAttemptButSameKeyRecovers; accepted decision 0002 |
| T26 | Ticket.confirmedUnitsIssueExactlyOnceWithOpaqueStableQr; partial-crash rehearsal excluded |
| T27 | Notification.smtpFailureKeepsObligationForRetryWithoutOrderMutation; new outage rehearsal excluded |
| T28 | Ticket.simultaneousScansYieldOneFreshAdmissionAndStatsCountOnce |
| T29 | Ticket.requestReplayIsExplicitAndChangedPayloadConflicts; existing Phase 4 response-loss regression |
| T30 | Ticket.wrongEventAndInvalidQrDoNotExposeTicketOrAdmit; Phase 4 |
| T31 | Ticket.cancellationVoidsUnusedButPreservesUsedHistoryAndRemovesQr; Phase 4 cached QR |
| T32 | Order.cancelledEventAtomicallyClosesPendingOrdersAndLateSuccessIsException; Ticket.cancellationAndIssuanceRaceLeavesNoUsableTicket; Phase 4 |
| T33 | Payment.refundRequiresReviewEvidenceAndCorrectOutstandingAmount; Phase 3 missing evidence form |
| T34 | Commission.configChangeOnlyAffectsNewOrdersAndSameRequestReplaysOriginalSnapshot; Phase 5 held 5% / new 7% |
| T35 | Settlement.incompleteCancelledAndPendingPaymentEventsStayBlocked; missingFeeLegacySnapshotAndExceptionNeverBecomePayable |
| T36 | Settlement.concurrentRequestsCannotClaimTheEventTwice |
| T37 | Settlement.unknownCannotRetryUntilDefiniteFailureAndClaimIsRetained; existing Phase 5 regression |
| T38 | Settlement.successfulEvidenceSettlesExactlyOnceAndKeepsAudit; paidReferenceCannotBeReusedForAnotherEventSettlement |
| T39 | Settlement.sourceChangesAfterReviewBlockEvidenceAndDestinationNeedsExplicitRefresh; accepted decision 0005 |
| T40 | Reporting.snapshotPaginationAndCsvAgreeEvenWhenNewOrdersArrive; Phase 5 real CSV |
| T41 | Order.reminderRechecksWindowConfirmedOrderStartAndCancellation; Notification.reminderHasOneObligationPerRecipientAndRechecksCurrentEligibility |
| T42 | Order.lockedBuyerDoesNotDiscardVerifiedFundsFromAnExistingAttempt |
| T43 | Order.expiryReleasesOnceAndRejectsFreeConfirmation; lateCollectionNeverReacquiresReleasedStock; new worker-outage rehearsal excluded |
| T44 | Reporting.zeroSalesAndNoAttemptsHaveNoInventedSuccessRate; Phase 5 zero-sale fixture |
| T45 | Reporting.unmappedCashCountsGloballyWithoutInventingEventSalesOrLeakingIntoOrganizerReports; Phase 5 unmatched cash proof; extra collection is not a second sale |
| T46 | Settlement.missingFeeLegacySnapshotAndExceptionNeverBecomePayable; accepted decision 0005 |
| T47 | Payment.nanosecondEvidenceReplayPreservesOneReturnAndConflictingReuseFails |
| T48 | Existing outbox/notification tests remain; new crash-between-commit/publish rehearsal excluded |
| T49 | Order.confirmedOrderCannotBeCancelledOrReleaseSoldInventory |
| T50 | Ticket.cancellationAndScanRaceCannotCreateAdmissionAfterFence |

## UX traceability

| ID | Evidence |
| --- | --- |
| UX-01 | Phase 3 guest quantity and return destination preserved |
| UX-02 | Phase 6 two-browser race; losing buyer reloads zero available stock |
| UX-03 | Phase 3 same expiresAt after reload; booking-models monotonic countdown |
| UX-04 | order/payment-checkout specs and Phase 3 unknown/expired payment state |
| UX-05 | Phase 3 success query remains pending; order component regression |
| UX-06 | Phase 3 duplicate callback; Phase 6 three ticket IDs stable after refresh |
| UX-07 | Phase 3 edges late payment → exception, no ticket |
| UX-08 | Phase 3 free confirm; Phase 4 issuance |
| UX-09 | Existing ticket/notification tests; new failure rehearsal excluded |
| UX-10 | Phase 1 rejection → correction → approval |
| UX-11 | Phase 2 approved state remains private until publish |
| UX-12 | Phase 2 invalid quota retains form; Event native rejection |
| UX-13 | Ticket simultaneous scan test; Phase 4 camera/manual duplicate feedback |
| UX-14 | Phase 4 camera denied/manual lookup and offline no-admission |
| UX-15 | Phase 4 wrong event/cancelled ticket |
| UX-16 | Phase 4 cancellation stops allocation/admission, preserves history |
| UX-17 | Phase 1 foreign organization route/API; existing authorization regression |
| UX-18 | Phase 5 settlement eligibility + native exception guard |
| UX-19 | Existing Phase 5 ambiguous response regression; no new recovery rehearsal |
| UX-20 | Phase 5 old/new fee snapshots and audit |
| UX-21 | Session/native scope tests; Phase 6 logout clears private order view |
| UX-22 | Phase 6 320/390/768/1440px, keyboard, 200% root text scaling; browser-native zoom and screen-reader traversal are not certified |

## Reproduction and evidence

Follow `docs/local-development.md` for the isolated runtime and ownership. Native:
`./backend/gradlew -p backend test bootJar`,
`npm --prefix frontend run test:unit`, `npm --prefix frontend run build:all`.
After rebuilding, restart only the owned validation launcher. Execute browser
workflows sequentially when they share accounts, organization status or configuration:
Phase 1 → Phase 2 → prepare/run Phase 3 → prepare/run Phase 3 edges → Phase 4 →
payment methods → Phase 5 → unmatched cash → `python3 scripts/run-phase6-browser.py`.

Phase 6 fixture and raw browser logs are ignored/protected; no credentials belong
in the acceptance report. Final counts, findings, retained warnings and observed
limits are recorded in `docs/qa/phase6-local-acceptance.md` after execution.
