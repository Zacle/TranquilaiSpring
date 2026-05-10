# TranquilAI Spring Backend

Spring/Kotlin microservices backend for the TranquilAI Android app. The system is organized around an API gateway, independently owned services, per-service persistence, internal service-to-service APIs, and subscription entitlements.

## Architecture

```text
Android app / web clients
        |
        v
api-gateway :8080
Spring Cloud Gateway, CORS, JWT validation, auth rate limiting
        |
        | adds X-User-Id, X-User-Email, X-User-Roles
        v
+----------------------+----------------------+----------------------+
| auth-service :8081   | user-service :8082   | ai-service :8083     |
| PostgreSQL + Redis   | PostgreSQL           | MongoDB + RabbitMQ   |
+----------------------+----------------------+----------------------+
| content-service :8084| activity-service:8085| plan-service :8086   |
| PostgreSQL           | PostgreSQL + RabbitMQ| PostgreSQL + Gemini  |
+----------------------+----------------------+----------------------+
| progress-service:8087| notification-svc:8088| subscription-svc:8089|
| PostgreSQL           | PostgreSQL + FCM     | PostgreSQL + Redis   |
+----------------------+----------------------+----------------------+
```

Internal APIs use `X-Internal-Key` and `/internal/**`. The gateway blocks `/internal/**` from external clients.

## Services

| Service | Port | Storage | Responsibility |
| --- | ---: | --- | --- |
| api-gateway | 8080 | Redis for rate limiting | Public entry point, route forwarding, JWT validation, CORS, auth rate limits |
| auth-service | 8081 | PostgreSQL, Redis | Registration, login, email verification, password reset, JWT and refresh tokens |
| user-service | 8082 | PostgreSQL | User profile, mental-health profile, settings, emergency contacts, account deletion |
| ai-service | 8083 | MongoDB, RabbitMQ | Conversations, journal AI, insights, AI-generated content, durable chat-started side effects |
| content-service | 8084 | PostgreSQL | Affirmations, breathing exercises, meditation content, journal prompts, audio/content assets |
| activity-service | 8085 | PostgreSQL, RabbitMQ | Mood, journal, breathing sessions, meditation sessions, affirmation views, analytics, durable activity side effects |
| plan-service | 8086 | PostgreSQL | Daily wellness plans and plan activity completion |
| progress-service | 8087 | PostgreSQL | Stats, streaks, badges, growth insights, progress counters |
| notification-service | 8088 | PostgreSQL | FCM device tokens, reminder schedules, notification history, push delivery |
| subscription-service | 8089 | PostgreSQL, Redis | Free/premium subscriptions, Stripe web checkout, Google Play verification, invoices, entitlements, usage metering |

## Infrastructure

| Component | Port | Purpose |
| --- | ---: | --- |
| PostgreSQL 16 | 5432 | Relational data for service-owned databases |
| MongoDB 7 | 27017 | AI conversations and flexible AI documents |
| Redis 7 | 6379 | Auth tokens/codes, gateway rate limiting, subscription cache |
| RabbitMQ 4 | 5672 / 15672 | Durable side-effect queues for activity and AI service events |
| Mailpit | 1025 / 8025 | Local SMTP capture and email web UI |

PostgreSQL databases created by `scripts/init-db.sql`:

```text
tranquilai_auth
tranquilai_users
tranquilai_content
tranquilai_activity
tranquilai_plans
tranquilai_progress
tranquilai_notifications
tranquilai_subscriptions
```

## Gateway Routes

All public traffic should go through `http://localhost:8080`.

| Route | Target | Auth |
| --- | --- | --- |
| `/api/auth/**` | auth-service | Public, rate limited by remote IP |
| `/api/users/**` | user-service | JWT |
| `/api/conversations/**` | ai-service | JWT |
| `/api/insights/**` | ai-service | JWT |
| `/api/ai/journal/**` | ai-service | JWT |
| `/api/affirmations/**` | content-service | JWT |
| `/api/journal-prompts/**` | content-service | JWT |
| `/api/meditation/**` | content-service | JWT |
| `/api/breathing/**` | content-service | JWT |
| `/api/mood/**` | activity-service | JWT |
| `/api/journal/**` | activity-service | JWT |
| `/api/breathing-sessions/**` | activity-service | JWT |
| `/api/meditation-sessions/**` | activity-service | JWT |
| `/api/affirmation-views/**` | activity-service | JWT |
| `/api/analytics/**` | activity-service | JWT |
| `/api/plans/**` | plan-service | JWT |
| `/api/progress/**` | progress-service | JWT |
| `/api/notifications/**` | notification-service | JWT |
| `/api/subscriptions/**` | subscription-service | JWT |
| `/api/webhooks/**` | subscription-service | Public; Stripe signature is verified by subscription-service |
| `/internal/**` | blocked | Returns 404 at the gateway |

## Subscription Architecture

The backend supports both Stripe and Google Play, with platform-specific purchase rules:

