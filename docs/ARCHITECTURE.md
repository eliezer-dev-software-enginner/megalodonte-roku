# Roku target architecture

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
