# Megalodonte Roku

Build-time Java API for native Roku SceneGraph applications. A `RokuScreen`
supplies one or more `RokuScene` pages; when invoked by an application build,
`RokuExporter` evaluates that screen and writes a sideload ZIP containing a
Roku manifest, SceneGraph XML, BrightScript entry point, and icon. **Java does
not run on the Roku TV.**

The API supports named pages, text labels, packaged PNG/JPEG images, remote-control buttons,
route history, and a counter that updates its label at runtime. It is a
foundation for explicit Roku-compatible components and behaviors, without
promising arbitrary Java-to-BrightScript translation. Client code can use
unprefixed `megalodonte.components.Button`, `Text`, `Image`,
and `Counter`, plus a build-time `megalodonte.theme.Theme` palette. Generated
BrightScript may be checked with [BrighterScript](https://github.com/rokucommunity/brighterscript)
(`bsc`) before sideloading; BrighterScript is a validator/transpiler for
BrightScript, not a Java compiler.

## Build this library

This repository is a Java library, not a standalone Roku channel. It requires
JDK 25 to build; the included Gradle Wrapper uses Gradle 9.2.0. From the
repository root, run:

```sh
./gradlew build
```

The library can be used from a Java project that has it on its build classpath.
For example, the exporter API accepts a scene, an application title, and a
destination ZIP path:

```java
RokuExporter.export(new RokuScene("Hello", "Welcome"), "Hello", Path.of("build/roku/hello.zip"));
```

Import `megalodonte.roku.RokuExporter` and `RokuScene`, plus `java.nio.file.Path`.
The exported ZIP contains the native Roku files; the Java classes are used only
during the build. This repository's Gradle build does not define a `packageRoku`
task.

## Generated applications

Use `megalodonte <name> --platform roku --mode native` from
[`megalodonte-cli`](https://github.com/eliezer-dev-software-enginner/megalodonte-cli).
The generated project includes the Gradle Wrapper and a `packageRoku` task;
run `./gradlew packageRoku` from that generated project's root to create its
sideload ZIP.

See [API](docs/API.md), [architecture](docs/ARCHITECTURE.md), [events](docs/EVENTS.md), and
[device testing](docs/TESTING.md) for the supported model and its limits.