- Web/backend checkout uses Stripe through `subscription-service`.
- Android subscriptions are verified through Google Play purchase tokens.
- Stripe webhooks arrive at `/api/webhooks/**` and are not JWT-protected; Stripe signatures are verified in the service.
- Google Play purchases are submitted by the Android app to `/api/subscriptions/verify-play-purchase`.
- Entitlement and usage checks are exposed internally under `/internal/subscriptions/**`.
- Free usage limits are configured with `SUBSCRIPTION_FREE_AI_CHAT_LIMIT` and `SUBSCRIPTION_FREE_JOURNAL_LIMIT`.

User-facing subscription endpoints:

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/subscriptions/current` | Current subscription |
| GET | `/api/subscriptions/plans` | Available plans |
| POST | `/api/subscriptions/checkout` | Create Stripe checkout session |
| POST | `/api/subscriptions/verify-play-purchase` | Verify and activate Google Play purchase |
| POST | `/api/subscriptions/cancel` | Cancel Stripe subscription at period end |
| POST | `/api/subscriptions/reactivate` | Reactivate canceled Stripe subscription |
| GET | `/api/subscriptions/portal` | Billing portal URL or provider-specific management message |
| GET | `/api/subscriptions/invoices` | Invoice history |
| GET | `/api/subscriptions/usage` | Current usage summary keyed by feature, such as `AI_CHAT` and `JOURNAL_ENTRY` |

Internal subscription endpoints:

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/internal/subscriptions/entitlement?userId=&feature=` | Check feature entitlement |
| GET | `/internal/subscriptions/usage?userId=&feature=` | Check metered feature usage |
| POST | `/internal/subscriptions/usage/increment` | Increment feature usage |
| GET | `/internal/subscriptions/user/{userId}` | Fetch or create user's free subscription |

## Technology Stack

| Layer | Technology |
| --- | --- |
| Language | Kotlin 2.0.21 |
| Runtime | Java 21 |
| Framework | Spring Boot 3.4.1 |
| Gateway | Spring Cloud Gateway 2024.0.0 |
| Persistence | Spring Data JPA, Hibernate, Flyway, PostgreSQL |
| Document storage | Spring Data MongoDB |
| Cache | Redis |
| Message broker | RabbitMQ |
| AI provider config | Gemini API via `GEMINI_API_KEY` and `GEMINI_BASE_URL` |
| Security | Spring Security, JJWT 0.12.6, internal service key |
| Email | Spring Mail with Mailpit for local development |
| Push | Firebase Admin SDK / FCM |
| Payments | Stripe, Google Play server-side verification |
| Build | Maven multi-module project |
| Containers | Docker Compose |

## Project Structure

```text
tranquilai-spring/
|-- pom.xml
|-- docker-compose.yml
|-- .env.example
|-- scripts/
|   `-- init-db.sql
|-- api-gateway/
|-- auth-service/
|-- user-service/
|-- ai-service/
|-- content-service/
|-- activity-service/
|-- plan-service/
|-- progress-service/
|-- notification-service/
`-- subscription-service/
```

Each service follows the same general layout:

```text
[service]/
|-- pom.xml
|-- Dockerfile
`-- src/main/kotlin/com/tranquilai/[service]/
    |-- [Service]Application.kt
    |-- config/
    |-- controller/
    |-- dto/
    |-- entity/
    |-- repository/
    |-- security/
    |-- service/
    `-- exception/
```

## Environment Variables

Copy `.env.example` to `.env` and provide real secrets before running the full stack.

| Variable | Required | Used by | Description |
| --- | --- | --- | --- |
| `JWT_SECRET` | Yes | api-gateway, auth-service | HMAC secret for JWT signing and validation |
| `GEMINI_API_KEY` | Yes for AI | ai-service, plan-service | Gemini API key |
| `GEMINI_BASE_URL` | Yes for AI | ai-service, plan-service | Gemini base URL |
| `DB_PASSWORD` | No | Docker/local DB config | PostgreSQL password override |
| `REDIS_PASSWORD` | No | auth, gateway, subscription | Redis password override |
| `RABBITMQ_USERNAME` | No | Docker/local broker config | RabbitMQ username override |
| `RABBITMQ_PASSWORD` | No | Docker/local broker config | RabbitMQ password override |
| `INTERNAL_SERVICE_KEY` | Yes outside local dev | service-to-service APIs | Shared internal API secret |
| `SPRING_MAIL_HOST` / `SPRING_MAIL_PORT` | No | auth-service | SMTP host and port |
| `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | No | auth-service | SMTP credentials |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Required for push | notification-service | Firebase service account JSON |
| `STRIPE_SECRET_KEY` | Required for Stripe | subscription-service | Stripe secret key |
| `STRIPE_WEBHOOK_SECRET` | Required for Stripe webhooks | subscription-service | Stripe webhook signing secret |
| `STRIPE_PREMIUM_MONTHLY_PRICE_ID` | Required for Stripe checkout | subscription-service | Monthly Stripe price ID |
| `STRIPE_PREMIUM_ANNUAL_PRICE_ID` | Required for Stripe checkout | subscription-service | Annual Stripe price ID |
| `STRIPE_BILLING_PORTAL_RETURN_URL` | No | subscription-service | Return URL after Stripe portal |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Required for Play verification | subscription-service | Google Play service account JSON |
| `GOOGLE_PLAY_PACKAGE_NAME` | Required for Play verification | subscription-service | Android application package name |
| `SUBSCRIPTION_TRIAL_DAYS` | No | subscription-service | Trial duration |
| `SUBSCRIPTION_FREE_AI_CHAT_LIMIT` | No | subscription-service | Free daily AI chat limit |
| `SUBSCRIPTION_FREE_JOURNAL_LIMIT` | No | subscription-service | Free daily journal limit |

## Quick Start

### Prerequisites

- Docker Desktop
- Java 21
- Maven, or IntelliJ IDEA's bundled Maven

On this Windows setup, Android Studio's bundled JBR is Java 21:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
```

