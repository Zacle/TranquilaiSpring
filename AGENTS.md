# AGENTS.md — TranquilAI Spring Backend

Developer guide for working with this Spring Boot microservices project.

## Build & Run Commands

```bash
# Build all modules
./mvnw clean package -DskipTests

# Build a single service
./mvnw clean package -pl auth-service -DskipTests

# Run tests
./mvnw test
./mvnw test -pl auth-service

# Start infrastructure (Postgres, MongoDB, Redis, Mailpit)
docker compose up postgres mongodb redis mailpit -d

# Start a service locally (from its directory)
cd auth-service && ./mvnw spring-boot:run

# Start the full stack
docker compose up --build

# Lint (not configured — use IDE Kotlin inspections)
```

## Architecture Overview

9 microservices behind a Spring Cloud Gateway. The gateway handles JWT validation and injects `X-User-Id / X-User-Email / X-User-Roles` headers into every forwarded request.

```
Android App → API Gateway (:8080) → microservices
```

Services never validate JWTs — they trust gateway-injected headers.

### Databases
- PostgreSQL 16 — 7 isolated databases, one per service (except ai-service)
- MongoDB 7 — ai-service only
- Redis 7 — auth-service (verification codes, refresh token revocation, rate limiting)

## Module Organization

Parent POM: `pom.xml` — defines dependency management (Spring Boot 3.4.1, Spring AI 1.0.0, JJWT 0.12.6).

```
api-gateway/         # Spring Cloud Gateway, reactive, port 8080
auth-service/        # Registration, login, JWT, port 8081
user-service/        # Profiles, settings, mental health, port 8082
ai-service/          # Chat, summarization, insights, port 8083
content-service/     # Static content (affirmations etc.), port 8084
activity-service/    # User activity logging + analytics, port 8085
plan-service/        # AI daily wellness plans, port 8086
progress-service/    # Stats, streaks, badges, port 8087
notification-service/# FCM push notifications, port 8088
```

Each service structure:
```
[service]/
├── pom.xml
├── Dockerfile
└── src/main/kotlin/com/tranquilai/[service]/
    ├── config/          # SecurityConfig, beans, HTTP clients
    ├── controller/
    ├── service/
    ├── dto/
    │   ├── request/
    │   └── response/
    ├── entity/
    ├── repository/
    └── exception/       # Domain exceptions + GlobalExceptionHandler
```

## Security Patterns

### User-Facing Endpoints
Every controller reads `X-User-Id` from the request header. The gateway guarantees its presence on all authenticated routes. No JWT parsing in microservices.

```kotlin
// Typical controller parameter
@AuthenticationPrincipal user: GatewayUser
// GatewayUser is populated by a WebFilter/SecurityConfig that reads X-User-Id
```

### Service-to-Service (Internal) Endpoints
Internal routes (`/internal/**`) are blocked at the gateway. They require a shared secret:

```
X-Internal-Key: <value of INTERNAL_SERVICE_KEY env var>
```

The receiving service's `SecurityConfig` validates this header and grants `ROLE_INTERNAL`. Failures should be logged as warnings — internal calls are best-effort (don't propagate failures to the user-facing request).

## Common Development Tasks

### Adding a New Service

1. Create the Maven module directory with `pom.xml` (copy from `progress-service/pom.xml` as a template)
2. Add `<module>my-service</module>` to the root `pom.xml`
3. Copy `Dockerfile` from any existing service
4. Add container to `docker-compose.yml` with its datasource env vars
5. Add the route block to `api-gateway/src/main/resources/application.yml`
6. Add `CREATE DATABASE tranquilai_myservice;` + GRANT to `scripts/init-db.sql`
7. Write Flyway migrations in `src/main/resources/db/migration/V1__create_initial_tables.sql`

### Adding an Endpoint

1. Create/update request DTO in `dto/request/`
2. Create/update response DTO in `dto/response/`
3. Add repository method (Spring Data derived query or `@Query`)
4. Implement service method
5. Add controller method with `@AuthenticationPrincipal user: GatewayUser`

### Adding an Internal Endpoint (Service-to-Service)

1. Create a controller annotated with `@PreAuthorize("hasRole('INTERNAL')")`
2. Map it under `/internal/` path
3. Create an HTTP client in the calling service (use `RestTemplate` + `X-Internal-Key` header)
4. Catch all exceptions in the client and log as warn — don't propagate

```kotlin
// Calling service — client example
fun callOtherService(userId: String, body: Any) {
    try {
        restTemplate.exchange(
            "$serviceUrl/internal/...",
            HttpMethod.PUT,
            HttpEntity(body, headers()),  // headers() adds X-Internal-Key
            Void::class.java
        )
    } catch (ex: Exception) {
        log.warn("Call to other-service failed: ${ex.message}")
    }
}
```

### Adding a Flyway Migration

Name the file `V{N}__description_with_underscores.sql` under `src/main/resources/db/migration/`.

All services use `baseline-on-migrate: true` in `application.yml`.

