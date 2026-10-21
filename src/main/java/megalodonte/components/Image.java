package megalodonte.components;

import java.nio.file.Path;

/** A local PNG or JPEG image packaged as a Roku SceneGraph Poster.
 * @param source image file on the build machine
 * @param x horizontal position
 * @param y vertical position
 * @param width poster width
 * @param height poster height
 */
public record Image(Path source, int x, int y, int width, int height) implements Component {
    /** Validates source and bounds. */
    public Image {
        if (source == null || x < 0 || y < 0 || width <= 0 || height <= 0)
            throw new IllegalArgumentException("Image source and positive bounds are required");
    }
}
