package megalodonte.components;

/** Static text positioned in the FHD Roku scene.
 * @param value displayed text
 * @param x horizontal position
 * @param y vertical position
 * @param width label width
 * @param height label height
 */
public record Text(String value, int x, int y, int width, int height) implements Component {
    /** Validates text and bounds. */
    public Text {
        if (value == null || x < 0 || y < 0 || width <= 0 || height <= 0)
            throw new IllegalArgumentException("Text and positive bounds are required");
    }
}
