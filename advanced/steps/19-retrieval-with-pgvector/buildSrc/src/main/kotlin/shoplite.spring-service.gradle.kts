// A deployable Spring Boot service: an executable jar with build info for /actuator/info.
plugins {
    id("shoplite.java-conventions")
    id("org.springframework.boot")
}

springBoot {
    buildInfo()
}

// Tests never talk to a running config server or Eureka: each test brings what it needs.
tasks.withType<Test> {
    systemProperty("spring.cloud.config.enabled", "false")
    systemProperty("eureka.client.enabled", "false")
}

// ./gradlew bootBuildImage builds an OCI image with Cloud Native Buildpacks: no Dockerfile.
// Every service gets the name shoplite/<project>:<version>, for example shoplite/catalog-service:0.1.0.
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>("bootBuildImage") {
    imageName = "shoplite/${project.name}:${project.version}"
    // The buildpack's memory calculator gives Netty (WebFlux, R2DBC, gRPC, Redis) only 10 MB of direct
    // memory, which the review service ran out of under light load. BPE_* variables are baked into the image.
    environment.putAll(mapOf(
        "BPE_DELIM_JAVA_TOOL_OPTIONS" to " ",
        "BPE_APPEND_JAVA_TOOL_OPTIONS" to "-XX:MaxDirectMemorySize=64M"))
}
