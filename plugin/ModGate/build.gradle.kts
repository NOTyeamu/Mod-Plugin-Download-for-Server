plugins {
    java
}

group = "my.yeamu"
version = "1.0.0"

repositories {
    mavenCentral()
    // Репозиторий PaperMC с API сервера
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Paper API 1.21.11 (поставляется сервером, в jar не упаковываем)
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") { expand(props) }
}

tasks.jar {
    archiveFileName.set("ModGate-${project.version}.jar")
}
