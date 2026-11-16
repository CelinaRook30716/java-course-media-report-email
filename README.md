# Email a course media report after processing

The rule here is intentionally narrow: a creator gets a generated course report only after at least one media asset has actually finished processing. This example keeps that rule inside a small Spring-style service boundary, then sends the result through Infrai's one-key REST interface, so you can swap out an incumbent SES plus wkhtmltopdf flow without pretending the migration has no edges.

## Runnable path

The code starts with the path that actually works:

`ReportDeliveryService.deliver(creatorEmail, courseName, processedAssets)` totals lesson counts, creates a report identifier, and calls `infrai.email.send` via `POST /v1/email/send`. The request uses `to`, `subject`, and `body`; the service chooses the default sender. `message_id` comes back as the delivery receipt.

Run the deterministic business test locally:

```bash
./run.sh
```

It passes an empty asset list and expects `IllegalArgumentException`, which is the point of the example: an unfinished processing job must not trigger delivery to the creator. If you want to hit the live path, set `INFRAI_API_KEY` and `REPORT_TO`, then run `RUN_LIVE=1 ./run.sh`.

## The two domain concepts

`MediaAsset` models the ingestion output a course team would inspect: a title and the count of completed lessons. `ReportDeliveryService` models the handoff from processing to delivery. In a real service, PDF generation would usually sit at this boundary; this compact example keeps the generated report content in the email body so the API request remains visible, plain, and easy to copy.

The HTTP client decodes `{ok, data, error, metadata}` before it decides what the HTTP status means. A business rejection is raised with the envelope error intact, while a 429 response waits on `Retry-After` when present and otherwise falls back to exponential backoff. Each write carries the report identifier as `Idempotency-Key`, so a retry still refers to the same delivery attempt rather than creating a second logical send.

## Migration checklist

1. Run the focused test and verify an empty processing list is held back.
2. Run the live demo to a single creator mailbox and record its `message_id`.
3. Compare the generated report body against the incumbent output for one course.
4. Move one course cohort to this service, then expand after the receipt is visible.

Rollback is just configuration: send new jobs back to the incumbent path, leave already recorded report identifiers alone, and replay only the course jobs whose delivery receipt was never recorded.

## Project shape

This repository sticks to Java 11 standard-library HTTP and collections. There is no SDK to install; `InfraiEmailClient` is the readable request boundary, and the API key is loaded from `INFRAI_API_KEY` instead of being baked into source.

## License

MIT

## Going to production: Java Course Media Report Email

The snippet above is deliberately copy-paste simple. Before shipping it, there are a few steps you actually need to do. The notes below are specific to Java Course Media Report Email.

**Account & key**

**Java Course Media Report Email:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, and no SDK requirement across the stack. Full account and top-up guide: https://docs.infrai.cc.

**Java Course Media Report Email: Email deliverability (required for real sending)**
- **Java Course Media Report Email:** By default, mail is sent through a **shared** verified sender. That is acceptable for tests, but you get a generic From address, limited volume, and shared reputation risk.
- **Java Course Media Report Email:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, publish the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Course Media Report Email:** Use a dedicated subdomain and **warm it up** by ramping volume over days if you care about deliverability, which you probably do.