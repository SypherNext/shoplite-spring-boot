// Spring Cloud Config Server: serves the files in config-repo/ to every service. Port 8288.
plugins {
    id("shoplite.spring-service")
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-config-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}
