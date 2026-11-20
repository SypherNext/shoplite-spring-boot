rootProject.name = "shoplite-advanced"

// One Gradle build, several deployable services plus a shared library.
// Run one service with: ./gradlew :services:catalog-service:bootRun
include(
    "libs:web-common",
    "services:catalog-service",
    "services:order-service",
    "services:gateway",
    "services:review-service",
    "services:config-server",
    "services:discovery-server",
)
