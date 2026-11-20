// Eureka: the registry where every service instance announces itself. Port 8761.
plugins {
    id("shoplite.spring-service")
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
}
