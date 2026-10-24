plugins {
    id("java")
    application
}

group = "ru.spbstu.khramov"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass = "ru.spbstu.khramov.Main"
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.run {
    standardInput = System.`in`
}

// Пакетные эксперименты: ./gradlew experiments --args="periods 12 100 100000"
tasks.register<JavaExec>("experiments") {
    mainClass = "ru.spbstu.khramov.Experiments"
    classpath = sourceSets["main"].runtimeClasspath
}
