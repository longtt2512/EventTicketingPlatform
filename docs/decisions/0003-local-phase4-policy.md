# Accepted local Phase 4 ticket fulfillment and admission policy

Date: 2026-11-20
Status: Accepted by the repository owner in conversation

The owner explicitly accepted this rule bundle and instructed implementation.
This authorizes local Phase 4 development and validation, not deployment.

- Check-in is online, in the half-open window `[event.startAt - 2h, event.endAt)`.
- Tickets display the buyer's name; no separate attendee collection form is added.
- Static QR credentials are random and contain no personal data. Only the owner
  may retrieve the credential or download the personal ticket. Used/voided ticket
  PDFs display their status and omit the QR. Email links require login to view or
  download tickets; email does not distribute QR credentials or PDF attachments.
- Suspended organizations and revoked staff cannot check in. Existing admission
  history is retained. Current account, membership and event scope are checked.

PRD ETK-01–10 and CHK-01–10 define scope. BRS §§10–11 require recoverable
unit-level issuance, one admission per ticket, no offline success, and no usable
ticket after cancellation. SDD §6.3 owns the cross-service cancellation fence.
The existing separate-schema, authenticated REST, JSON Kafka outbox/inbox and
private MinIO architecture is retained. A copied static QR is not identity proof.

Local credential storage uses a SHA-256 lookup hash and encrypted recoverable
material under the existing environment-owned encryption key. Current event/time
and status determine validity; rendering never rotates the credential. Production
key rotation, retention and abuse quotas are not established by this decision.
