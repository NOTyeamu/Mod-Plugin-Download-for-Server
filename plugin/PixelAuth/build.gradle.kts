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
    // Log4j есть в сервере — нужен только для фильтра, скрывающего пароли из консоли
    compileOnly("org.apache.logging.log4j:log4j-core:2.24.1")
    // PixelUI-Paper — необязательная зависимость (красивые окна входа).
    // CI собирает его раньше, jar берём из соседней папки.
    compileOnly(fileTree(mapOf("dir" to "../PixelUI/build/libs", "include" to listOf("PixelUI-Paper-*.jar"))))
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
    archiveFileName.set("PixelAuth-${project.version}.jar")
}
