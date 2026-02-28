plugins {
    java
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
}

group = "com.interviewme"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":billing"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Hypersistence Utils for JSONB/pgvector support
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.0")

    // LangChain4j for query routing and embeddings
    implementation("dev.langchain4j:langchain4j:1.11.0")
    implementation("dev.langchain4j:langchain4j-ollama:1.11.0")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
