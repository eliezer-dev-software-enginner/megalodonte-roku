# Roku target architecture

## Roku references

- [Defining SceneGraph components](https://developer.roku.com/dev/docs/defining-scenegraph-components) describes XML components, their `children`, and `Poster` nodes with `pkg:/images/...` asset URIs. The exporter uses these structures for pages and packaged images.
- [BrightScript/XML markup equivalence](https://developer.roku.com/dev/docs/brightscript-xml-markup-equivalence) explains that SceneGraph nodes can be declared in XML and then located or updated in BrightScript. The exporter follows this pattern for focus, navigation, and counter updates.
- [Handling application events](https://developer.roku.com/dev/docs/handling-application-events) defines `onKeyEvent`, field observers, and render-thread messages. See [event mapping](EVENTS.md) for what this target implements today.

The Android target hosts Megalodonte Java components on Android. Roku devices
have no JVM or JavaFX runtime, so the Roku target evaluates a restricted Java
description during the build and ships only platform-native files:

```text
Java RokuScreen → validated RokuScene model → SceneGraph XML + BrightScript
                                       → manifest + icon → sideload ZIP
```

Each Java page becomes a SceneGraph `Group`. Buttons become a Rectangle/Label
pair. The generated `MainScene.brs` handles remote keys, route history, focus
color, and counter state. `source/main.brs` opens the scene and waits for the
screen to close. A shared Megalodonte source-language API can be considered
later, but existing JavaFX or Android components are not implicitly portable.

[BrighterScript](https://github.com/rokucommunity/brighterscript) validates
the generated BrightScript and can later transpile hand-authored `.bs` files.
It does not compile Java. Keep the exporter independent of Node.js so a normal
Gradle/JDK build can always produce a sideload ZIP; use `bsc` in validation
when available.

The current output targets FHD static layout plus route and counter behavior.
Next platform work should add typed layout, artwork, data loading, and media
playback one feature at a time, with device validation for each SceneGraph
mapping. Arbitrary Java execution, reflection on the TV, and WebView are not
part of this target.
