plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "com.interviewme"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    // All application modules
    implementation(project(":common"))
    implementation(project(":billing"))
    implementation(project(":ai-chat"))
    implementation(project(":exports"))
    implementation(project(":linkedin"))
    implementation(project(":backend"))

    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core:4.25.1")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    // Hypersistence Utils for JSONB support
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.0")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

springBoot {
    mainClass.set("com.interviewme.Application")
    buildInfo()
}

// Copy frontend build to sboot static resources
val copyFrontend = tasks.register<Copy>("copyFrontend") {
    group = "build"
    description = "Copy frontend dist to sboot static resources"
    dependsOn(":frontend:npmBuild")
    from("${project.rootDir}/frontend/dist")
    into("${project.layout.buildDirectory.get()}/resources/main/static")
}

tasks.named("processResources") {
    dependsOn(copyFrontend)
}
