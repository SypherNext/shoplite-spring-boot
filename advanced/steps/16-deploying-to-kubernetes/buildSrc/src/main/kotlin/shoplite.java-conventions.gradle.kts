// Settings for every Java project in the build: Java 25, Maven Central, the BOMs, JUnit 5.
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    id("io.spring.dependency-management")
}

group = "com.shoplite"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        // The versions Spring Boot tests together, plus Spring Modulith's and the Spring Cloud release train.
        mavenBom(SpringBootPlugin.BOM_COORDINATES)
        mavenBom("org.springframework.modulith:spring-modulith-bom:2.1.2")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.4")
    }
}

dependencies {
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
