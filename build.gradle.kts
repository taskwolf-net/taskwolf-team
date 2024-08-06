plugins {
  id("java")
  id("org.springframework.boot") version "3.2.5"
}

group = "net.taskwolf"
version = "1.0.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

repositories {
  mavenCentral()
  maven {
    url = uri("https://git.taskwolf.net/api/v4/projects/8/packages/maven")
    credentials(HttpHeaderCredentials::class) {
      name = "Private-Token"
      value = System.getenv("TASKWOLF_GITLAB_PRIVATE_TOKEN") ?:
        findProperty("taskwolfGitlabPrivateToken") as String?
    }
    authentication {
      create("header", HttpHeaderAuthentication::class)
    }
  }
}

dependencies {
  testCompileOnly(platform("org.junit:junit-bom:5.10.2"))
  testCompileOnly("org.junit.jupiter:junit-jupiter:5.10.2")

  implementation("net.taskwolf:core:1.0.0-SNAPSHOT")

  implementation("com.google.inject:guice:7.0.0")

  implementation("com.google.guava:guava:33.1.0-jre")

  implementation("org.projectlombok:lombok:1.18.32")
  annotationProcessor("org.projectlombok:lombok:1.18.32")
  testImplementation("org.projectlombok:lombok:1.18.32")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.32")

  implementation("com.datastax.oss:java-driver-core:4.17.0")

  implementation("org.json:json:20240303")
  implementation("commons-io:commons-io:2.16.1")

  implementation("org.springframework.boot:spring-boot-starter-web:3.2.5")

  implementation("io.jsonwebtoken:jjwt:0.12.5")

  implementation("com.sun.mail:javax.mail:1.6.2")

  implementation("com.stripe:stripe-java:26.1.0")
}

tasks.test {
  useJUnitPlatform()
}

tasks.bootJar {
  mainClass = "net.taskwolf.team.TeamApplication"
}