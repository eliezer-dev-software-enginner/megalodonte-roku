package megalodonte.components;

/** Runtime-updating integer displayed on a Roku page.
 * @param label visible caption
 * @param initialValue value on startup
 */
public record Counter(String label, int initialValue) implements Component {
    /** Validates the visible label. */
    public Counter {
        if (label == null || label.isBlank()) throw new IllegalArgumentException("Counter label is required");
    }
}
