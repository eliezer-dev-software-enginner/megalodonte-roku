# Roku event mapping

The [Roku guide to handling application events](https://developer.roku.com/dev/docs/handling-application-events)
distinguishes remote key presses, node field changes, and functional fields.
These are SceneGraph/BrightScript events; Java client code is evaluated at build
time and does not receive events on the TV.

## Implemented: remote keys

`RokuExporter` generates `onKeyEvent(key, press)` in `MainScene.brs`. It ignores
key releases, handles `up` and `down` to move button focus, `OK` to navigate or
increment the counter, and `back` to return to the previous page. It returns
`true` for handled keys and `false` otherwise, allowing unhandled events to
bubble through SceneGraph. The device-tested Hello World covers navigation,
focus, and increment actions.

The counter's BrightScript value is updated on `OK`, then its SceneGraph
`Label.text` field is set. This is a direct state-to-view update, not a field
observer or the desktop/Android Java reactivity runtime.

## Future event contracts

- For nodes that handle keys internally, use `observeField()` on the relevant
  field instead of duplicating their key handling in `onKeyEvent()`.
- For asynchronous Task-to-render-thread data, evaluate `roRenderThreadQueue`
  or the appropriate SceneGraph field observer, keeping network work off the
  render thread.
- Add typed Java build models and generated BrightScript handlers together;
  do not expose a Java callback that cannot execute on the TV.

The current target does not implement field observers, functional fields,
Task-based data loading, or arbitrary client callbacks.
