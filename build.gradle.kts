plugins {
    java
    jacoco
    id("com.github.hierynomus.license-base") version "0.16.1"
    id("org.unbroken-dome.test-sets") version "4.1.0"
}

apply(plugin = "com.github.hierynomus.license")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

testSets {
    create("systemTest")
}

tasks.check {
    dependsOn(tasks.findByName("systemTest"))
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines.add("junit-jupiter")
        includeEngines.add("jqwik")
    }
    systemProperty("logback.configurationFile", "src/systemTest/resources/logback.xml")
}

sourceSets {
    main {
        java.srcDir("src/workflow/java")
    }
}

repositories {
    mavenCentral()
}

license {
    setIgnoreFailures(false)
    setHeader(File("$rootDir/licenceHeader.txt"))
    setSkipExistingHeaders(false)
    exclude("**/*.json")
    exclude("**/test.html")
}

dependencies {
    implementation("io.github.keymaster65:copper2go-api:3.2.1")

    testImplementation("org.assertj:assertj-assertions-generator:2+")
    testImplementation("net.jqwik:jqwik:1.6.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("org.mockito:mockito-core:4.2.0")

    testImplementation("org.testcontainers:testcontainers:1.19.1")
    testImplementation("org.testcontainers:kafka:1.19.1")

    testImplementation("org.mock-server:mockserver-netty:5.15.0")
}