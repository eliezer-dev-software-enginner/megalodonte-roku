package megalodonte.roku;

/** A number displayed by SceneGraph and updated by an INCREMENT button. */
public record RokuCounter(String label, int initialValue) {
    public RokuCounter {
        if (label == null || label.isBlank()) throw new IllegalArgumentException("Counter label is required");
    }
}
