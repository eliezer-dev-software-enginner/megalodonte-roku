# Roku Java API

`megalodonte-roku` is a build-time API. The Gradle `packageRoku` task loads a
`RokuScreen`, calls `pages()`, validates the model, and emits a native Roku ZIP.
No Java bytecode is shipped to the TV.

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

## Reactive state

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
