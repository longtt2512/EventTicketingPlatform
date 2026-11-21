# QR/card checkout configuration

The customer flow is **Thanh toán → QR or card → payment result → tickets**.
This is the local provider authorized in [decision 0004](decisions/0004-configurable-checkout-methods.md),
with no VNPAY or external bank integration. Customer UI and emails use normal
payment language. QR contains an opaque local session reference, not a bank
transfer destination. Existing order/receipt/ticket processing remains real.

## Configuration table

Database: `event_ticketing`; table: `payment.mock_config`.
The `key` column identifies a method; `value` is JSONB.

| Key | Default | Purpose |
| --- | --- | --- |
| `QR_PAYMENT` | SUCCESS, 4 seconds, 600-second maximum lifetime | QR result, delay, expiry and error |
| `CARD_PAYMENT` | SUCCESS, 4 seconds, configured sample cards | Card result and allowlist |

The QR deadline is the earlier of the configured lifetime and the original
order hold. No reload/retry extends that hold. If a scheduled result would arrive
at or after the method deadline, the provider returns an expiry failure instead.

Configuration is read **when the customer submits the chosen method**. SQL edits
apply without restart to subsequent submissions. A session already processing
retains its original outcome, delay, failure reason and QR. Invalid/missing
configuration rejects the submission without scheduling a result.

Connect to the isolated local database:

```sh
docker compose -f infra/validation/docker-compose.yml exec postgres psql -U postgres -d event_ticketing
```

Inspect configuration:

```sql
SELECT key, jsonb_pretty(value), updated_at FROM payment.mock_config ORDER BY key;
```

Make new QR payments fail using a simple string:

```sql
UPDATE payment.mock_config
SET value = to_jsonb('failed'::text), updated_at = clock_timestamp()
WHERE key = 'QR_PAYMENT';
```

Or specify the delay, lifetime and customer-facing reason:

```sql
UPDATE payment.mock_config
SET value = '{
  "status": "FAILED",
  "delaySeconds": 4,
  "lifetimeSeconds": 120,
  "failure": {
    "code": "QR_NETWORK_BUSY",
    "message": "Kết nối thanh toán đang bận. Vui lòng thử lại sau."
  }
}'::jsonb, updated_at = clock_timestamp()
WHERE key = 'QR_PAYMENT';
```

Restore default QR success:

```sql
UPDATE payment.mock_config
SET value = '{"status":"SUCCESS","delaySeconds":4,"lifetimeSeconds":600}'::jsonb,
    updated_at = clock_timestamp()
WHERE key = 'QR_PAYMENT';
```

To observe expiry, set `lifetimeSeconds` to 2 and `delaySeconds` to 4. For a longer
QR preview, use a delay such as 20 with a lifetime such as 120.
`status` accepts `SUCCESS` or `FAILED` (case-insensitive); delay is an integer
0–300 seconds and lifetime an integer 1–600 seconds. The normal default is four
seconds. Failure codes are at most 80 characters and messages 500 characters.
Keep messages appropriate for customers; do not put internal secrets in them.

## Configured sample cards

Only these seeded sample numbers are recognized. No real card issuer is contacted.

| Number | Default outcome |
| --- | --- |
| `4242 4242 4242 4242` | Success |
| `5555 5555 5555 4444` | Success |
| `4000 0000 0000 0002` | Failure: insufficient funds |

Any other 16-digit number fails with `CARD_NOT_SUPPORTED`. Inputs accept spaces;
short or malformed input is rejected before scheduling. Expiry/CVV are not
requested. Only the last four submitted digits are retained in the session;
full sample numbers exist only in the operator-controlled configuration allowlist.

Fail all recognized cards, while preserving the allowlist:

```sql
UPDATE payment.mock_config
SET value = jsonb_set(value, '{status}', '"FAILED"'::jsonb),
    updated_at = clock_timestamp()
WHERE key = 'CARD_PAYMENT';
```

Change `FAILED` back to `SUCCESS` to restore per-card behavior. Each object in
`CARD_PAYMENT.value.cards` has `number`, `status`, and optional `failure` with
`code`/`message`. A top-level FAILED overrides per-card success. Keep the cards
array when editing this configuration; a bare string is supported for QR only.

## Runtime and proof

Session API: authenticated owner-only `GET /api/v1/payment-sessions/{id}` and
`POST /api/v1/payment-sessions/{id}/method` with `{ "method": "QR" }` or
`{ "method": "CARD", "cardNumber": "4242424242424242" }`.
Clients cannot send the outcome, amount or delay. The session response contains
server time, expiry and a generated QR image while valid. A browser countdown
never confirms a payment. Durable scheduled provider receipts drive the existing
signed verification/outbox flow, even if the browser closes.

The old `/payment/mock/:id` links redirect to the new checkout. Legacy scenario
API is admin-only test tooling for duplicate/late/mismatched callback proof;
it is absent from customer screens.

```sh
python3 scripts/prepare-phase3-fixture.py
python3 scripts/run-payment-methods-browser.py
```

This workflow uses synthetic fixture accounts and temporarily overrides both
configuration keys. It restores their original values and timestamps in `finally`.
Do not run other payment browser suites concurrently with it. If the process is
forcibly terminated, restore from protected
`.runtime/phase1/checkout-original-config.json` before further checkout tests.
Results and responsive screenshots: `output/playwright/payment-methods-*`.
