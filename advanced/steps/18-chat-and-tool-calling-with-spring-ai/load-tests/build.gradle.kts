// Load tests with Gatling. Not part of "./gradlew build": run them on purpose, against a running app:
//   ./gradlew :load-tests:gatlingRun --simulation com.shoplite.loadtests.ProductSearchSimulation
plugins {
    java
    id("io.gatling.gradle") version "3.16.1"
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
