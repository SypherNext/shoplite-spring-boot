# ShopLite: Advanced Architecture and Scale

Companion code for the course "Spring Boot: Advanced Architecture and Scale". It starts from the end of
the production APIs course (`../production-apis/final`) and grows ShopLite into a set of services.

## Layout

```text
advanced/
  steps/NN-lesson-slug/   the complete code at the end of each lesson
  final/                  the capstone: every service, compose files, Kubernetes manifests, CI workflow
```

Every folder is a complete Gradle build with its own wrapper. Steps 01 to 06 are one Spring Boot
application; from step 07 on, each step is a multi-project build with `services/*` and `libs/web-common`.
Step 21 is a separate small project: `before/` on Spring Boot 3.5, the folder itself after the upgrade.

## Requirements

- JDK 25
- Docker with at least 8 GB of memory (the native image build in step 14 needs about 7 GB)
- A Bash shell with `curl` for the commands in the lessons

## Running a step

Steps 01 to 06:

```bash
cd steps/02-application-events
./gradlew bootRun          # starts the containers in compose.yaml, then the application on :8280
./gradlew build            # compiles and runs the tests (Testcontainers starts its own containers)
```

From step 07 on:

```bash
cd steps/12-reactive-spring-with-webflux
docker compose up -d                                   # PostgreSQL, Redis, Kafka, RabbitMQ, Keycloak, ...
./gradlew :services:catalog-service:bootRun            # one terminal per service
./gradlew build
```

From step 15 on, `./gradlew bootBuildImage` builds an image per service and
`docker compose -f compose.yaml -f compose.apps.yaml up -d` runs them.

## Ports

The first two courses use 8080 for the app, 18080 for Keycloak and 18090 for the payment stub. This course
uses its own range, so its stack can run next to theirs.

| What | Port |
| --- | --- |
| monolith (steps 01 to 06), later the gateway | 8280 |
| catalog-service (gRPC 9281) | 8281 |
| order-service | 8282 |
| review-service | 8283 |
| assistant-service | 8284 |
| config-server | 8288 |
| discovery-server (Eureka) | 8761 |
| PostgreSQL / Redis / Kafka | 18232 / 18279 / 18292 |
| RabbitMQ (management UI) | 18272 (18273) |
| Keycloak / payment stub | 18180 / 18190 |
| Grafana / OTLP HTTP | 18300 / 18318 |
| Ollama | 18434 |

## Development users

Keycloak imports the `shoplite` realm with three users that exist only in this local setup:
`alice` and `bob` (CUSTOMER) and `olivia` (ADMIN). Each password is the user name followed by `-dev-pass`.
Every password in this repository is for local development only.

## Models for the assistant (steps 18 to 20, final)

```bash
docker compose up -d ollama
docker exec shoplite-ollama ollama pull qwen2.5:3b
docker exec shoplite-ollama ollama pull nomic-embed-text
```

## Steps

01 setup and environment, 02 application events, 03 messaging with Kafka, 04 the transactional outbox,
05 RabbitMQ and choosing a broker, 06 a modular monolith with Spring Modulith, 07 splitting into services,
08 an API gateway with Spring Cloud Gateway, 09 configuration and service discovery, 10 circuit breakers
with Resilience4j, 11 service calls with gRPC, 12 reactive Spring with WebFlux, 13 profiling and load
testing, 14 faster startup with AOT, CDS and native images, 15 container images with buildpacks,
16 deploying to Kubernetes, 17 CI/CD with GitHub Actions, 18 chat and tool calling with Spring AI,
19 retrieval with pgvector, 20 building an MCP server, 21 upgrading from Spring Boot 3 to 4.
