package megalodonte.roku;

/** A focusable remote-control button that navigates or updates a counter. */
public record RokuButton(String label, Action action, String targetRoute) {
    public enum Action { NAVIGATE, INCREMENT }

    public RokuButton {
        if (label == null || label.isBlank()) throw new IllegalArgumentException("Button label is required");
        if (action == null) throw new IllegalArgumentException("Button action is required");
        if (action == Action.NAVIGATE && (targetRoute == null || !targetRoute.matches("[a-z][a-z0-9-]*")))
            throw new IllegalArgumentException("Button target must be a route");
        if (action == Action.INCREMENT && targetRoute != null)
            throw new IllegalArgumentException("Increment buttons cannot navigate");
    }

    public RokuButton(String label, String targetRoute) { this(label, Action.NAVIGATE, targetRoute); }
    public static RokuButton increment(String label) { return new RokuButton(label, Action.INCREMENT, null); }
}
