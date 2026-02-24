plugins {
    `java-library`
}

group = "com.interviewme"
version = "1.0.0"

dependencies {
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Validation API
    api("jakarta.validation:jakarta.validation-api:3.0.2")
}
