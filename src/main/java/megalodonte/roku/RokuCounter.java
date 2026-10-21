package megalodonte.roku;

/**
 * A number displayed by SceneGraph and updated by an increment button.
 * Its value persists while navigating, and resets when the app starts again.
 *
 * @param label visible label beside the value
 * @param initialValue value on application startup
 */
public record RokuCounter(String label, int initialValue) {
    /** Validates the visible label. */
    public RokuCounter {
        if (label == null || label.isBlank()) throw new IllegalArgumentException("Counter label is required");
    }
}