### Inter-Service HTTP Client Pattern

```kotlin
@Component
class OtherServiceClient(
    private val restTemplate: RestTemplate,
    @Value("\${app.other-service-url}") private val serviceUrl: String,
    @Value("\${app.internal-service-key}") private val internalKey: String
) {
    private fun headers() = HttpHeaders().apply {
        set("X-Internal-Key", internalKey)
        contentType = MediaType.APPLICATION_JSON
    }
}
```

Declare the `RestTemplate` bean in `config/AppConfig.kt`:
```kotlin
@Configuration
class AppConfig {
    @Bean
    fun restTemplate() = RestTemplate()
}
```

## Key Service Notes

### auth-service
- Issues JWTs signed with `JWT_SECRET`. Access token: 15 min. Refresh token: 7 days (stored in Redis).
- On registration, calls `user-service POST /internal/users` to create the user profile.

### user-service
- Settings endpoint (`PUT /api/users/me/settings`) calls notification-service to sync reminder schedule after every save. This is best-effort.
- Hard account delete (`DELETE /api/users/me/account`) cascades via FK `ON DELETE CASCADE` to `mental_health_profiles` and `user_settings`.

### ai-service
- Uses Spring AI `ChatClient` with MongoDB-backed conversation memory (`MongoDBChatMemory`).
- Each user gets a `conversationId = userId` to maintain persistent chat history.

### content-service
- All content tables have a `language_code` column. **Always pass `languageCode`** to repository methods — even when filtering by category. The category query methods include `AndLanguageCode` in their name to enforce this.
- Favorites pattern: toggle returns `{"isFavorite": true/false}`. List/get responses include `isFavorite` inline.

### activity-service
- Analytics endpoints group by ISO date string in Kotlin (not SQL) to avoid DB-specific date functions.
- Period values: `week` = 7 days, `month` = 30 days, `year` = 365 days.

### progress-service
- `getGrowthAreas()` computes scores entirely from `UserStats` — no additional DB queries.
- Growth areas: Mood Tracking, Journaling, Consistency, Mindful Conversation, Wellness Practice.

### notification-service
- `@EnableScheduling` + `@Scheduled(cron = "0 * * * * *")` fires every minute to check `reminder_schedules` where `enabled = true` and `reminder_time` matches current UTC time (HH:mm).
- Firebase Admin SDK initialized from `FIREBASE_SERVICE_ACCOUNT_JSON` env var (full JSON string). Falls back to Application Default Credentials if the env var is empty.
- Stale FCM tokens are auto-deactivated: when Firebase returns `UNREGISTERED`, `DeviceToken.isActive` is set to `false`.

### api-gateway
- Reactive (WebFlux). The `JwtAuthFilter` validates tokens and populates `SecurityContext` with a `GatewayAuthentication` object.
- Routes are defined in `application.yml`. Each route specifies a `uri` (downstream service URL) and `predicates` (path pattern).

## Naming Conventions

| Artifact | Convention | Example |
|----------|-----------|---------|
| Controller | Resource + Controller | `UserController` |
| Service | Resource + Service | `UserService` |
| Repository | Resource + Repository | `UserRepository` |
| Entity | Noun (singular) | `User`, `DeviceToken` |
| Request DTO | Action + Request | `UpdateUserRequest` |
| Response DTO | Resource + Response | `UserResponse` |
| Migration | `V{N}__verb_noun.sql` | `V1__create_initial_tables.sql` |

## Environment Variables

| Variable | Services | Description |
|----------|----------|-------------|
| `JWT_SECRET` | api-gateway, auth-service | 256-bit JWT signing secret |
| `OPENAI_API_KEY` | ai-service, plan-service | OpenAI API key |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | notification-service | Full Firebase service account JSON |
| `INTERNAL_SERVICE_KEY` | auth-service, user-service, plan-service, notification-service | Shared service-to-service secret |
| `DB_PASSWORD` | all PostgreSQL services | Database password |
| `REDIS_PASSWORD` | auth-service | Redis password |

## Key Decisions

- **Gateway-injected headers** — keeps microservices stateless with respect to auth; no JWT library needed per service.
- **Internal key** over mTLS — simple enough for the current scale; upgrade to mTLS or service mesh if needed later.
- **Best-effort internal calls** — inter-service HTTP failures are logged but not propagated; user-facing requests succeed even if a downstream internal call fails.
- **Flyway `baseline-on-migrate`** — allows running migrations on a DB that was created before Flyway was introduced.
- **RestTemplate over WebClient** in non-reactive services — keeps code synchronous and easier to reason about; all microservices are Servlet-based (not WebFlux) except api-gateway.
- **Per-minute reminder scheduler** — simple cron approach; no message broker required. Acceptable at this scale. Reminder times are stored in UTC (HH:mm).
- **FCM token self-healing** — stale tokens deactivated automatically on `UNREGISTERED` error; clients only need to register on login/launch and deregister on logout.
