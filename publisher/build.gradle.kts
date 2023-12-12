import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.1.4"
	id("io.spring.dependency-management") version "1.1.3"
	kotlin("jvm") version "1.8.22"
	kotlin("plugin.spring") version "1.8.22"
}

group = "com.cmj"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

extra["springCloudVersion"] = "2022.0.4"

val exposedVersion: String by project
dependencies {
	implementation("org.springframework.boot:spring-boot-starter-amqp")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springframework.cloud:spring-cloud-starter-openfeign") //
	compileOnly("org.projectlombok:lombok")
	runtimeOnly("com.mysql:mysql-connector-j")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.amqp:spring-rabbit-test")

	// https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

	// spring-data-jdbc
	// https://mvnrepository.com/artifact/org.springframework.data/spring-data-jdbc
	// implementation("org.springframework.data:spring-data-jdbc:3.1.3")

	// expose 의존성
	implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
	implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
	implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
	implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

	implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

	implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
	implementation("org.jetbrains.exposed:exposed-money:$exposedVersion")

	// spring-data-jdbc 내장, 데이터소스 객체 생성 자동화
	// spring transaction 호환, @Transaction
	// 스프링 트랜잭션 매니저를 안 써야 더 디테일한 처리를 할 수 있음.
	implementation("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")

	implementation("com.auth0:java-jwt:4.4.0")
	implementation("at.favre.lib:bcrypt:0.10.2")

	// 스웨거
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs += "-Xjsr305=strict"
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
