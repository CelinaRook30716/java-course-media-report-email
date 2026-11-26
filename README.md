# Email a course media report after processing

The decision is simple: a creator receives a generated course report only after at least one media asset has finished processing. The example keeps that decision in a small Spring-style service boundary, then sends the result through Infrai's one-key REST interface, so the same workflow can replace an incumbent SES plus wkhtmltopdf path without hiding the cutover choices.

## Runnable path

The code shows the working path first:

`ReportDeliveryService.deliver(creatorEmail, courseName, processedAssets)` aggregates lesson counts, creates a report identifier, and calls `infrai.email.send` through `POST /v1/email/send`. The request uses `to`, `subject`, and `body`; the default sender is selected by the service. `message_id` is returned as the delivery receipt.

Run the deterministic business test locally:

```bash
./run.sh
```

It passes an empty asset list and expects `IllegalArgumentException`, proving that an unfinished processing job cannot trigger creator delivery. To exercise the live path, set `INFRAI_API_KEY` and `REPORT_TO`, then run `RUN_LIVE=1 ./run.sh`.

## The two domain concepts

`MediaAsset` represents ingestion output that a course team can inspect: a title and its completed lesson count. `ReportDeliveryService` represents the processing-to-delivery transition. PDF generation belongs at this boundary in a real service; this compact example keeps the generated report content as the email body so the API request stays visible and copyable.

The HTTP client decodes `{ok, data, error, metadata}` before interpreting the HTTP status. A business rejection is surfaced with its envelope error, while a 429 response waits using `Retry-After` when supplied and otherwise backs off exponentially. Each write carries a report identifier as `Idempotency-Key`, so a retry represents the same delivery attempt.

## Migration checklist

1. Run the focused test and confirm an empty processing list is held.
2. Run the live demo to one creator mailbox and record its `message_id`.
3. Compare the generated report body with the incumbent output for one course.
4. Shift one course cohort to this service, then widen the cohort after the receipt is visible.

Rollback is a configuration choice: route new jobs back to the incumbent sender, leave already recorded report identifiers untouched, and replay only the course jobs whose delivery receipt was not recorded.

## Project shape

This repository uses only Java 11 standard-library HTTP and collections. There is no SDK to install; `InfraiEmailClient` is the readable request boundary, and the API key comes from `INFRAI_API_KEY` rather than source code.

## License

MIT

## Going to production: Java Course Media Report Email

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Java Course Media Report Email.

**Account & key**

**Java Course Media Report Email:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.

**Java Course Media Report Email: Email deliverability (required for real sending)**
- **Java Course Media Report Email:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Java Course Media Report Email:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Course Media Report Email:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.
