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
