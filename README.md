# Praxicraft Assess Java SDK

Official Java client for the **[Praxicraft Assess](https://assess.praxicraft.com)** Public API.

Use it to invite candidates, check invite quota, manage webhooks, enroll hiring pipelines, and fetch results from your ATS, backend, or automation scripts.

Published on [GitHub Packages](https://github.com/praxicraft-platform/praxicraft-java/packages) as `com.praxicraft:assess`.

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/praxicraft-platform/praxicraft-java</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.praxicraft</groupId>
  <artifactId>assess</artifactId>
  <version>1.0.0</version>
</dependency>
```

Authenticate to GitHub Packages with a PAT that has `read:packages` (server id `github` in `~/.m2/settings.xml`).

**Requires Java 17+.** Full API reference: [docs.praxicraft.com/sdks/java](https://docs.praxicraft.com/sdks/java)

## Table of Contents

- [Authentication](#authentication)
- [Quickstart](#quickstart)
- [What you can do](#what-you-can-do)
  - [Check invite quota before bulk sends](#check-invite-quota-before-bulk-sends)
  - [Bulk invites](#bulk-invites)
  - [Build and activate an assessment via API](#build-and-activate-an-assessment-via-api)
  - [Register and test a webhook](#register-and-test-a-webhook)
  - [Enroll into a hiring pipeline](#enroll-into-a-hiring-pipeline)
  - [Paginate cohort results](#paginate-cohort-results)
  - [Verify webhook signatures](#verify-webhook-signatures)
- [Errors](#errors)
- [Requirements & support](#requirements--support)
- [License](#license)

---

## Authentication

Create an organisation API key in Assess:

**Assess → Developer → API Keys** → create key → copy `ct_live_…` (shown once).

```bash
export PRAXICRAFT_API_KEY="ct_live_xxxxxxxxxxxxxxxx"
```

Or pass the key when constructing the client:

```java
import com.praxicraft.assess.Client;

Client client = Client.builder()
    .apiKey("ct_live_xxxxxxxxxxxxxxxx")
    .build();
```

Optional: override the API host with `PRAXICRAFT_API_BASE_URL` or `Client.builder().baseUrl("...")`.
Default host: `https://assess.praxicraft.com`.

Never commit API keys. Prefer environment variables or a secrets manager.

Scopes and rotation: [Authentication](https://docs.praxicraft.com/authentication)

---

## Quickstart

```java
import com.praxicraft.assess.Client;

import java.util.List;
import java.util.Map;

public class Main {
  @SuppressWarnings("unchecked")
  public static void main(String[] args) {
    Client client = new Client(); // reads PRAXICRAFT_API_KEY

    Map<String, Object> page = client.assessments().list(null);
    List<Map<String, Object>> results = (List<Map<String, Object>>) page.get("results");
    for (Map<String, Object> assessment : results) {
      System.out.println(assessment.get("slug") + " " + assessment.get("status"));
    }

    // Invite a candidate (idempotent on email — safe to retry)
    Map<String, Object> invite = client.invites().create(
        "senior-backend-screen",
        Map.of(
            "email", "candidate@example.com",
            "name", "Jane Doe",
            "send_email", true
        )
    );
    System.out.println(invite.get("invite_token") + " " + invite.get("invite_url"));

    Map<String, Object> result = client.results().retrieve((String) invite.get("invite_token"));
    System.out.println(result);
  }
}
```

Responses are **flat JSON** (same shape as the Public API — no `{ "data": … }` wrapper).

---

## What you can do

| Resource | Common methods |
|----------|----------------|
| `client.org()` | `retrieve()`, `stats()` |
| `client.assessments()` | `list()`, `retrieve()`, `create()`, `update()`, `activate()`, `listTasks()`, `attachTasks()`, `replaceTasks()`, `removeTask()` |
| `client.invites()` | `create()`, `bulkCreate()`, `list()`, `retrieve()`, `remind()`, `cancel()` |
| `client.results()` | `list()`, `retrieve()`, `iterAll()` |
| `client.webhooks()` | `list()`, `create()`, `retrieve()`, `update()`, `delete()`, `test()`, `deliveries()` |
| `client.pipelines()` | `list()`, `retrieve()`, `enroll()`, `bulkEnroll()`, `listEnrollments()`, `getEnrollment()` |
| `Webhooks.verifySignature` | Verify `X-Praxicraft-Signature` on webhook payloads |

All paths target `/api/v1/public/…` on the Assess host.

### Check invite quota before bulk sends

```java
Map<String, Object> org = client.org().retrieve();
Number remaining = (Number) org.getOrDefault("invites_remaining", 0);
if (remaining.intValue() < candidates.size()) {
  throw new IllegalStateException("Not enough invites remaining this month");
}
```

### Bulk invites

```java
client.invites().bulkCreate(
    "senior-backend-screen",
    List.of(
        Map.of("email", "a@example.com", "name", "Alex"),
        Map.of("email", "b@example.com", "name", "Blair")
    ),
    Map.of("send_email", true)
);
```

### Build and activate an assessment via API

```java
Map<String, Object> assessment = client.assessments().create(Map.of("title", "Backend screen"));
String slug = (String) assessment.get("slug");
client.assessments().attachTasks(
    slug,
    Map.of(
        "tasks",
        List.of(Map.of("task_id", "<platform-or-org-task-uuid>", "source", "platform"))
    )
);
client.assessments().activate(slug);
```

### Register and test a webhook

```java
Map<String, Object> hook = client.webhooks().create(Map.of(
    "url", "https://example.com/hooks/praxicraft",
    "events", List.of("assessment.completed", "candidate.passed")
));
// Store hook.get("secret_key") (whsec_…) — shown once
String id = (String) hook.get("id");
client.webhooks().test(id);
client.webhooks().update(id, Map.of("is_active", true));
```

### Enroll into a hiring pipeline

```java
Map<String, Object> enrollment = client.pipelines().enroll(
    "grad-2025",
    Map.of(
        "email", "alex@example.com",
        "name", "Alex Lee",
        "send_email", true
    )
);
Map<String, Object> status = client.pipelines().getEnrollment((String) enrollment.get("enrollment_id"));
```

### Paginate cohort results

```java
for (Object rowObj : client.results().iterAll("senior-backend-screen", Map.of("page_size", 50))) {
  @SuppressWarnings("unchecked")
  Map<String, Object> row = (Map<String, Object>) rowObj;
  System.out.println(row.get("email") + " " + row.get("score_percentage") + " " + row.get("passed"));
}
```

### Verify webhook signatures

Assess signs the **raw request body** with your webhook secret (`whsec_…`):

```java
import com.praxicraft.assess.Webhooks;

boolean ok = Webhooks.verifySignature(secret, rawBody, signatureHeader);
```

Header format: `X-Praxicraft-Signature: sha256=<hex>`

Event catalog and payload examples: [Webhooks](https://docs.praxicraft.com/webhooks)

---

## Errors

Public API errors look like:

```json
{
  "error": {
    "code": "INSUFFICIENT_SCOPE",
    "message": "This API key does not have the 'candidates:read' scope."
  }
}
```

The SDK raises typed exceptions. **Branch on `getErrorCode()`**, not the message text:

```java
import com.praxicraft.assess.exception.AuthenticationException;
import com.praxicraft.assess.exception.InsufficientScopeException;
import com.praxicraft.assess.exception.RateLimitException;
import com.praxicraft.assess.exception.ValidationException;

try {
  client.invites().create("demo", Map.of("email", "candidate@example.com"));
} catch (ValidationException e) {
  System.out.println(e.getErrorCode() + " " + e.getDetails());
} catch (InsufficientScopeException e) {
  System.out.println(e.getErrorCode());
} catch (AuthenticationException e) {
  System.out.println(e.getErrorCode());
} catch (RateLimitException e) {
  System.out.println(e.getRetryAfter());
}
```

Error codes: [Errors](https://docs.praxicraft.com/errors)

---

## Requirements & support

- Java **17** or **21+**
- Runtime dependency: [Jackson databind](https://github.com/FasterXML/jackson)
- Product docs: [docs.praxicraft.com](https://docs.praxicraft.com)
- Issues: [GitHub Issues](https://github.com/praxicraft-platform/praxicraft-java/issues)

---

## License

[MIT](LICENSE)
