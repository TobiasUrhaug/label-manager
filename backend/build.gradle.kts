plugins {
	java
	checkstyle
	jacoco
	id("org.springframework.boot") version "4.0.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.diffplug.spotless") version "7.0.2"
}

spotless {
	java {
		// AOSP variant: 4-space indent and a 100-column limit, matching the Indentation
		// and LineLength rules in config/checkstyle/checkstyle.xml.
		googleJavaFormat("1.28.0").aosp()
		removeUnusedImports()
		trimTrailingWhitespace()
		endWithNewline()
	}
}

group = "org.omt"
version = "0.0.1-SNAPSHOT"
description = "Label Manager application"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

checkstyle {
	toolVersion = "10.21.4"
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-json")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // PostgreSQL driver
    implementation("org.postgresql:postgresql:42.7.3")
    // Flyway for Postgres
    implementation("org.springframework.boot:spring-boot-starter-flyway")

    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("com.h2database:h2")

    // AWS S3 SDK for document storage
    implementation(platform("software.amazon.awssdk:bom:2.31.59"))
    implementation("software.amazon.awssdk:s3")

	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-resttestclient")
    testImplementation("org.springframework.security:spring-security-test")
    // Testcontainers versions are managed by the Spring Boot BOM. Do not pin them:
    // a pinned 1.20.1 negotiated Docker API v1.32, which engines with MinAPIVersion
    // 1.40+ reject with a 400, making Testcontainers report "no valid Docker environment".
    // Testcontainers 2.x prefixes its module artifacts: junit-jupiter -> testcontainers-junit-jupiter.
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-minio")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")
}

tasks.withType<Test> {
	useJUnitPlatform()
	finalizedBy(tasks.jacocoTestReport)
}

jacoco {
	toolVersion = "0.8.14"
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)

	reports {
		xml.required = true
		html.required = true
	}
}
