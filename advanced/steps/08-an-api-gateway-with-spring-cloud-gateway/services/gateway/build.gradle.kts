// The API gateway: the one address clients use. Port 8280.
plugins {
    id("shoplite.spring-service")
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    // Swagger UI only: the gateway shows the OpenAPI documents of the services behind it.
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
}
