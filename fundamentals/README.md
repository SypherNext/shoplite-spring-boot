# Spring Boot Fundamentals: companion code

The ShopLite catalog service, from an empty Spring Initializr project to a tested REST API on PostgreSQL.
Set up your computer first (JDK 25, Docker, an editor): see the [README at the repository root](../README.md).

Stack: Spring Boot 4.1.2, Spring Framework 7, Java 25, Gradle 9 (Kotlin DSL), Hibernate 7, Flyway, PostgreSQL 18,
springdoc-openapi 3, Testcontainers 2.

## Steps

Each folder is a complete Gradle project: the app at the end of that lesson. Lessons 0 to 2 and 21 do not change
code, so they have no folder.

| Folder | Lesson | What the app has at the end of it |
| --- | --- | --- |
| `steps/03-your-first-spring-boot-app` | Your First Spring Boot App | the project as Spring Initializr generates it (web only) |
| `steps/04-beans-and-dependency-injection` | Beans and Dependency Injection | `Category`, a repository interface with an in-memory implementation, `CategoryService`, a start-up report |
| `steps/05-configuration-classes-scopes-and-lifecycle` | Configuration Classes, Scopes and Lifecycle | `InMemoryCatalogConfig` with `@Bean` methods, plus a demo package (lifecycle and scopes) |
| `steps/06-how-auto-configuration-works` | How Auto-Configuration Works | the start-up report switched by `@ConditionalOnBooleanProperty` |
| `steps/07-milestone-the-shoplite-skeleton` | Milestone: The ShopLite Skeleton | products next to categories, seeded catalog of 3 categories and 8 products, demo code removed |
| `steps/08-configuration-files-and-profiles` | Configuration Files and Profiles | shop settings in `application.properties`, `dev` and `prod` profiles, `@Value` |
| `steps/09-typed-settings-with-configuration-properties` | Typed Settings with Configuration Properties | `ShopProperties` record with `@ConfigurationProperties` and validation |
| `steps/10-your-first-rest-controller` | Your First REST Controller | GET endpoints for shop, categories and products; Swagger UI |
| `steps/11-request-and-response-bodies` | Request and Response Bodies | request and response records; POST, PUT and DELETE |
| `steps/12-validating-input` | Validating Input | Bean Validation on the request records |
| `steps/13-clear-errors-with-problem-details` | Clear Errors with Problem Details | `ApiExceptionHandler` with Problem Details; 409 conflicts |
| `steps/14-milestone-an-in-memory-catalog-api` | Milestone: An In-Memory Catalog API | search, SKU lookup, low-stock list; MockMvc API tests |
| `steps/15-postgresql-with-docker-compose-support` | PostgreSQL with Docker Compose Support | `compose.yaml`, Docker Compose support, Testcontainers for tests |
| `steps/16-entities-and-repositories` | Entities and Repositories | JPA entities, Spring Data repositories, transactions (tables from `create-drop` and `data.sql`) |
| `steps/17-writing-queries` | Writing Queries | derived queries, a JPQL `@Query`, SQL logging in dev |
| `steps/18-database-migrations-with-flyway` | Database Migrations with Flyway | Flyway migrations V1 and V2, `ddl-auto=validate` |
| `steps/19-logging-and-debugging` | Logging and Debugging | service log lines, ECS JSON logs in the `prod` profile |
| `steps/20-capstone-the-catalog-api-on-postgresql` | Capstone: The Catalog API on PostgreSQL | OpenAPI title, tags and documented errors; identical to `final/` |
| `final/` | | the finished catalog service; Spring Boot: Production-Ready APIs starts here |

Steps 03 to 14 need only JDK 25. From step 15 on, Docker must be running for `build` (Testcontainers) and
`bootRun` (Docker Compose support).

## Run the finished app

```bash
cd fundamentals/final
./gradlew build
./gradlew bootRun
```

Then open `http://localhost:8080/swagger-ui.html`, or:

```bash
curl -s localhost:8080/api/categories
curl -s "localhost:8080/api/products?categoryId=2"
curl -s localhost:8080/api/products/low-stock
```

Stop with Ctrl+C and remove the database container with `docker compose down -v`.

The `prod` profile reads the database from environment variables and writes JSON logs:

```bash
./gradlew bootJar
DATABASE_URL=jdbc:postgresql://localhost:<port>/shoplite DATABASE_USERNAME=shoplite DATABASE_PASSWORD=shoplite-dev-only \
  java -jar build/libs/shoplite-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

The database user and password in `compose.yaml` are for local development only.
