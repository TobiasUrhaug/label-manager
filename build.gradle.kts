plugins {
	java
	checkstyle
	jacoco
	id("org.springframework.boot") version "4.0.0"
	id("io.spring.dependency-management") version "1.1.7"
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
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
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
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-thymeleaf-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:testcontainers:1.20.1")
    testImplementation("org.testcontainers:junit-jupiter:1.20.1")
    testImplementation("org.testcontainers:postgresql:1.20.1")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:minio:1.20.1")
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
