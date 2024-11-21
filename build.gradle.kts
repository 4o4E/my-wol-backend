plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    application
    idea
}

group = "top.e404"
version = "1.0.0"

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.12"
fun ktor(module: String, version: String = ktorVersion) = "io.ktor:ktor-${module}:${version}"

dependencies {
    // ktor
    implementation(ktor("server-core-jvm"))
    implementation(ktor("server-netty-jvm"))
    implementation(ktor("server-auth-jvm"))
    implementation(ktor("server-websockets-jvm"))
    implementation(ktor("server-status-pages"))
    implementation(ktor("server-content-negotiation-jvm"))
    implementation(ktor("serialization-kotlinx-json-jvm"))
    implementation(ktor("server-call-logging-jvm"))
    // log
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

application {
    mainClass.set("top.e404.mywol.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

idea {
    module.excludeDirs.add(file(".run"))
    module.excludeDirs.add(file(".kotlin"))
}