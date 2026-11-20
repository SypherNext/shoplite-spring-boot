# ShopLite: Production-Ready APIs

Code for the course **Spring Boot: Production-Ready APIs**, the second course in the ShopLite
Spring Boot series. It starts from `../fundamentals/final` (the catalog API) and adds customers,
orders, payments, security, caching, background work and observability.

## What is in this folder

```text
production-apis/
  README.md          this file
  steps/             one complete, runnable Gradle project per lesson that changes code
  final/             the finished app (the same code as steps/21), and the start of the advanced course
```

| Step folder | Lesson | What it adds |
| --- | --- | --- |
| `steps/02-modeling-orders-with-relationships` | 2 | Customer, Order, OrderLine, migration V3, order endpoints (customer named by a `Customer-Email` header for now) |
| `steps/03-fixing-the-n-plus-1-problem` | 3 | Entity graphs and JOIN FETCH |
| `steps/04-pagination-sorting-and-projections` | 4 | Pages, a sort whitelist, DTO and interface projections |
| `steps/05-transactions-in-practice` | 5 | `Propagation.MANDATORY` for stock changes |
| `steps/06-optimistic-locking` | 6 | `@Version`, admin status changes, 409 on stale data |
| `steps/07-versioning-your-api` | 7 | `API-Version` header, version 1.0 and 2.0 of an order |
| `steps/08-documenting-with-openapi` | 8 | One OpenAPI document per version, examples, bearer scheme |
| `steps/09-idempotent-requests` | 9 | `Idempotency-Key`, migration V4 |
| `steps/10-unit-tests-with-junit-and-mockito` | 10 | Unit tests |
| `steps/11-slice-tests-for-web-and-data` | 11 | `@WebMvcTest` and `@DataJpaTest` tests |
| `steps/12-integration-tests-with-testcontainers` | 12 | One shared Testcontainers context for full-app tests |
| `steps/13-how-spring-security-works` | 13 | Spring Security filter chain, URL rules, CORS |
| `steps/14-users-passwords-and-roles` | 14 | In-memory users with BCrypt hashes, roles, `@PreAuthorize` |
| `steps/15-jwt-and-the-oauth2-resource-server` | 15 and 16 | JWT access tokens from Keycloak (lesson 16 needs no code change) |
| `steps/17-calling-other-services` | 17 | Payment provider client, WireMock stub |
| `steps/18-timeouts-retries-and-ssrf-protection` | 18 | Timeouts, `@Retryable`, 503 + Retry-After, SSRF-safe webhooks |
| `steps/19-caching-with-redis` | 19 | Redis cache for product lookups |
| `steps/20-async-work-scheduling-and-virtual-threads` | 20 | `@Async` webhooks, `@Scheduled` cleanup, virtual threads |
| `steps/21-health-metrics-and-tracing` | 21 | Actuator, a custom metric, OpenTelemetry tracing, Grafana LGTM |

## What you need

- JDK 25 (for example Eclipse Temurin 25). Check with `java -version`.
- Docker with Compose v2 (Docker Desktop on Windows and macOS). Check with `docker compose version`.
- Free host ports **8080** (the app), **18080** (Keycloak, from step 15) and **18090** (payment stub, from step 17).
  PostgreSQL, Redis and Grafana LGTM get random host ports.
- About 4 GB of disk space for the Docker images. The first start pulls them and takes a few minutes.

## Run a step (or the final app)

```bash
cd production-apis/final          # or any steps/NN-... folder
./gradlew build                   # Windows: gradlew.bat build. Compiles and runs the tests (Testcontainers)
./gradlew bootRun                 # starts compose.yaml through Spring Boot's Docker Compose support
```

The app listens on http://localhost:8080. Swagger UI: http://localhost:8080/swagger-ui.html.
Stop the app with Ctrl+C, then remove the containers and their data:

```bash
docker compose down -v
```

From step 15 on you need an access token for most endpoints. The local Keycloak imports the
`shoplite` realm from `keycloak/shoplite-realm.json` on its first start:

```bash
ALICE=$(curl -s -X POST http://localhost:18080/realms/shoplite/protocol/openid-connect/token \
  -d grant_type=password -d client_id=shoplite-cli -d username=alice -d password=alice-dev-pass \
  | python -c "import json,sys; print(json.load(sys.stdin)['access_token'])")
curl -s localhost:8080/api/customers/me -H "Authorization: Bearer $ALICE"
```

The password grant (`shoplite-cli`) exists only so curl can get a token in this local lab. Real
apps use the authorization code flow with PKCE (lesson 16).

From step 21 on, find Grafana's port with `docker compose port grafana-lgtm 3000`.

## Development-only credentials

All of these are fake and only work against the local containers. Never reuse them.

| What | Value |
| --- | --- |
| PostgreSQL | database `shoplite`, user `shoplite`, password `shoplite-dev-only` |
| Keycloak admin console (http://localhost:18080) | `admin` / `admin-dev-only` |
| Keycloak users in realm `shoplite` | `alice` / `alice-dev-pass` (CUSTOMER), `bob` / `bob-dev-pass` (CUSTOMER), `olivia` / `olivia-dev-pass` (ADMIN) |
| Step 14 in-memory users | `alice@shoplite.test` / `alice-dev-pass`, `bob@shoplite.test` / `bob-dev-pass`, `olivia@shoplite.test` / `olivia-dev-pass` |

## Troubleshooting

- **Port 8080, 18080 or 18090 is already in use.** Stop whatever uses it (another copy of the app,
  another Keycloak) or change the port mapping in `compose.yaml` and the matching URL in
  `application.properties`.
- **Keycloak changes in `shoplite-realm.json` do not show up.** `--import-realm` only imports a realm
  that does not exist yet. Run `docker compose down -v` and start again.
- **`docker compose exec ... /opt/keycloak/...` fails in Git Bash with a strange path.** Git Bash
  rewrites arguments that start with `/`. Run `export MSYS_NO_PATHCONV=1` first.
- **Startup takes more than a minute.** Normal from step 15 on: Docker Compose support waits until
  Keycloak (and, from step 21, the Grafana LGTM container) is ready.
- **Payment calls fail with "RST_STREAM" or "EOF reached while reading".** The JDK HTTP client asks
  plain-HTTP servers to upgrade to HTTP/2; the WireMock container must run with
  `--disable-http2-plain` (already set in `compose.yaml`). Right after the container starts, give it
  a few seconds before the first call.

## Build everything

`../scripts/verify-all.ps1` (Windows PowerShell) or `../scripts/verify-all.sh` builds and tests
every step folder and every final folder. Both read `JAVA_HOME`.