### Configure Environment

```powershell
Copy-Item .env.example .env
```

Fill in production secrets before using any non-local environment. Do not commit `.env`.

### Start Infrastructure

```powershell
docker compose up postgres mongodb redis rabbitmq mailpit -d
```

### Start All Services

```powershell
docker compose up --build
```

### Health Checks

```text
http://localhost:8080/actuator/health  api-gateway
http://localhost:8081/actuator/health  auth-service
http://localhost:8082/actuator/health  user-service
http://localhost:8083/actuator/health  ai-service
http://localhost:8084/actuator/health  content-service
http://localhost:8085/actuator/health  activity-service
http://localhost:8086/actuator/health  plan-service
http://localhost:8087/actuator/health  progress-service
http://localhost:8088/actuator/health  notification-service
http://localhost:8089/actuator/health  subscription-service
http://localhost:8025                  Mailpit UI
http://localhost:15672                 RabbitMQ management UI
```

## Testing

Use Java 21 for tests. Java 17 cannot run classes compiled for this project.

With global Maven:

```powershell
mvn test
mvn -pl api-gateway test
```

With IntelliJ's bundled Maven on Windows:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
& 'C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.3\plugins\maven\lib\maven3\bin\mvn.cmd' -pl api-gateway test
```

Current gateway tests cover JWT rejection/forwarding and rate-limit key resolution.

## Service Communication

Primary internal calls:

| Caller | Callee | Purpose |
| --- | --- | --- |
| auth-service | user-service | Create user profile after registration |
| user-service | notification-service | Sync reminder schedule when settings change |
| plan-service | user-service | Fetch profile data for plan generation |
| activity-service | ai-service | Request journal/insight AI workflows |
| activity-service | progress-service | Increment progress after user activity |
| activity-service | plan-service | Update plan progress from completed activity |
| ai-service | plan-service / progress-service | Enrich AI output and progress-aware insights |
| feature services | subscription-service | Check entitlement and increment metered usage |

Durable asynchronous flows use RabbitMQ instead of `@Async` HTTP dispatch. Producers return after publishing a durable message; workers call the existing internal HTTP APIs and let listener retry handle temporary downstream outages.

| Producer | Queue | Worker action |
| --- | --- | --- |
| activity-service | `activity.progress-events` | Update progress-service stats after mood, journal, breathing, meditation, or affirmation activity |
| activity-service | `activity.plan-events` | Mark the matching plan-service activity as completed |
| activity-service | `activity.ai-mood-insight-events` | Request AI mood insight and publish the generated result |
| activity-service | `activity.ai-mood-insight-result-events` | Save the generated mood insight back to the mood entry |
| activity-service | `activity.ai-journal-summary-events` | Request AI journal summary and publish the generated result |
| activity-service | `activity.ai-journal-summary-result-events` | Save the generated journal summary back to the journal entry |
| ai-service | `ai.chat-plan-events` | Mark `CHAT_WITH_AI` complete |
| ai-service | `ai.chat-progress-events` | Update chat progress |

AI conversation analysis remains local `@Async` work inside ai-service because it does not depend on another service being available.
Each queue has a `.dlq` dead-letter queue bound to the service DLX for messages that exhaust retries.
Rabbit publishers use transacted channels so publish failures surface at the producer instead of being treated as successful fire-and-forget work.

## Security Model

Public client requests:

1. Client authenticates through auth-service.
2. Auth-service issues JWT access and refresh tokens.
3. Client sends `Authorization: Bearer <accessToken>` to the gateway.
4. Gateway validates the JWT and forwards identity headers.
5. Downstream services trust gateway-injected headers and do not parse JWTs directly.

Forwarded identity headers:

```text
X-User-Id
X-User-Email
X-User-Roles
```

Internal service requests:

```text
X-Internal-Key: <shared secret>
```

These endpoints are not exposed through the gateway.

## Development Notes

When adding or changing a service:

1. Add or update the Maven module.
2. Add/update `Dockerfile`.
3. Add/update `docker-compose.yml` service definition.
4. Add/update gateway route in `api-gateway/src/main/resources/application.yml`.
5. Add Flyway migrations under `src/main/resources/db/migration`.
6. Add service tests before wiring the endpoint into the mobile app.

## Status

Implemented modules:

- api-gateway
- auth-service
- user-service
- ai-service
- content-service
- activity-service
- plan-service
- progress-service
- notification-service
- subscription-service
