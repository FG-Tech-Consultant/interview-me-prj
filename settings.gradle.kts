rootProject.name = "interview-me-prj"

include("common")
include("billing")
include("backend")
include("ai-chat")
include("exports")
include("linkedin")
include("sboot")
include("frontend")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("org.springframework.boot") version "3.4.2"
        id("io.spring.dependency-management") version "1.1.4"
        id("com.github.node-gradle.node") version "7.0.2"
    }
}
