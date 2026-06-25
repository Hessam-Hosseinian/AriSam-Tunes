plugins {
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.0"
    id("io.ktor.plugin") version "3.5.0"
}

group = "com.arisamtunes"
version = "1.0.0"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("org.jetbrains.exposed:exposed-core:1.3.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:1.3.0")
    implementation("org.jetbrains.exposed:exposed-java-time:1.3.0")
    implementation("org.flywaydb:flyway-core:12.9.0")
    implementation("org.flywaydb:flyway-database-postgresql:12.9.0")
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("org.postgresql:postgresql:42.7.11")
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("com.mpatric:mp3agic:0.9.1")

    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("seedMusic") {
    group = "application"
    description = "Extract metadata and seed the project music catalog"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.arisamtunes.seed.SeedMainKt")
    workingDir = rootProject.projectDir.parentFile
}
