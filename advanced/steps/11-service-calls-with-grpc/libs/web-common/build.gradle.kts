// Code every ShopLite service shares: Problem Details error handling, the exceptions it maps,
// the shop-wide settings record, and (as test fixtures) ready-made JWTs for tests.
plugins {
    id("shoplite.library")
}

dependencies {
    // compileOnly: each service brings these starters itself, at the versions of the Boot BOM.
    compileOnly("org.springframework.boot:spring-boot-starter-webmvc")
    compileOnly("org.springframework.boot:spring-boot")
    compileOnly("org.springframework.boot:spring-boot-starter-validation")
    compileOnly("org.springframework:spring-tx")
    compileOnly("org.springframework.boot:spring-boot-starter-security")
    compileOnly("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")
    testFixturesApi("org.springframework.security:spring-security-test")
    testFixturesApi("org.springframework.security:spring-security-oauth2-jose")
}
