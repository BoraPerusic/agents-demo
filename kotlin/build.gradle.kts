plugins {
    kotlin("jvm") version "2.0.0"
    application
    kotlin("plugin.serialization") version "2.0.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

    implementation("io.ktor:ktor-server-core:2.3.12")
    implementation("io.ktor:ktor-server-cio:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.12")

    implementation("com.github.ajalt.clikt:clikt:4.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Koog dependency (placeholder)
    // implementation("ai.koog:koog-core:0.1.0")

    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("org.wiremock:wiremock:3.9.1")
}

application {
    mainClass.set("agents.RobAgentKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runRob") {
    group = "application"
    mainClass.set("agents.RobAgentKt")
    classpath = sourceSets.getByName("main").runtimeClasspath
    standardInput = System.`in`
}

tasks.register<JavaExec>("runMike") {
    group = "application"
    mainClass.set("agents.MikeAgentKt")
    classpath = sourceSets.getByName("main").runtimeClasspath
    standardInput = System.`in`
}
