// buildSrc holds the build logic every project shares (the "convention plugins" in src/main/kotlin).
// Plugin versions are declared once, here.
plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-gradle-plugin:4.1.2")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
}
