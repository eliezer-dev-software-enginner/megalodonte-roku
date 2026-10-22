# Roku Java API

`megalodonte-roku` is a build-time API. The Gradle `packageRoku` task loads a
`RokuScreen`, calls `pages()`, validates the model, and emits a native Roku ZIP.
No Java bytecode is shipped to the TV.

## Client-facing components and theme

Roku projects can import `megalodonte.components.Button`, `Text`, `Image`, and
`Counter`, matching the unprefixed component naming used by the Android target.
The component implementations remain platform-specific: Roku converts them to
SceneGraph at build time. A `RokuScene` accepts these components directly:

```java
new RokuScene("home", "Welcome", "Choose a page",
        new Text("Featured", 100, 100, 500, 60),
        new Image(Path.of("assets/featured.png"), 100, 180, 600, 340),
        new Button("About", "about"));
```

Override `RokuScreen.theme()` to provide `megalodonte.theme.Theme`, a palette
with background, heading, text, button, and focused-button colors in
`0xRRGGBBAA` format. This is a build-time palette, not runtime theme switching.
The older `RokuButton`, `RokuText`, `RokuImage`, and `RokuCounter` models remain
available to existing applications.

`megalodonte.application.Router` can register pages with `route(page)` and
return their ordered list via `pages()`. The first route starts the app;
navigation and Back are implemented by the generated BrightScript. This router
is a build-time registry, not a Java object running on the TV.

## Pages and navigation

Implement `RokuScreen.render()` for the first page. Override `pages()` to
return all pages; the first one opens at launch. Every `RokuScene` has a unique
lowercase route. A `RokuButton(label, targetRoute)` opens a route in this list.

```java
public final class HomeScreen implements RokuScreen {
    @Override public RokuScene render() {
        return new RokuScene("home", "Welcome", "Choose a page",
                List.of(new RokuButton("Counter", "counter")));
    }

    @Override public List<RokuScene> pages() {
        return List.of(render(), new RokuScene("counter", "Counter", "Try OK",
                List.of(RokuButton.increment("Add one")),
                new RokuCounter("Clicks", 0)));
    }
}
```

On the remote, **Up/Down** moves the highlighted button, **OK** activates it,
and **Back** returns to the previous page. Navigating back keeps the counter's
current value while the app remains open. The generated scene currently fits
up to five buttons per page at FHD resolution.

## Text and images

Use `RokuText` for additional static labels and `RokuImage` for local PNG or
JPEG assets. Coordinates and sizes use the generated 1920 × 1080 scene. The
exporter copies image bytes into the ZIP and emits a SceneGraph `Poster` with a
`pkg:/images/...` URI. The path is read on the build machine, not on the TV.

```java
new RokuScene("home", "Welcome", "Choose a page", List.of(), null,
        List.of(new RokuText("Featured", 100, 100, 500, 60)),
        List.of(new RokuImage(Path.of("assets/featured.png"), 100, 180, 600, 340)));
```

These Roku-specific build models are static in this version; the counter is
the supported runtime-updating element.

## Reactive state

`Slideshow` displays a list of local PNG/JPEG files in a loop. It packages the
images as SceneGraph `Poster` nodes and uses a repeating `Timer`; the timer runs
only while its route is active. The last image wraps back to the first. A page
with a slideshow supports one button because additional buttons would extend
below the generated FHD scene.

```java
new Slideshow(List.of(Path.of("assets/ad-01.png"), Path.of("assets/ad-02.png")),
        320, 185, 1280, 720, 5); // five seconds per image
```

The [Roku Timer reference](https://developer.roku.com/dev/docs/timer) defines
the `duration`, `repeat`, `control`, and observed `fire` fields used here.

## Project-owned SceneGraph behavior

An application can implement `megalodonte.components.RokuExtension` when its
behavior goes beyond the built-in components. Keep the implementation, its
BrightScript source, and its media in the **application repository**. The
interface supplies the page's XML children, files to package under
`components/` or `media/`, and BrightScript calls for initialization, enter,
leave, and remote-key handling. The exporter embeds these resources in the
ZIP and preserves router history. Only one extension page per package is
currently supported. The Roku Hello World example keeps its Tic Tac Toe game
in its own `src/main/` tree; the framework contains no game logic.

An extension owns its page controls. Do not mix it with built-in buttons,
counters, or slideshows on the same page. Project-supplied XML and BrightScript
are trusted build input; validate the packaged output with BrighterScript and
test behavior on a device.

`RokuCounter(label, initialValue)` binds a displayed number to a runtime
BrightScript value. `RokuButton.increment(label)` changes it and immediately
updates the SceneGraph label. This proves the event → state → view path on Roku;
it is **not** the desktop/Android reactivity library running on the TV.
Additional state types and actions should extend the typed build model and
emitter together.

## Validation

The exporter rejects missing pages, duplicate routes, unknown button targets,
increment buttons without a counter, invalid route names, and more than five
buttons on a page. It escapes XML text and BrightScript string literals.
`RokuScene(heading, message)` remains available for a static one-page app.
