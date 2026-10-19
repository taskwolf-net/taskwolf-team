import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.net.URL
import java.util.zip.GZIPInputStream

plugins {
  id("java")
  id("io.freefair.lombok") version "9.8.0"
  id("org.springframework.boot") version "4.1.1"
}

group = "net.taskwolf"
version = "1.0.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

repositories {
  mavenCentral()
  mavenLocal()
}

dependencies {
  testCompileOnly(platform("org.junit:junit-bom:6.1.3"))
  testCompileOnly("org.junit.jupiter:junit-jupiter:6.1.3")

  implementation("net.taskwolf:core:1.0.0-SNAPSHOT")
  implementation("net.taskwolf:workflow:1.0.0-SNAPSHOT")

  implementation("com.google.inject:guice:7.0.0")

  implementation("com.google.guava:guava:33.7.2-jre")

  implementation("org.projectlombok:lombok:1.18.48")
  annotationProcessor("org.projectlombok:lombok:1.18.48")
  testImplementation("org.projectlombok:lombok:1.18.48")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.48")

  implementation("com.datastax.oss:java-driver-core:4.17.0")

  implementation("org.json:json:20260814")
  implementation("commons-io:commons-io:2.22.0")

  implementation("org.springframework.boot:spring-boot-starter-web:4.1.1")

  implementation("io.jsonwebtoken:jjwt:0.13.0")

  implementation("com.sun.mail:javax.mail:1.6.2")

  implementation("com.stripe:stripe-java:28.4.0")

  implementation("dev.samstevens.totp:totp:1.7.1")

  implementation("io.kubernetes:client-java:27.0.0")

  compileOnly("com.maxmind.geoip2:geoip2:5.2.0")
}

tasks.test {
  useJUnitPlatform()
}

tasks.bootJar {
  mainClass = "net.taskwolf.team.TeamApplication"
}

tasks.register("downloadGeoLite2Database") {
  val licenseKey = System.getenv("MAXMIND_LICENSE_KEY") ?: ""
  val databaseUrl = "https://download.maxmind.com/app/geoip_download?" +
    "edition_id=GeoLite2-City&license_key=$licenseKey&suffix=tar.gz"
  val resourcesDir = File("geo")
  val downloadFile = File(buildDir, "GeoLite2-City.tar.gz")
  doLast {
    resourcesDir.mkdirs()
    if (downloadFile.exists()) {
      downloadFile.delete()
    }
    URL(databaseUrl).openStream().use { input ->
      downloadFile.outputStream().use { output ->
        input.copyTo(output)
      }
    }
    extract(downloadFile, resourcesDir)
    downloadFile.delete()
  }
}

fun extract(file: File, destination: File) {
  GZIPInputStream(file.inputStream()).use { gis ->
    TarArchiveInputStream(gis).use { tis ->
      var entry = tis.nextTarEntry
      while (entry != null) {
        if (!entry.isDirectory && entry.name.endsWith(".mmdb")) {
          val outputFile = File(destination, "GeoLite2-City.mmdb")
          outputFile.outputStream().use { os ->
            tis.copyTo(os)
          }
        }
        entry = tis.nextTarEntry
      }
    }
  }
}