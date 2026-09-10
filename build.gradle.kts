import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
    id("maven-publish")
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"

    jacoco
}

group = "com.carbonlogger"
version = "1.0.0"
description = "Custom Spring request logger"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            artifactId = "carbon-logger"
        }
    }
    repositories {
        // Publish into local Maven (~/.m2)
        mavenLocal()
    }
}

tasks {
    build {
        dependsOn("ktlintFormat", "ktlintCheck")
    }

    named("ktlintCheck") {
        mustRunAfter("ktlintFormat")
    }

    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs += "-Xjsr305=strict"
            jvmTarget = "21"
        }
    }

    withType<Test> {
        useJUnitPlatform()
        finalizedBy(jacocoTestReport)
    }

    jacocoTestReport {
        reports {
            xml.required.set(true)
            html.required.set(false)
        }
    }
}

tasks.test {
    testLogging {
        showStandardStreams = true
        events("passed", "skipped", "failed")
    }
    jvmArgs = listOf("--add-opens=java.base/java.time=ALL-UNNAMED")
}
