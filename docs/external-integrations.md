# External integrations and v2 boundaries

LinePulse keeps the operational core functional without depending on third-party credentials. Features that require durable external delivery or storage are isolated from the core domain so they can be added without rewriting incident, maintenance or audit flows.

## Attachments

A production attachment feature should use durable object storage rather than the application container filesystem. Render free instances are not an appropriate durable file store for operational photos, manuals or PDFs.

Recommended flow:

```text
Browser -> LinePulse API -> signed object-storage upload
                         -> attachment metadata in PostgreSQL
```

Attachment metadata should reference the related entity (`INCIDENT`, `WORK_ORDER` or `MACHINE`), object key, original filename, media type, size, uploader registration and creation timestamp.

Until object-storage credentials are configured, LinePulse intentionally does not expose a fake upload button that would lose files after an instance restart.

## AI-assisted incident classification

The incident domain already records the information an AI suggestion layer would need:

- free-text title and description;
- technical category;
- priority;
- machine context;
- root cause;
- applied solution.

A future AI endpoint should return suggestions rather than mutate an incident directly. The operator or technician remains responsible for accepting or changing the proposed category and priority.

Recommended response contract:

```json
{
  "suggestedCategory": "MECHANICAL",
  "suggestedPriority": "HIGH",
  "inspectionAreas": ["bearings", "lubrication"],
  "reason": "..."
}
```

No model/provider is hard-coded until a provider and credential are intentionally selected.

## External notifications

The internal alert service already derives active operational risks from incidents, downtime, work orders and preventive maintenance plans. A future delivery adapter can consume those alerts and send them through webhook, e-mail or messaging without moving alert rules out of the domain service.

External delivery should persist a delivery key/status before retries to avoid sending the same alert repeatedly.

## Observability

Before higher-volume production use, add structured application logs, request correlation IDs, health/metrics collection and alerting for API/database failures. These concerns are independent of the operational domain and can be introduced without changing user workflows.
