package megalodonte.roku;

/**
 * Static text emitted as a SceneGraph Label.
 *
 * @param value displayed text
 * @param x horizontal position in the 1920 × 1080 scene
 * @param y vertical position in the 1920 × 1080 scene
 * @param width label width
 * @param height label height
 */
public record RokuText(String value, int x, int y, int width, int height) {
    /** Validates the text and bounds. */
    public RokuText {
        if (value == null) throw new IllegalArgumentException("Text value is required");
        if (x < 0 || y < 0 || width <= 0 || height <= 0)
            throw new IllegalArgumentException("Text bounds must be positive");
    }
}
