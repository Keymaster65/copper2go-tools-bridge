plugins {
    java
    jacoco
    id("com.github.hierynomus.license-base") version "0.16.1"
    id("org.unbroken-dome.test-sets") version "4.0.0"
}

apply(plugin = "com.github.hierynomus.license")

java {
    sourceCompatibility = JavaVersion.VERSION_17
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
    mavenLocal() // TODO to be removed after copper2go-api 3 release
}

license {
    setIgnoreFailures(false)
    setHeader(File("$rootDir/licenceHeader.txt"))
    setSkipExistingHeaders(false)
    exclude("**/*.json")
    exclude("**/test.html")
}

dependencies {
    implementation("io.github.keymaster65:copper2go-api:2+")
    implementation("org.copper-engine:copper-coreengine:5+")
    implementation("org.slf4j:slf4j-api:2.0.0-alpha5")
    implementation("ch.qos.logback:logback-classic:1.3.0-alpha12")

    testImplementation("org.testcontainers:testcontainers:1.+")
    testImplementation("org.testcontainers:kafka:1.+")
    testImplementation("org.assertj:assertj-assertions-generator:2+")
    testImplementation("net.jqwik:jqwik:1.+")
    testImplementation("org.junit.jupiter:junit-jupiter:5.+")
    testImplementation("org.mockito:mockito-core:4+")
    testImplementation("org.mock-server:mockserver-netty:5.+")
}