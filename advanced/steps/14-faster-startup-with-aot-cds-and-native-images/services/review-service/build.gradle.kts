// The review service: product reviews, reactive from end to end (WebFlux + R2DBC). Port 8283.
plugins {
    id("shoplite.spring-service")
    // Native image support. bootBuildImage now builds a native image instead of a JVM image.
    id("org.graalvm.buildtools.native")
}

dependencies {
    // Only for shoplite-defaults.properties: this service does not scan the library's MVC classes.
    implementation(project(":libs:web-common"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-webclient")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:3.2.0")
    // Flyway still talks JDBC (once, at startup); the application itself only uses R2DBC.
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-r2dbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    // Lets @ServiceConnection hand Spring Boot an R2DBC connection for the PostgreSQL container.
    testImplementation("org.testcontainers:testcontainers-r2dbc")
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
    testImplementation("io.projectreactor:reactor-test")
}

// The native image gets its own name, so it can sit next to a JVM image of the same service.
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>("bootBuildImage") {
    imageName = "shoplite/review-service:0.1.0-native"
}
