package megalodonte.roku;

import java.nio.file.Path;

/**
 * A local image embedded in the Roku package and emitted as a SceneGraph Poster.
 *
 * @param source readable local PNG or JPEG file
 * @param x horizontal position in the 1920 × 1080 scene
 * @param y vertical position in the 1920 × 1080 scene
 * @param width displayed width
 * @param height displayed height
 */
public record RokuImage(Path source, int x, int y, int width, int height) {
    /** Validates the source path and bounds. */
    public RokuImage {
        if (source == null) throw new IllegalArgumentException("Image source is required");
        if (x < 0 || y < 0 || width <= 0 || height <= 0)
            throw new IllegalArgumentException("Image bounds must be positive");
    }
}
