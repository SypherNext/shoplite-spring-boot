// A deployable Spring Boot service: an executable jar with build info for /actuator/info.
plugins {
    id("shoplite.java-conventions")
    id("org.springframework.boot")
}

springBoot {
    buildInfo()
}
