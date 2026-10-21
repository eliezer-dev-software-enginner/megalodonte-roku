plugins { `java-library` }
group = "megalodonte"
version = "0.1.0"
java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
repositories { mavenCentral() }
dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.test { useJUnitPlatform() }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
