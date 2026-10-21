package megalodonte.roku;

import java.util.List;

/** One named page with text and remote-control navigation actions. */
public record RokuScene(String route, String heading, String message, List<RokuButton> buttons, RokuCounter counter) {
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

    public RokuScene(String heading, String message) {
        this("home", heading, message, List.of(), null);
    }

    public RokuScene(String route, String heading, String message, List<RokuButton> buttons) {
        this(route, heading, message, buttons, null);
    }
}
