package megalodonte.roku;

import java.util.List;

/**
 * One named page with text and remote-control actions.
 *
 * @param route unique lowercase route used by navigation buttons
 * @param heading page heading
 * @param message page body text
 * @param buttons up to five focusable actions
 * @param counter optional counter, or {@code null} when absent
 */
public record RokuScene(String route, String heading, String message, List<RokuButton> buttons, RokuCounter counter) {
    /** Validates the route and actions, and copies the button list. */
    public RokuScene {
        if (route == null || !route.matches("[a-z][a-z0-9-]*"))
            throw new IllegalArgumentException("Route must be lowercase kebab-case");
        if (heading == null || heading.isBlank() || message == null || message.isBlank())
            throw new IllegalArgumentException("A Roku scene needs a heading and message");
        buttons = List.copyOf(buttons);
        if (buttons.size() > 5) throw new IllegalArgumentException("A Roku scene supports up to five buttons");
        if (counter == null && buttons.stream().anyMatch(button -> button.action() == RokuButton.Action.INCREMENT))
            throw new IllegalArgumentException("An increment button needs a counter on the same page");
    }

    /**
     * Creates a static page at the {@code home} route.
     * @param heading page heading
     * @param message page body text
     */
    public RokuScene(String heading, String message) {
        this("home", heading, message, List.of(), null);
    }

    /**
     * Creates a page without a counter.
     * @param route unique lowercase route
     * @param heading page heading
     * @param message page body text
     * @param buttons focusable actions
     */
    public RokuScene(String route, String heading, String message, List<RokuButton> buttons) {
        this(route, heading, message, buttons, null);
    }
}
