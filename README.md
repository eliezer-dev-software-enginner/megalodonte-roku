# Megalodonte Roku

Build-time Java API for native Roku SceneGraph applications. A `RokuScreen`
returns a `RokuScene`; `RokuExporter` evaluates that Java screen during the
Gradle build and writes a sideload ZIP containing a Roku manifest, SceneGraph
XML, BrightScript entry point, and icon. **Java does not run on the Roku TV.**

The API supports named pages, heading/message text, remote-control buttons,
route history, and a counter that updates its label at runtime. It is a
foundation for explicit Roku-compatible components and behaviors, without
promising arbitrary Java-to-BrightScript translation. Generated
BrightScript may be checked with [BrighterScript](https://github.com/rokucommunity/brighterscript)
(`bsc`) before sideloading; BrighterScript is a validator/transpiler for
BrightScript, not a Java compiler.

Use `megalodonte <name> --platform roku --mode native` from
[`megalodonte-cli`](https://github.com/eliezer-dev-software-enginner/megalodonte-cli).
The generated project includes the Gradle Wrapper and a `packageRoku` task.

See [API](docs/API.md), [architecture](docs/ARCHITECTURE.md), and
[device testing](docs/TESTING.md) for the supported model and its limits.
