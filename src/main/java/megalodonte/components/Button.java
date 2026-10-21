package megalodonte.components;

/**
 * A remote-control action that navigates to a route or increments a counter.
 * @param label visible caption
 * @param action behavior on OK
 * @param targetRoute destination for navigation, otherwise {@code null}
 */
public record Button(String label, Action action, String targetRoute) implements Component {
    /** Supported button actions. */
    public enum Action {
        /** Navigate to another route. */ NAVIGATE,
        /** Increment the current page's counter. */ INCREMENT
    }

    /** Creates a navigation button.
     * @param label visible caption
     * @param targetRoute destination route
     */
    public Button(String label, String targetRoute) { this(label, Action.NAVIGATE, targetRoute); }

    /** Creates an increment button for the counter on the current screen.
     * @param label visible caption
     * @return increment action
     */
    public static Button increment(String label) { return new Button(label, Action.INCREMENT, null); }

    /** Validates the label and action. */
    public Button {
        if (label == null || label.isBlank() || action == null)
            throw new IllegalArgumentException("Button label and action are required");
        if (action == Action.NAVIGATE && (targetRoute == null || !targetRoute.matches("[a-z][a-z0-9-]*")))
            throw new IllegalArgumentException("Button target must be a route");
        if (action == Action.INCREMENT && targetRoute != null)
            throw new IllegalArgumentException("Increment buttons cannot navigate");
    }
}
