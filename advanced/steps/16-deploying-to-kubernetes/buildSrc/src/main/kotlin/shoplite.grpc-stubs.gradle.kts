// gRPC stubs generated from the shared proto/ folder at the root of the build.
// Spring Boot's Gradle plugin sees the protobuf plugin and configures protoc and the
// protoc-gen-grpc-java generator for it, at the versions Spring Boot manages.
import com.google.protobuf.gradle.id

plugins {
    id("com.google.protobuf")
}

the<SourceSetContainer>().named("main") {
    extensions.getByName<SourceDirectorySet>("proto").srcDir(rootProject.file("proto"))
}

protobuf {
    plugins {
        id("grpc")
    }
}