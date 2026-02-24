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

    // Hypersistence Utils for JSONB support
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.7.0")

    // Thymeleaf (standalone mode for PDF templates)
    implementation("org.thymeleaf:thymeleaf:3.1.2.RELEASE")

    // Flying Saucer - HTML to PDF conversion with CSS support
    implementation("org.xhtmlrenderer:flying-saucer-pdf:9.7.2")

    // OpenPDF
    implementation("com.github.librepdf:openpdf:2.0.3")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
