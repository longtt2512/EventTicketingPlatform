# Accepted local QR/card payment experience

Date: 2026-11-20
Status: Accepted by owner in conversation

The owner requests normal customer-facing checkout copy and QR/card methods,
without a VNPAY integration. QR must display an expiring code and receive an
automatic outcome after 3–5 seconds. Cards use a fixed configurable allowlist.
A database configuration table must control per-method outcomes and failure
reasons, including QR_PAYMENT set to failed or a JSON configuration.

This replaces decision 0002's requirement to label every customer payment as
simulated and expose scenario selection. Customer-facing pages and transaction
emails use normal payment language. Operational/testing tools may retain precise
local-provider terminology. No external gateway, money movement or production
payment capability is introduced.

Existing authority remains: immutable price/hold snapshots, ten-minute holds,
backend-verified receipt processing, retry only after confirmed failure, and
idempotent order/ticket effects. Reload must not extend a payment lifetime.

Implementation uses JSONB configuration under Payment ownership, captured when
the method is submitted. Default outcome delay is four seconds; QR validity uses
the remaining hold (configurable shorter lifetime). Configured expiry cannot
extend the order hold. Failure codes/messages are captured with the session.
Configured sample card numbers are development fixtures; customer submissions
are never logged or persisted in full. No CVV or card expiry is requested.
Customer credentials cannot invoke the separate legacy scenario-selection API.
