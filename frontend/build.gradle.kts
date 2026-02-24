plugins {
    id("com.github.node-gradle.node")
}

node {
    version.set("20.11.0")
    npmVersion.set("10.2.4")
    download.set(true)
    nodeProjectDir.set(file("${project.projectDir}"))
}

// The node-gradle plugin already provides npmInstall task
// We just need to define our custom tasks

tasks.register<com.github.gradle.node.npm.task.NpmTask>("npmBuild") {
    group = "build"
    description = "Build frontend with Vite"
    dependsOn(tasks.npmInstall)
    args.set(listOf("run", "build"))
    workingDir.set(file("${project.projectDir}"))
}

tasks.register<com.github.gradle.node.npm.task.NpmTask>("npmDev") {
    group = "application"
    description = "Run frontend dev server"
    dependsOn(tasks.npmInstall)
    args.set(listOf("run", "dev"))
    workingDir.set(file("${project.projectDir}"))
}

tasks.named("clean") {
    doLast {
        delete("${project.projectDir}/dist")
        delete("${project.projectDir}/node_modules")
    }
}
