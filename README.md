# Megalodonte Roku

Build-time Java API for native Roku SceneGraph applications. A `RokuScreen`
returns a `RokuScene`; `RokuExporter` evaluates that Java screen during the
Gradle build and writes a sideload ZIP containing a Roku manifest, SceneGraph
XML, BrightScript entry point, and icon. **Java does not run on the Roku TV.**

The first version intentionally supports a heading and message. It is a
foundation for adding explicit Roku-compatible components and behaviors,
without promising arbitrary Java-to-BrightScript translation. Generated
BrightScript may be checked with [BrighterScript](https://github.com/rokucommunity/brighterscript)
(`bsc`) before sideloading; BrighterScript is a validator/transpiler for
BrightScript, not a Java compiler.

Use `megalodonte <name> --platform roku --mode native` from
[`megalodonte-cli`](https://github.com/eliezer-dev-software-enginner/megalodonte-cli).
The generated project includes the Gradle Wrapper and a `packageRoku` task.
