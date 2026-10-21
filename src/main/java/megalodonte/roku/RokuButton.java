package megalodonte.roku;

/**
 * A focusable remote-control button that navigates or updates a counter.
 *
 * @param label visible button text
 * @param action action performed when the viewer presses OK
 * @param targetRoute destination for navigation, or {@code null} for increment
 */
public record RokuButton(String label, Action action, String targetRoute) {
    /** Supported remote-control actions. */
    public enum Action {
        /** Open the route named by {@code targetRoute}. */
        NAVIGATE,
        /** Increase the counter on the current page. */
        INCREMENT
    }

    /** Validates the label, action, and destination. */
    public RokuButton {
        if (label == null || label.isBlank()) throw new IllegalArgumentException("Button label is required");
        if (action == null) throw new IllegalArgumentException("Button action is required");
        if (action == Action.NAVIGATE && (targetRoute == null || !targetRoute.matches("[a-z][a-z0-9-]*")))
            throw new IllegalArgumentException("Button target must be a route");
        if (action == Action.INCREMENT && targetRoute != null)
            throw new IllegalArgumentException("Increment buttons cannot navigate");
    }

    /**
     * Creates a button that opens another route.
     * @param label visible button text
     * @param targetRoute destination route
     */
    public RokuButton(String label, String targetRoute) { this(label, Action.NAVIGATE, targetRoute); }

    /**
     * Creates a button that increments the counter on its page.
     * @param label visible button text
     * @return increment button
     */
    public static RokuButton increment(String label) { return new RokuButton(label, Action.INCREMENT, null); }
}
