plugins { `java-library` }
group = "megalodonte"
version = "0.1.0"
java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
