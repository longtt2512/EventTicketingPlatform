# Accepted local Phase 3 booking and simulated payment policy

Date: 2026-11-20
Status: Accepted by the repository owner in conversation

The owner authorized Phase 3 implementation using a local mock payment gateway,
then explicitly accepted the proposed local rule bundle. This is local behavior,
not a selection of a real payment provider or production/financial policy.

- PRD ORD-01–15 and PAY-01–13 define scope. A reservation lasts ten minutes by
  backend time; reload, retry and changing tabs do not extend it.
- Pending order lines and their integer VND price snapshots are immutable.
  Changing selection cancels a safely cancellable order and creates a new one.
  Customers pay the sum of ticket prices, with no additional local fees.
- Pending/unknown payment blocks a new attempt and customer cancellation.
  Confirmed failure permits a new attempt while the original hold remains valid.
- The local mock offers success, failure, unknown/delayed outcome, repeated
  callbacks, late success and wrong amount. It never collects money or card data.
  Customer UI labeling and method selection are superseded by decision 0004. Only the provider is simulated;
  orders, atomic holds, expiry, history and reconciliation use real persistence.
- Browser returns are non-authoritative. Verified backend evidence can confirm
  only an active, unexpired hold, with matching reference/amount/currency.
  Late or mismatched collection is an exception and never reacquires inventory.
- Admin reviews exceptions and records simulated return evidence. No actual bank
  transfer or gateway refund occurs. Outstanding funds cannot be resolved merely
  by clicking a button; evidence, amount and a unique return reference are required.
- QR issuance/check-in remain Phase 4. Confirmation persists a recoverable order
  fact for that integration, without inventing tickets or attendees.

Architecture authority: SDD §§4–6 (separate Payment database, REST commands,
Kafka facts, outbox/inbox). BRS §§8–9 are the state/invariant authority. Local
implementation details (HMAC mock contract, durable request keys, bounded workers)
are documented with the Phase 3 contract and execution plan.
